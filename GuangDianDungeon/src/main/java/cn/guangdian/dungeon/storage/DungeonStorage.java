package cn.guangdian.dungeon.storage;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File; import java.sql.*; import java.util.*; import java.util.concurrent.*;
public class DungeonStorage {
    private final JavaPlugin p; private final File f; private Connection c;
    private final Map<UUID,Map<String,long[]>> clears=new ConcurrentHashMap<>(); // key->[time,count,bestTime,bestScore]
    private final Map<UUID,Map<String,Long>> cds=new ConcurrentHashMap<>(); // dungeonId->cooldownEnd
    public DungeonStorage(JavaPlugin pp){p=pp;f=new File(p.getDataFolder(),"dungeon.db");}
    public boolean init(){try{Class.forName("org.sqlite.JDBC");f.getParentFile().mkdirs();
        c=DriverManager.getConnection("jdbc:sqlite:"+f.getAbsolutePath());
        try(Statement s=c.createStatement()){s.execute("CREATE TABLE IF NOT EXISTS clears(uuid TEXT,clear_key TEXT,first_time BIGINT,count INTEGER,best_time BIGINT,best_score INTEGER,PRIMARY KEY(uuid,clear_key))");s.execute("CREATE TABLE IF NOT EXISTS cooldowns(uuid TEXT,dungeon_id TEXT,end_time BIGINT,PRIMARY KEY(uuid,dungeon_id))");}
        p.getLogger().info("SQLite 副本已初始化");return true;
    }catch(Exception e){p.getLogger().severe("SQLite 副本失败: "+e.getMessage());return false;}}
    public void load(){if(c==null)return;clears.clear();cds.clear();
        try(Statement s=c.createStatement()){ResultSet r=s.executeQuery("SELECT * FROM clears");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));clears.computeIfAbsent(u,k->new ConcurrentHashMap<>()).put(r.getString("clear_key"),new long[]{r.getLong("first_time"),r.getInt("count"),r.getLong("best_time"),r.getInt("best_score")});}
            r=s.executeQuery("SELECT * FROM cooldowns");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));cds.computeIfAbsent(u,k->new ConcurrentHashMap<>()).put(r.getString("dungeon_id"),r.getLong("end_time"));}
        }catch(SQLException e){p.getLogger().warning("加载副本失败: "+e.getMessage());}
        p.getLogger().info("已加载副本数据: "+clears.size()+" 玩家");}
    public CompletableFuture<Void> saveAsync(){return CompletableFuture.runAsync(this::save);}
    public void save(){if(c==null)return;
        try{c.setAutoCommit(false);
            try(PreparedStatement d1=c.prepareStatement("DELETE FROM clears");PreparedStatement i1=c.prepareStatement("INSERT OR REPLACE INTO clears VALUES(?,?,?,?,?,?)");
                PreparedStatement d2=c.prepareStatement("DELETE FROM cooldowns");PreparedStatement i2=c.prepareStatement("INSERT OR REPLACE INTO cooldowns VALUES(?,?,?)")){
                d1.executeUpdate();d2.executeUpdate();
                for(var e:clears.entrySet())for(var e2:e.getValue().entrySet()){i1.setString(1,e.getKey().toString());i1.setString(2,e2.getKey());i1.setLong(3,e2.getValue()[0]);i1.setInt(4,(int)e2.getValue()[1]);i1.setLong(5,e2.getValue()[2]);i1.setInt(6,(int)e2.getValue()[3]);i1.addBatch();}
                i1.executeBatch();
                for(var e:cds.entrySet())for(var e2:e.getValue().entrySet()){i2.setString(1,e.getKey().toString());i2.setString(2,e2.getKey());i2.setLong(3,e2.getValue());i2.addBatch();}
                i2.executeBatch();c.commit();
            }catch(SQLException e){c.rollback();throw e;}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){p.getLogger().severe("保存副本失败: "+e.getMessage());}}
    public void close(){if(c!=null)try{c.close();}catch(SQLException ignored){}}
    public Map<UUID,Map<String,long[]>> clears(){return clears;}
    public Map<UUID,Map<String,Long>> cooldowns(){return cds;}
}
