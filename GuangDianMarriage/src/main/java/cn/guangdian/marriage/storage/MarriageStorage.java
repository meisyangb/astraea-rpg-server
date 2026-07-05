package cn.guangdian.marriage.storage;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File; import java.sql.*; import java.util.*; import java.util.concurrent.*;
public class MarriageStorage {
    private final JavaPlugin p; private final File f; private Connection c;
    public record M(String p1,String p2,long d,int lp,String n1,String n2){}
    private final List<M> list=new CopyOnWriteArrayList<>();
    public MarriageStorage(JavaPlugin pp){p=pp;f=new File(p.getDataFolder(),"marriage.db");}
    public boolean init(){try{Class.forName("org.sqlite.JDBC");f.getParentFile().mkdirs();
        c=DriverManager.getConnection("jdbc:sqlite:"+f.getAbsolutePath());
        try(Statement s=c.createStatement()){s.execute("CREATE TABLE IF NOT EXISTS marriages(id INTEGER PRIMARY KEY AUTOINCREMENT,p1 TEXT,p2 TEXT,marry_date BIGINT,love_points INTEGER DEFAULT 0,nick1 TEXT DEFAULT '',nick2 TEXT DEFAULT '')");}
        p.getLogger().info("SQLite 结婚已初始化");return true;
    }catch(Exception e){p.getLogger().severe("SQLite 结婚失败: "+e.getMessage());return false;}}
    public void load(){if(c==null)return;list.clear();
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM marriages")){
            while(r.next()){list.add(new M(r.getString("p1"),r.getString("p2"),r.getLong("marry_date"),r.getInt("love_points"),r.getString("nick1"),r.getString("nick2")));}
        }catch(SQLException e){p.getLogger().warning("加载结婚失败: "+e.getMessage());}
        p.getLogger().info("已加载 "+list.size()+" 对夫妻");}
    public CompletableFuture<Void> saveAsync(){return CompletableFuture.runAsync(this::save);}
    public void save(){if(c==null)return;
        try{c.setAutoCommit(false);
            try(PreparedStatement d=c.prepareStatement("DELETE FROM marriages");
                PreparedStatement i=c.prepareStatement("INSERT INTO marriages(p1,p2,marry_date,love_points,nick1,nick2) VALUES(?,?,?,?,?,?)")){
                d.executeUpdate();
                for(M m:list){i.setString(1,m.p1());i.setString(2,m.p2());i.setLong(3,m.d());i.setInt(4,m.lp());i.setString(5,m.n1());i.setString(6,m.n2());i.addBatch();}
                i.executeBatch();c.commit();
            }catch(SQLException e){c.rollback();throw e;}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){p.getLogger().severe("保存结婚失败: "+e.getMessage());}}
    public void close(){if(c!=null)try{c.close();}catch(SQLException ignored){}}
    public void add(String p1,String p2,long d,int lp,String n1,String n2){list.add(new M(p1,p2,d,lp,n1,n2));}
    public void remove(String p){list.removeIf(m->m.p1().equalsIgnoreCase(p)||m.p2().equalsIgnoreCase(p));}
    public List<M> all(){return list;}
    public void clear(){list.clear();}
}
