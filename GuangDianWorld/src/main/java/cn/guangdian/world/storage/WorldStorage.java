package cn.guangdian.world.storage;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File; import java.sql.*; import java.util.*; import java.util.concurrent.*;
public class WorldStorage {
    private final JavaPlugin p; private final File f; private Connection c;
    private final List<Map<String,Object>> worlds=new CopyOnWriteArrayList<>(); // List of world config maps
    public WorldStorage(JavaPlugin pp){p=pp;f=new File(p.getDataFolder(),"worlds.db");}
    public boolean init(){try{Class.forName("org.sqlite.JDBC");f.getParentFile().mkdirs();
        c=DriverManager.getConnection("jdbc:sqlite:"+f.getAbsolutePath());
        try(Statement s=c.createStatement()){s.execute("CREATE TABLE IF NOT EXISTS worlds(name TEXT PRIMARY KEY,alias TEXT DEFAULT '',environment TEXT DEFAULT 'normal',difficulty TEXT DEFAULT 'normal',gamemode TEXT DEFAULT 'survival',pvp INTEGER DEFAULT 1,allow_flight INTEGER DEFAULT 0,allow_weather INTEGER DEFAULT 1,hunger INTEGER DEFAULT 1,keep_spawn_mem INTEGER DEFAULT 0,auto_load INTEGER DEFAULT 1,mob_spawn INTEGER DEFAULT 1,fire_tick INTEGER DEFAULT 1,keep_inv INTEGER DEFAULT 0,respawn_world TEXT DEFAULT '',generator TEXT DEFAULT '',spawn_x REAL DEFAULT 0,spawn_y REAL DEFAULT 64,spawn_z REAL DEFAULT 0,spawn_yaw REAL DEFAULT 0,spawn_pitch REAL DEFAULT 0)");}
        p.getLogger().info("SQLite 世界已初始化");return true;
    }catch(Exception e){p.getLogger().severe("SQLite 世界失败: "+e.getMessage());return false;}}
    public void load(){if(c==null)return;worlds.clear();
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM worlds")){
            ResultSetMetaData m=r.getMetaData();
            while(r.next()){Map<String,Object> w=new HashMap<>();for(int i=1;i<=m.getColumnCount();i++)w.put(m.getColumnName(i),r.getObject(i));worlds.add(w);}
        }catch(SQLException e){p.getLogger().warning("加载世界失败: "+e.getMessage());}
        p.getLogger().info("已加载 "+worlds.size()+" 世界");}
    public CompletableFuture<Void> saveAsync(){return CompletableFuture.runAsync(this::save);}
    public void save(){if(c==null)return;
        try{c.setAutoCommit(false);
            try(PreparedStatement d=c.prepareStatement("DELETE FROM worlds");
                PreparedStatement i=c.prepareStatement("INSERT INTO worlds VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")){
                d.executeUpdate();
                for(Map<String,Object> w:worlds){i.setString(1,(String)w.getOrDefault("name",""));i.setString(2,(String)w.getOrDefault("alias",""));i.setString(3,(String)w.getOrDefault("environment","normal"));i.setString(4,(String)w.getOrDefault("difficulty","normal"));i.setString(5,(String)w.getOrDefault("gamemode","survival"));i.setInt(6,toInt(w.get("pvp"),1));i.setInt(7,toInt(w.get("allow_flight"),0));i.setInt(8,toInt(w.get("allow_weather"),1));i.setInt(9,toInt(w.get("hunger"),1));i.setInt(10,toInt(w.get("keep_spawn_mem"),0));i.setInt(11,toInt(w.get("auto_load"),1));i.setInt(12,toInt(w.get("mob_spawn"),1));i.setInt(13,toInt(w.get("fire_tick"),1));i.setInt(14,toInt(w.get("keep_inv"),0));i.setString(15,(String)w.getOrDefault("respawn_world",""));i.setString(16,(String)w.getOrDefault("generator",""));i.setDouble(17,toDouble(w.get("spawn_x"),0));i.setDouble(18,toDouble(w.get("spawn_y"),64));i.setDouble(19,toDouble(w.get("spawn_z"),0));i.setDouble(20,toDouble(w.get("spawn_yaw"),0));i.setDouble(21,toDouble(w.get("spawn_pitch"),0));i.addBatch();}
                i.executeBatch();c.commit();
            }catch(SQLException e){c.rollback();throw e;}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){p.getLogger().severe("保存世界失败: "+e.getMessage());}}
    private int toInt(Object o,int d){return o instanceof Number?((Number)o).intValue():d;}
    private double toDouble(Object o,double d){return o instanceof Number?((Number)o).doubleValue():d;}
    public void close(){if(c!=null)try{c.close();}catch(SQLException ignored){}}
    public List<Map<String,Object>> worlds(){return worlds;}
}
