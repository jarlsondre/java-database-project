import java.sql.*;
import java.util.*;

public class piazzaController extends DBConn {
	
    // private PreparedStatement regStatement;
    private String email;
    private int totalPosts;

	
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
	
	
	/**
	 * Metode som logger en bruker ut.
	 * */
	public boolean logOutUser() {
		if(this.email != null) {
			this.email = null;
			System.out.println("Du er nå logget ut");
			return true;
		}
		throw new IllegalStateException("Du er ikke logget inn");
	}
	
	
	public boolean post(String content, String postName,  String folder, String tag, String courseID, boolean anonymous) {
		if(this.email == null) {
			throw new IllegalStateException("Du må være logget inn for å poste.");
		}
		try {
			
			PreparedStatement statement = this.conn.prepareStatement("select courceID, year, folder.name, allowAnonymous from user inner join memberOfCourse using email"
					+ " inner join courseInYear on memberOfCourse.courceID = courseInYear.courseID and memberOfCourse.year = courseInYear.year"
					+ " inner join folder on folder.courseID = courseInYear.courseID and folder.year = courseInYear.year"
					+ " where email = (?) and courseID = (?)"
					+ " ordered by year desc");
			statement.setString(1, this.email);
			statement.setString(2, courseID);
			ResultSet rs = statement.executeQuery();
			String courseID1;
			int year = 0;
			String folderName;
			boolean anonymousAllowed = false;
			while (rs.next()) {
				if(rs.getString("folder.name").equals(folder)) {
					courseID1 = rs.getString("courseID");
					year = rs.getInt("year");
					folderName = rs.getString("folder.name");
					anonymousAllowed = rs.getBoolean("allowAnonymous");
				}
			}
			if(year == 0) {
				throw new IllegalArgumentException("Du har ikke tilgang til denne folderen.");
			}
			statement = this.conn.prepareStatement("INSERT INTO post VALUES ( (?), (?), (?), (?), (?), (?), (?), (?))");
			this.totalPosts += 1;
			statement.setInt(1, this.totalPosts);
			statement.setInt(2, 1);
			statement.setString(3, this.email);
			statement.setString(4, postName);
			statement.setString(5, content);
			if(tag != null) {
				statement.setString(6, tag);
			}
			if (anonymous && !anonymousAllowed) {
				throw new IllegalArgumentException("Course doesn't allow anonymous posts.");
			}
			statement.setBoolean(7, anonymous);
			// TODO Sett statement time
			// TODO lag tuppel i threadInFolder skjemaet.
		} catch(SQLException e) {
			
		}
	}
	
	public boolean post(String content, String postName,  String folder, String courseID, boolean anonymous) {
		return this.post(content, postName, folder, null, courseID, anonymous);
	}
	
	
	
	
	
	
	public static void main(String[] args) {
		piazzaController controller = new piazzaController();
		// controller.logInUser("bendik@gmail.com", "bendik");
	}
	
	
}