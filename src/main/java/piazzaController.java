import java.sql.*;
import java.util.*;

public class piazzaController extends DBConn {
	
    private PreparedStatement regStatement;

	
	public piazzaController() {
        try { 
            regStatement = conn.prepareStatement("INSERT INTO course VALUES ( (?), (?), (?) )"); 
        } catch (Exception e) { 
            System.out.println("db error during prepare of insert into pizzatyper.");
        }
        try {
        	regStatement.setInt(1, 1);
			regStatement.setString(2, "OOP");
			regStatement.setString(3, "Høst");
			regStatement.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		piazzaController controller = new piazzaController();
	}
	
	
}