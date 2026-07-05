package cn.guangdian.collection.storage;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File; import java.sql.*; import java.util.*; import java.util.concurrent.*;
public class CollectionStorage {
    private final JavaPlugin p; private final File f; private Connection c;
    private final Map<UUID,Map<String,Long>> items=new ConcurrentHashMap<>(); // entryId->collectedAt
    private final Map<UUID,Map<String,Integer>> progress=new ConcurrentHashMap<>(); // category->count
    public CollectionStorage(JavaPlugin pp){p=pp;f=new File(p.getDataFolder(),"collection.db");}
    public boolean init(){try{Class.forName("org.sqlite.JDBC");f.getParentFile().mkdirs();
        c=DriverManager.getConnection("jdbc:sqlite:"+f.getAbsolutePath());
        try(Statement s=c.createStatement()){s.execute("CREATE TABLE IF NOT EXISTS collected(uuid TEXT,entry_id TEXT,collected_at BIGINT,PRIMARY KEY(uuid,entry_id))");s.execute("CREATE TABLE IF NOT EXISTS cat_progress(uuid TEXT,category TEXT,count INTEGER,PRIMARY KEY(uuid,category))");}
        p.getLogger().info("SQLite 图鉴已初始化");return true;
    }catch(Exception e){p.getLogger().severe("SQLite 图鉴失败: "+e.getMessage());return false;}}
    public void load(){if(c==null)return;items.clear();progress.clear();
        try(Statement s=c.createStatement()){ResultSet r=s.executeQuery("SELECT * FROM collected");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));items.computeIfAbsent(u,k->new ConcurrentHashMap<>()).put(r.getString("entry_id"),r.getLong("collected_at"));}
            r=s.executeQuery("SELECT * FROM cat_progress");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));progress.computeIfAbsent(u,k->new ConcurrentHashMap<>()).put(r.getString("category"),r.getInt("count"));}
        }catch(SQLException e){p.getLogger().warning("加载图鉴失败: "+e.getMessage());}
        p.getLogger().info("已加载图鉴: "+items.size()+" 玩家");}
    public CompletableFuture<Void> saveAsync(){return CompletableFuture.runAsync(this::save);}
    public void save(){if(c==null)return;
        try{c.setAutoCommit(false);
            try(PreparedStatement d1=c.prepareStatement("DELETE FROM collected");PreparedStatement i1=c.prepareStatement("INSERT OR REPLACE INTO collected VALUES(?,?,?)");
                PreparedStatement d2=c.prepareStatement("DELETE FROM cat_progress");PreparedStatement i2=c.prepareStatement("INSERT OR REPLACE INTO cat_progress VALUES(?,?,?)")){
                d1.executeUpdate();d2.executeUpdate();
                for(var e:items.entrySet())for(var e2:e.getValue().entrySet()){i1.setString(1,e.getKey().toString());i1.setString(2,e2.getKey());i1.setLong(3,e2.getValue());i1.addBatch();}
                i1.executeBatch();
                for(var e:progress.entrySet())for(var e2:e.getValue().entrySet()){i2.setString(1,e.getKey().toString());i2.setString(2,e2.getKey());i2.setInt(3,e2.getValue());i2.addBatch();}
                i2.executeBatch();c.commit();
            }catch(SQLException e){c.rollback();throw e;}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){p.getLogger().severe("保存图鉴失败: "+e.getMessage());}}
    public void close(){if(c!=null)try{c.close();}catch(SQLException ignored){}}
    public Map<UUID,Map<String,Long>> items(){return items;}
    public Map<UUID,Map<String,Integer>> progress(){return progress;}
}
