
import java.sql.*;
import java.util.Properties;


public abstract class DBConn {
    protected Connection conn;
    public DBConn () {
    	this.connect();
    }
    public void connect() {
    	try {
            // Class.forName("com.mysql.jdbc.Driver").newInstance(); when you are using MySQL 5.7
	    Class.forName("com.mysql.cj.jdbc.Driver"); 
            // Properties for user and password.
            Properties p = new Properties();
            p.put("user", "root"); // Legg inn brukernavn
            p.put("password", ""); // Legg inn passord
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1/piazza?allowPublicKeyRetrieval=true&autoReconnect=true&useSSL=false",p);
        } catch (Exception e)
    	{
            throw new RuntimeException("Unable to connect", e);
    	}
    }
}
