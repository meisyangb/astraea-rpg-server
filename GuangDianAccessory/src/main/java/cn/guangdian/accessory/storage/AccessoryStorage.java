package cn.guangdian.accessory.storage;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File; import java.sql.*; import java.util.*; import java.util.concurrent.*;
public class AccessoryStorage {
    private final JavaPlugin p; private final File f; private Connection c;
    private final Map<UUID,Map<String,String>> data=new ConcurrentHashMap<>(); // uuid -> {slot->accessoryId}
    public AccessoryStorage(JavaPlugin pp){p=pp;f=new File(p.getDataFolder(),"accessory.db");}
    public boolean init(){try{Class.forName("org.sqlite.JDBC");f.getParentFile().mkdirs();
        c=DriverManager.getConnection("jdbc:sqlite:"+f.getAbsolutePath());
        try(Statement s=c.createStatement()){s.execute("CREATE TABLE IF NOT EXISTS accessories(uuid TEXT,slot TEXT,acc_id TEXT,PRIMARY KEY(uuid,slot))");}
        p.getLogger().info("SQLite 饰品已初始化");return true;
    }catch(Exception e){p.getLogger().severe("SQLite 饰品失败: "+e.getMessage());return false;}}
    public void load(){if(c==null)return;data.clear();
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM accessories")){
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));data.computeIfAbsent(u,k->new ConcurrentHashMap<>()).put(r.getString("slot"),r.getString("acc_id"));}
        }catch(SQLException e){p.getLogger().warning("加载饰品失败: "+e.getMessage());}
        p.getLogger().info("已加载 "+data.size()+" 玩家饰品");}
    public CompletableFuture<Void> saveAsync(){return CompletableFuture.runAsync(this::save);}
    public void save(){if(c==null)return;
        try{c.setAutoCommit(false);
            try(PreparedStatement d=c.prepareStatement("DELETE FROM accessories");
                PreparedStatement i=c.prepareStatement("INSERT OR REPLACE INTO accessories VALUES(?,?,?)")){
                d.executeUpdate();
                for(var e:data.entrySet())for(var e2:e.getValue().entrySet()){i.setString(1,e.getKey().toString());i.setString(2,e2.getKey());i.setString(3,e2.getValue());i.addBatch();}
                i.executeBatch();c.commit();
            }catch(SQLException e){c.rollback();throw e;}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){p.getLogger().severe("保存饰品失败: "+e.getMessage());}}
    public void close(){if(c!=null)try{c.close();}catch(SQLException ignored){}}
    public Map<UUID,Map<String,String>> data(){return data;}
}
