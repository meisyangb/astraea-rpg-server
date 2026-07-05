package cn.guangdian.gift.storage;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File; import java.sql.*; import java.util.*; import java.util.concurrent.*;

/** 礼包数据 SQLite 存储 */
public class GiftStorage {
    private final JavaPlugin p; private final File f; private Connection c;
    private final Map<UUID,Map<String,Integer>> claims=new ConcurrentHashMap<>();
    private final Map<UUID,Map<String,Long>> cooldowns=new ConcurrentHashMap<>();

    public GiftStorage(JavaPlugin pp){p=pp;f=new File(p.getDataFolder(),"gift.db");}
    public boolean init(){
        try{Class.forName("org.sqlite.JDBC");f.getParentFile().mkdirs();
            c=DriverManager.getConnection("jdbc:sqlite:"+f.getAbsolutePath());
            try(Statement s=c.createStatement()){s.execute("CREATE TABLE IF NOT EXISTS claims(uuid TEXT,gift_id TEXT,count INTEGER,PRIMARY KEY(uuid,gift_id))");s.execute("CREATE TABLE IF NOT EXISTS cooldowns(uuid TEXT,gift_id TEXT,last_time BIGINT,PRIMARY KEY(uuid,gift_id))");}
            p.getLogger().info("SQLite 礼包已初始化");return true;
        }catch(Exception e){p.getLogger().severe("SQLite 礼包失败: "+e.getMessage());return false;}}
    public void load(){
        if(c==null)return;claims.clear();cooldowns.clear();
        try(Statement s=c.createStatement()){ResultSet r=s.executeQuery("SELECT * FROM claims");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));claims.computeIfAbsent(u,k->new ConcurrentHashMap<>()).put(r.getString("gift_id"),r.getInt("count"));}
            r=s.executeQuery("SELECT * FROM cooldowns");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));cooldowns.computeIfAbsent(u,k->new ConcurrentHashMap<>()).put(r.getString("gift_id"),r.getLong("last_time"));}
        }catch(SQLException e){p.getLogger().warning("加载礼包失败: "+e.getMessage());}
        p.getLogger().info("已加载礼包: "+claims.size()+" 玩家领取,"+cooldowns.size()+" 冷却");}
    public CompletableFuture<Void> saveAsync(){return CompletableFuture.runAsync(this::save);}
    public void save(){
        if(c==null)return;
        try{c.setAutoCommit(false);
            try(PreparedStatement d1=c.prepareStatement("DELETE FROM claims");PreparedStatement i1=c.prepareStatement("INSERT OR REPLACE INTO claims VALUES(?,?,?)");
                PreparedStatement d2=c.prepareStatement("DELETE FROM cooldowns");PreparedStatement i2=c.prepareStatement("INSERT OR REPLACE INTO cooldowns VALUES(?,?,?)")){
                d1.executeUpdate();d2.executeUpdate();
                for(var e:claims.entrySet())for(var e2:e.getValue().entrySet()){i1.setString(1,e.getKey().toString());i1.setString(2,e2.getKey());i1.setInt(3,e2.getValue());i1.addBatch();}
                i1.executeBatch();
                for(var e:cooldowns.entrySet())for(var e2:e.getValue().entrySet()){i2.setString(1,e.getKey().toString());i2.setString(2,e2.getKey());i2.setLong(3,e2.getValue());i2.addBatch();}
                i2.executeBatch();c.commit();
            }catch(SQLException e){c.rollback();throw e;}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){p.getLogger().severe("保存礼包失败: "+e.getMessage());}}
    public void close(){if(c!=null)try{c.close();}catch(SQLException ignored){}}
    public Map<UUID,Map<String,Integer>> claims(){return claims;}
    public Map<UUID,Map<String,Long>> cooldowns(){return cooldowns;}
}
