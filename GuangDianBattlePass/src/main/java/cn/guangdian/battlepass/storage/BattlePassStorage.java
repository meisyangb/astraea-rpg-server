package cn.guangdian.battlepass.storage;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File; import java.sql.*; import java.util.*; import java.util.concurrent.*;
/** 战令系统 SQLite 存储 */
public class BattlePassStorage {
    private final JavaPlugin p; private final File f; private Connection c;
    private final Map<UUID,Map<String,Object>> d=new ConcurrentHashMap<>();
    public BattlePassStorage(JavaPlugin pp){p=pp;f=new File(p.getDataFolder(),"battlepass.db");}
    public boolean init(){try{Class.forName("org.sqlite.JDBC");f.getParentFile().mkdirs();
        c=DriverManager.getConnection("jdbc:sqlite:"+f.getAbsolutePath());
        try(Statement s=c.createStatement()){s.execute("CREATE TABLE IF NOT EXISTS bp_data(uuid TEXT PRIMARY KEY,season_id INTEGER DEFAULT 1,level INTEGER DEFAULT 1,current_exp INTEGER DEFAULT 0,total_exp INTEGER DEFAULT 0,premium INTEGER DEFAULT 0,premium_time BIGINT DEFAULT 0)");s.execute("CREATE TABLE IF NOT EXISTS bp_claimed(uuid TEXT,reward_level INTEGER,is_premium INTEGER DEFAULT 0,PRIMARY KEY(uuid,reward_level,is_premium))");s.execute("CREATE TABLE IF NOT EXISTS bp_tasks(uuid TEXT,task_id TEXT,progress INTEGER DEFAULT 0,PRIMARY KEY(uuid,task_id))");}
        p.getLogger().info("SQLite 战令已初始化");return true;
    }catch(Exception e){p.getLogger().severe("SQLite 战令失败: "+e.getMessage());return false;}}
    public void load(){if(c==null)return;d.clear();
        try(Statement s=c.createStatement()){ResultSet r=s.executeQuery("SELECT * FROM bp_data");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));Map<String,Object> m=new HashMap<>();m.put("seasonId",r.getInt("season_id"));m.put("level",r.getInt("level"));m.put("currentExp",r.getInt("current_exp"));m.put("totalExp",r.getInt("total_exp"));m.put("premium",r.getInt("premium")==1);m.put("premiumPurchaseTime",r.getLong("premium_time"));m.put("claimedFree",new HashSet<Integer>());m.put("claimedPremium",new HashSet<Integer>());m.put("taskProgress",new HashMap<String,Integer>());m.put("lastUpdateTime",System.currentTimeMillis());d.put(u,m);}
            r=s.executeQuery("SELECT * FROM bp_claimed");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));Map<String,Object> m=d.get(u);if(m!=null){if(r.getInt("is_premium")==1)((Set<Integer>)m.get("claimedPremium")).add(r.getInt("reward_level"));else((Set<Integer>)m.get("claimedFree")).add(r.getInt("reward_level"));}}
            r=s.executeQuery("SELECT * FROM bp_tasks");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));Map<String,Object> m=d.get(u);if(m!=null)((Map<String,Integer>)m.get("taskProgress")).put(r.getString("task_id"),r.getInt("progress"));}
        }catch(SQLException e){p.getLogger().warning("加载战令失败: "+e.getMessage());}
        p.getLogger().info("已加载 "+d.size()+" 战令玩家");}
    public CompletableFuture<Void> saveAsync(UUID uuid,Map<String,Object> m){return CompletableFuture.runAsync(()->{d.put(uuid,m);savePlayer(uuid,m);});}
    public CompletableFuture<Void> saveAllAsync(){return CompletableFuture.runAsync(()->{for(var e:d.entrySet())savePlayer(e.getKey(),e.getValue());});}
    public void saveAll(){for(var e:d.entrySet())savePlayer(e.getKey(),e.getValue());}
    public void close(){if(c!=null)try{c.close();}catch(SQLException ignored){}}
    private void savePlayer(UUID uuid,Map<String,Object> m){
        if(c==null)return;
        try{String sql="INSERT OR REPLACE INTO bp_data VALUES(?,?,?,?,?,?,?)";
            try(PreparedStatement ps=c.prepareStatement(sql)){ps.setString(1,uuid.toString());ps.setInt(2,(int)m.getOrDefault("seasonId",1));ps.setInt(3,(int)m.getOrDefault("level",1));ps.setInt(4,(int)m.getOrDefault("currentExp",0));ps.setInt(5,(int)m.getOrDefault("totalExp",0));ps.setInt(6,Boolean.TRUE.equals(m.get("premium"))?1:0);ps.setLong(7,(long)m.getOrDefault("premiumPurchaseTime",0L));ps.executeUpdate();}
            try(PreparedStatement pd=c.prepareStatement("DELETE FROM bp_claimed WHERE uuid=?");PreparedStatement pi=c.prepareStatement("INSERT OR REPLACE INTO bp_claimed VALUES(?,?,?)")){
                pd.setString(1,uuid.toString());pd.executeUpdate();
                Set<Integer> free=(Set<Integer>)m.get("claimedFree");if(free!=null)for(int lv:free){pi.setString(1,uuid.toString());pi.setInt(2,lv);pi.setInt(3,0);pi.addBatch();}
                Set<Integer> prem=(Set<Integer>)m.get("claimedPremium");if(prem!=null)for(int lv:prem){pi.setString(1,uuid.toString());pi.setInt(2,lv);pi.setInt(3,1);pi.addBatch();}
                pi.executeBatch();}
            try(PreparedStatement pd=c.prepareStatement("DELETE FROM bp_tasks WHERE uuid=?");PreparedStatement pi=c.prepareStatement("INSERT OR REPLACE INTO bp_tasks VALUES(?,?,?)")){
                pd.setString(1,uuid.toString());pd.executeUpdate();
                Map<String,Integer> tasks=(Map<String,Integer>)m.get("taskProgress");if(tasks!=null)for(var t:tasks.entrySet()){pi.setString(1,uuid.toString());pi.setString(2,t.getKey());pi.setInt(3,t.getValue());pi.addBatch();}
                pi.executeBatch();}
        }catch(SQLException e){p.getLogger().severe("保存战令失败: "+uuid+" - "+e.getMessage());}}
    public Map<UUID,Map<String,Object>> cache(){return d;}
}
