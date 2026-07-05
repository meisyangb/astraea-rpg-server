package cn.guangdian.bank.storage;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File; import java.sql.*; import java.util.*; import java.util.concurrent.*;
public class BankStorage {
    private final JavaPlugin p; private final File f; private Connection c;
    private final Map<UUID,long[]> accs=new ConcurrentHashMap<>(); // [balance, creditScore, lastInterest]
    public BankStorage(JavaPlugin pp){p=pp;f=new File(p.getDataFolder(),"bank.db");}
    public boolean init(){try{Class.forName("org.sqlite.JDBC");f.getParentFile().mkdirs();
        c=DriverManager.getConnection("jdbc:sqlite:"+f.getAbsolutePath());
        try(Statement s=c.createStatement()){s.execute("CREATE TABLE IF NOT EXISTS accounts(uuid TEXT PRIMARY KEY,balance BIGINT DEFAULT 0,credit_score INTEGER DEFAULT 100,last_interest BIGINT DEFAULT 0)");}
        p.getLogger().info("SQLite 银行已初始化");return true;
    }catch(Exception e){p.getLogger().severe("SQLite 银行失败: "+e.getMessage());return false;}}
    public void load(){if(c==null)return;accs.clear();
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM accounts")){
            while(r.next()){UUID u=UUID.fromString(r.getString("uuid"));accs.put(u,new long[]{r.getLong("balance"),r.getInt("credit_score"),r.getLong("last_interest")});}
        }catch(SQLException e){p.getLogger().warning("加载银行失败: "+e.getMessage());}
        p.getLogger().info("已加载 "+accs.size()+" 账户");}
    public CompletableFuture<Void> saveAsync(){return CompletableFuture.runAsync(this::save);}
    public void save(){if(c==null)return;
        try{c.setAutoCommit(false);
            try(PreparedStatement d=c.prepareStatement("DELETE FROM accounts");
                PreparedStatement i=c.prepareStatement("INSERT INTO accounts VALUES(?,?,?,?)")){
                d.executeUpdate();
                for(var e:accs.entrySet()){i.setString(1,e.getKey().toString());i.setLong(2,e.getValue()[0]);i.setInt(3,(int)e.getValue()[1]);i.setLong(4,e.getValue()[2]);i.addBatch();}
                i.executeBatch();c.commit();
            }catch(SQLException e){c.rollback();throw e;}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){p.getLogger().severe("保存银行失败: "+e.getMessage());}}
    public void close(){if(c!=null)try{c.close();}catch(SQLException ignored){}}
    public Map<UUID,long[]> accounts(){return accs;}
}
