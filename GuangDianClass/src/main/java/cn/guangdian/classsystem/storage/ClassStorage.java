package cn.guangdian.classsystem.storage;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File; import java.sql.*; import java.util.*; import java.util.concurrent.*;
/** 职业系统 SQLite 存储 - 替代原有单文件 playerdata.yml（多玩家并发写同一个文件会损坏） */
public class ClassStorage {
    private final JavaPlugin p; private final File f; private Connection c;
    private final Map<UUID,Map<String,Object>> d=new ConcurrentHashMap<>();

    public ClassStorage(JavaPlugin pp){p=pp;f=new File(p.getDataFolder(),"class.db");}
    public boolean init(){try{Class.forName("org.sqlite.JDBC");f.getParentFile().mkdirs();
        c=DriverManager.getConnection("jdbc:sqlite:"+f.getAbsolutePath());
        try(Statement s=c.createStatement()){
            s.execute("CREATE TABLE IF NOT EXISTS class_data(uuid TEXT PRIMARY KEY,class_id TEXT DEFAULT 'novice',tier INTEGER DEFAULT 1,exp BIGINT DEFAULT 0,advancement INTEGER DEFAULT 0,total_exp BIGINT DEFAULT 0,last_update BIGINT DEFAULT 0,attr_points INTEGER DEFAULT 50,used_attr INTEGER DEFAULT 0)");
            s.execute("CREATE TABLE IF NOT EXISTS class_attrs(uuid TEXT,attr TEXT,points INTEGER,PRIMARY KEY(uuid,attr))");
            s.execute("CREATE TABLE IF NOT EXISTS class_skills(uuid TEXT PRIMARY KEY,hotbar TEXT,unlocked TEXT)");
        }
        p.getLogger().info("SQLite 职业已初始化");return true;
    }catch(Exception e){p.getLogger().severe("SQLite 职业失败: "+e.getMessage());return false;}}

    public void load(){if(c==null)return;d.clear();
        try(Statement s=c.createStatement()){
            ResultSet r=s.executeQuery("SELECT * FROM class_data");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));Map<String,Object> m=new HashMap<>();
                m.put("classId",r.getString("class_id"));m.put("tier",r.getInt("tier"));m.put("exp",r.getLong("exp"));m.put("advancementLevel",r.getInt("advancement"));m.put("totalExp",r.getLong("total_exp"));m.put("lastUpdateTime",r.getLong("last_update"));m.put("attributePoints",r.getInt("attr_points"));m.put("usedAttributePoints",r.getInt("used_attr"));m.put("allocatedAttributes",new HashMap<String,Integer>());d.put(u,m);}
            r=s.executeQuery("SELECT * FROM class_attrs");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));Map<String,Object> m=d.get(u);if(m!=null)((Map<String,Integer>)m.get("allocatedAttributes")).put(r.getString("attr"),r.getInt("points"));}
            r=s.executeQuery("SELECT * FROM class_skills");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));Map<String,Object> m=d.get(u);if(m!=null){m.put("hotbarBindings",r.getString("hotbar"));m.put("unlockedSkills",r.getString("unlocked"));}}
        }catch(SQLException e){p.getLogger().warning("加载职业失败: "+e.getMessage());}
        p.getLogger().info("已加载 "+d.size()+" 职业玩家");}

    public CompletableFuture<Void> saveAsync(UUID uuid,Map<String,Object> data){
        return CompletableFuture.runAsync(()->{d.put(uuid,data);savePlayer(uuid,data);});}
    public CompletableFuture<Void> saveAllAsync(){
        return CompletableFuture.runAsync(()->{for(var e:d.entrySet())savePlayer(e.getKey(),e.getValue());});}
    public void saveAll(){for(var e:d.entrySet())savePlayer(e.getKey(),e.getValue());}
    public void close(){if(c!=null)try{c.close();}catch(SQLException ignored){}}

    private void savePlayer(UUID uuid,Map<String,Object> m){
        if(c==null)return;
        try{String sql="INSERT OR REPLACE INTO class_data VALUES(?,?,?,?,?,?,?,?,?)";
            try(PreparedStatement ps=c.prepareStatement(sql)){
                ps.setString(1,uuid.toString());ps.setString(2,(String)m.getOrDefault("classId","novice"));ps.setInt(3,(int)m.getOrDefault("tier",1));ps.setLong(4,(long)m.getOrDefault("exp",0L));ps.setInt(5,(int)m.getOrDefault("advancementLevel",0));ps.setLong(6,(long)m.getOrDefault("totalExp",0L));ps.setLong(7,(long)m.getOrDefault("lastUpdateTime",System.currentTimeMillis()));ps.setInt(8,(int)m.getOrDefault("attributePoints",50));ps.setInt(9,(int)m.getOrDefault("usedAttributePoints",0));ps.executeUpdate();}
            try(PreparedStatement pd=c.prepareStatement("DELETE FROM class_attrs WHERE uuid=?");PreparedStatement pi=c.prepareStatement("INSERT INTO class_attrs VALUES(?,?,?)")){
                pd.setString(1,uuid.toString());pd.executeUpdate();
                Map<String,Integer> attrs=(Map<String,Integer>)m.get("allocatedAttributes");if(attrs!=null)for(var a:attrs.entrySet()){pi.setString(1,uuid.toString());pi.setString(2,a.getKey());pi.setInt(3,a.getValue());pi.addBatch();}pi.executeBatch();}
            try(PreparedStatement ps=c.prepareStatement("INSERT OR REPLACE INTO class_skills VALUES(?,?,?)")){
                ps.setString(1,uuid.toString());ps.setString(2,(String)m.getOrDefault("hotbarBindings",""));ps.setString(3,(String)m.getOrDefault("unlockedSkills",""));ps.executeUpdate();}
        }catch(SQLException e){p.getLogger().severe("保存职业失败: "+uuid+" - "+e.getMessage());}}
    public Map<UUID,Map<String,Object>> cache(){return d;}
}
