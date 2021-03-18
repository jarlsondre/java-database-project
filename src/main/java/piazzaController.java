import java.sql.*;
import java.util.*;

public class piazzaController extends DBConn {
	
    private PreparedStatement regStatement;
    private String email;

	
	public piazzaController() {
//        try { 
//            regStatement = conn.prepareStatement("INSERT INTO course VALUES ( (?), (?), (?) )"); 
//        } catch (Exception e) { 
//            System.out.println("db error during prepare of insert into pizzatyper.");
//        }
//        try {
//        	regStatement.setInt(1, 1);
//			regStatement.setString(2, "OOP");
//			regStatement.setString(3, "Høst");
//			regStatement.execute();
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}
	
	/**
	 * Metode som logger en bruker inn.
	 * @param bruker email
	 * @param bruker passord
	 * */
	public boolean logInUser(String email, String passord) {
		try {
			PreparedStatement statement = this.conn.prepareStatement("select username from user where user.email=(?) and user.userPassword=(?)");
			statement.setString(1, email);
			statement.setString(2, passord);
			ResultSet rs = statement.executeQuery();
			if(rs.next()) {  // Hvis next er true, finnes en bruker med den eposten og det passordet som er oppgitt.
				this.email = rs.getString("username");
				System.out.println("Du er logget inn som " + this.email);
				return true;
			}
			else {				
				System.out.println("Ugyldig brukernavn eller passord.");
				return false;
			}
			
		} catch(SQLException e) {
			throw new RuntimeException("kan ikke logge inn", e);
		}
	}
	
	
	
	
	
	public static void main(String[] args) {
		piazzaController controller = new piazzaController();
		controller.logInUser("bendik@gmail.com", "bendik1");
	}
	
	
}