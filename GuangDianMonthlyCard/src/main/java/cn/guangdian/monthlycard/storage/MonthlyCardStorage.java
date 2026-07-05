package cn.guangdian.monthlycard.storage;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File; import java.sql.*; import java.util.*; import java.util.concurrent.*;
public class MonthlyCardStorage {
    private final JavaPlugin p; private final File f; private Connection c;
    private final Map<UUID,Map<String,Object>> data=new ConcurrentHashMap<>();
    public MonthlyCardStorage(JavaPlugin pp){p=pp;f=new File(p.getDataFolder(),"monthlycard.db");}
    public boolean init(){try{Class.forName("org.sqlite.JDBC");f.getParentFile().mkdirs();
        c=DriverManager.getConnection("jdbc:sqlite:"+f.getAbsolutePath());
        try(Statement s=c.createStatement()){s.execute("CREATE TABLE IF NOT EXISTS cards(uuid TEXT PRIMARY KEY,card_type TEXT,activate_time BIGINT,expire_time BIGINT,total_claimed INTEGER DEFAULT 0,last_claim BIGINT DEFAULT 0)");s.execute("CREATE TABLE IF NOT EXISTS claimed_days(uuid TEXT,day TEXT,PRIMARY KEY(uuid,day))");}
        p.getLogger().info("SQLite 月卡已初始化");return true;
    }catch(Exception e){p.getLogger().severe("SQLite 月卡失败: "+e.getMessage());return false;}}
    public void load(){if(c==null)return;data.clear();
        try(Statement s=c.createStatement()){ResultSet r=s.executeQuery("SELECT * FROM cards");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));Map<String,Object> m=new HashMap<>();m.put("card_type",r.getString("card_type"));m.put("activate_time",r.getLong("activate_time"));m.put("expire_time",r.getLong("expire_time"));m.put("total_claimed",r.getInt("total_claimed"));m.put("last_claim",r.getLong("last_claim"));m.put("claimed_days",new HashSet<String>());data.put(u,m);}
            r=s.executeQuery("SELECT * FROM claimed_days");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));Map<String,Object> m=data.get(u);if(m!=null)((Set<String>)m.get("claimed_days")).add(r.getString("day"));}
        }catch(SQLException e){p.getLogger().warning("加载月卡失败: "+e.getMessage());}
        p.getLogger().info("已加载 "+data.size()+" 月卡玩家");}
    public CompletableFuture<Void> saveAsync(){return CompletableFuture.runAsync(this::save);}
    public void save(){if(c==null)return;
        try{c.setAutoCommit(false);
            try(PreparedStatement d=c.prepareStatement("DELETE FROM cards");PreparedStatement i=c.prepareStatement("INSERT OR REPLACE INTO cards VALUES(?,?,?,?,?,?)");
                PreparedStatement dd=c.prepareStatement("DELETE FROM claimed_days");PreparedStatement ii=c.prepareStatement("INSERT OR REPLACE INTO claimed_days VALUES(?,?)")){
                d.executeUpdate();dd.executeUpdate();
                for(var e:data.entrySet()){var m=e.getValue();i.setString(1,e.getKey().toString());i.setString(2,(String)m.get("card_type"));i.setLong(3,(long)m.get("activate_time"));i.setLong(4,(long)m.get("expire_time"));i.setInt(5,(int)m.get("total_claimed"));i.setLong(6,(long)m.get("last_claim"));i.addBatch();
                    for(String day:(Set<String>)m.get("claimed_days")){ii.setString(1,e.getKey().toString());ii.setString(2,day);ii.addBatch();}}
                i.executeBatch();ii.executeBatch();c.commit();
            }catch(SQLException e){c.rollback();throw e;}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){p.getLogger().severe("保存月卡失败: "+e.getMessage());}}
    public void close(){if(c!=null)try{c.close();}catch(SQLException ignored){}}
    public Map<UUID,Map<String,Object>> data(){return data;}
}
