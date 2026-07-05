package cn.guangdian.forge.storage;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File; import java.sql.*; import java.util.*; import java.util.concurrent.*;
public class ForgeStorage {
    private final JavaPlugin p; private final File f; private Connection c;
    private final Map<UUID,int[]> data=new ConcurrentHashMap<>(); // [level, exp, total, success] + recipes list separately
    private final Map<UUID,Set<String>> recipes=new ConcurrentHashMap<>();
    public ForgeStorage(JavaPlugin pp){p=pp;f=new File(p.getDataFolder(),"forge.db");}
    public boolean init(){try{Class.forName("org.sqlite.JDBC");f.getParentFile().mkdirs();
        c=DriverManager.getConnection("jdbc:sqlite:"+f.getAbsolutePath());
        try(Statement s=c.createStatement()){s.execute("CREATE TABLE IF NOT EXISTS players(uuid TEXT PRIMARY KEY,forge_level INTEGER DEFAULT 1,forge_exp BIGINT DEFAULT 0,total_forges INTEGER DEFAULT 0,success_forges INTEGER DEFAULT 0)");s.execute("CREATE TABLE IF NOT EXISTS learned_recipes(uuid TEXT,recipe_id TEXT,PRIMARY KEY(uuid,recipe_id))");}
        p.getLogger().info("SQLite 锻造已初始化");return true;
    }catch(Exception e){p.getLogger().severe("SQLite 锻造失败: "+e.getMessage());return false;}}
    public void load(){if(c==null)return;data.clear();recipes.clear();
        try(Statement s=c.createStatement()){ResultSet r=s.executeQuery("SELECT * FROM players");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));data.put(u,new int[]{r.getInt("forge_level"),(int)r.getLong("forge_exp"),r.getInt("total_forges"),r.getInt("success_forges")});}
            r=s.executeQuery("SELECT * FROM learned_recipes");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));recipes.computeIfAbsent(u,k->ConcurrentHashMap.newKeySet()).add(r.getString("recipe_id"));}
        }catch(SQLException e){p.getLogger().warning("加载锻造失败: "+e.getMessage());}
        p.getLogger().info("已加载 "+data.size()+" 锻造玩家");}
    public CompletableFuture<Void> saveAsync(){return CompletableFuture.runAsync(this::save);}
    public void save(){if(c==null)return;
        try{c.setAutoCommit(false);
            try(PreparedStatement d1=c.prepareStatement("DELETE FROM players");PreparedStatement i1=c.prepareStatement("INSERT OR REPLACE INTO players VALUES(?,?,?,?,?)");
                PreparedStatement d2=c.prepareStatement("DELETE FROM learned_recipes");PreparedStatement i2=c.prepareStatement("INSERT OR REPLACE INTO learned_recipes VALUES(?,?)")){
                d1.executeUpdate();d2.executeUpdate();
                for(var e:data.entrySet()){int[] v=e.getValue();i1.setString(1,e.getKey().toString());i1.setInt(2,v[0]);i1.setLong(3,v[1]);i1.setInt(4,v[2]);i1.setInt(5,v[3]);i1.addBatch();}
                i1.executeBatch();
                for(var e:recipes.entrySet())for(String r:e.getValue()){i2.setString(1,e.getKey().toString());i2.setString(2,r);i2.addBatch();}
                i2.executeBatch();c.commit();
            }catch(SQLException e){c.rollback();throw e;}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){p.getLogger().severe("保存锻造失败: "+e.getMessage());}}
    public void close(){if(c!=null)try{c.close();}catch(SQLException ignored){}}
    public Map<UUID,int[]> data(){return data;}
    public Map<UUID,Set<String>> recipes(){return recipes;}
}
