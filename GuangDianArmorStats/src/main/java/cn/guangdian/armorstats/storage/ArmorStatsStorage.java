package cn.guangdian.armorstats.storage;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File; import java.sql.*; import java.util.*; import java.util.concurrent.*;
/** 装备属性 SQLite 存储 - 替代原有的每玩家 YAML 文件 */
public class ArmorStatsStorage {
    private final JavaPlugin p; private final File f; private Connection c;
    private final Map<UUID,Map<String,Object>> d=new ConcurrentHashMap<>();
    public ArmorStatsStorage(JavaPlugin pp){p=pp;f=new File(p.getDataFolder(),"armorstats.db");}
    public boolean init(){try{Class.forName("org.sqlite.JDBC");f.getParentFile().mkdirs();
        c=DriverManager.getConnection("jdbc:sqlite:"+f.getAbsolutePath());
        try(Statement s=c.createStatement()){
            s.execute("CREATE TABLE IF NOT EXISTS stats(uuid TEXT PRIMARY KEY,health REAL DEFAULT -1,max_health REAL DEFAULT 20,last_save BIGINT DEFAULT 0)");
            s.execute("CREATE TABLE IF NOT EXISTS armor_stats(uuid TEXT PRIMARY KEY,maxHealth REAL,minAttack REAL,maxAttack REAL,defenseMin REAL,defenseMax REAL,critChance REAL,critDamage REAL,lifesteal REAL,healthRegen REAL,dodge REAL,reflect REAL,reflectPct REAL,lifestealResist REAL,critResist REAL,critDmgResist REAL,parry REAL,pvpMinAtk REAL,pvpMaxAtk REAL,pvpDefMin REAL,pvpDefMax REAL,moveSpeed REAL,poison REAL,freeze REAL,blind REAL,expBonus REAL,lifestealMult REAL)");
            s.execute("CREATE TABLE IF NOT EXISTS armor_skills(uuid TEXT,skill TEXT,PRIMARY KEY(uuid,skill))");
        }
        p.getLogger().info("SQLite 装备属性已初始化");return true;
    }catch(Exception e){p.getLogger().severe("SQLite 装备属性失败: "+e.getMessage());return false;}}
    public void load(){if(c==null)return;d.clear();
        try(Statement s=c.createStatement()){ResultSet r=s.executeQuery("SELECT * FROM stats");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));Map<String,Object> m=new HashMap<>();m.put("health",r.getDouble("health"));m.put("maxHealth",r.getDouble("max_health"));m.put("lastSave",r.getLong("last_save"));m.put("armorStats",new HashMap<String,Double>());m.put("armorSkills",new ArrayList<String>());d.put(u,m);}
            r=s.executeQuery("SELECT * FROM armor_stats");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));Map<String,Object> m=d.get(u);if(m!=null){Map<String,Double> stats=new HashMap<>();ResultSetMetaData meta=r.getMetaData();for(int i=1;i<=meta.getColumnCount();i++){String col=meta.getColumnName(i);if(!col.equals("uuid")){stats.put(col,r.getDouble(i));}}m.put("armorStats",stats);}}
            r=s.executeQuery("SELECT * FROM armor_skills");
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));Map<String,Object> m=d.get(u);if(m!=null)((List<String>)m.get("armorSkills")).add(r.getString("skill"));}
        }catch(SQLException e){p.getLogger().warning("加载装备属性失败: "+e.getMessage());}
        p.getLogger().info("已加载 "+d.size()+" 玩家装备属性");}
    public CompletableFuture<Void> saveAsync(UUID uuid,Map<String,Object> m){return CompletableFuture.runAsync(()->{d.put(uuid,m);savePlayer(uuid,m);});}
    public CompletableFuture<Void> saveAllAsync(){return CompletableFuture.runAsync(()->{for(var e:d.entrySet())savePlayer(e.getKey(),e.getValue());});}
    public void saveAll(){for(var e:d.entrySet())savePlayer(e.getKey(),e.getValue());}
    public void close(){if(c!=null)try{c.close();}catch(SQLException ignored){}}
    private void savePlayer(UUID uuid,Map<String,Object> m){
        if(c==null)return;
        try{try(PreparedStatement ps=c.prepareStatement("INSERT OR REPLACE INTO stats VALUES(?,?,?,?)")){ps.setString(1,uuid.toString());ps.setDouble(2,(double)m.getOrDefault("health",-1.0));ps.setDouble(3,(double)m.getOrDefault("maxHealth",20.0));ps.setLong(4,(long)m.getOrDefault("lastSave",0L));ps.executeUpdate();}
            Map<String,Double> stats=(Map<String,Double>)m.get("armorStats");
            if(stats!=null){String cols="maxHealth,minAttack,maxAttack,defenseMin,defenseMax,critChance,critDamage,lifesteal,healthRegen,dodge,reflect,reflectPct,lifestealResist,critResist,critDmgResist,parry,pvpMinAtk,pvpMaxAtk,pvpDefMin,pvpDefMax,moveSpeed,poison,freeze,blind,expBonus,lifestealMult";
                try(PreparedStatement ps=c.prepareStatement("INSERT OR REPLACE INTO armor_stats VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")){ps.setString(1,uuid.toString());String[]cs=cols.split(",");for(int i=0;i<cs.length;i++)ps.setDouble(i+2,stats.getOrDefault(cs[i],0.0));ps.executeUpdate();}}
            try(PreparedStatement pd=c.prepareStatement("DELETE FROM armor_skills WHERE uuid=?");PreparedStatement pi=c.prepareStatement("INSERT OR REPLACE INTO armor_skills VALUES(?,?)")){
                pd.setString(1,uuid.toString());pd.executeUpdate();List<String> skills=(List<String>)m.get("armorSkills");if(skills!=null)for(String s:skills){pi.setString(1,uuid.toString());pi.setString(2,s);pi.addBatch();}pi.executeBatch();}
        }catch(SQLException e){p.getLogger().severe("保存装备属性失败: "+uuid+" - "+e.getMessage());}}
    public Map<UUID,Map<String,Object>> cache(){return d;}
}
