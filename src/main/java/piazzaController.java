import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
			PreparedStatement statement = this.conn.prepareStatement("select email from user where user.email=(?) and user.userPassword=(?)");
			statement.setString(1, email);
			statement.setString(2, passord);
			ResultSet rs = statement.executeQuery();
			if(rs.next()) {  // Hvis next er true, finnes en bruker med den eposten og det passordet som er oppgitt.
				this.email = rs.getString("email");
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
	
	
	// TODO Denne metoden er ikke testet
	public boolean post(String content, String postName,  String folder, String tag, String courseID, boolean anonymous) {
		if(this.email == null) {
			throw new IllegalStateException("Du må være logget inn for å poste.");
		}
		try {
			
			PreparedStatement statement = this.conn.prepareStatement("select courseInYear.courseID, courseInYear.courseYear, folder.folderName, allowAnonumous from user inner join memberOfCourse on user.email = memberOfCourse.email \n" + 
					"	inner join courseInYear on memberOfCourse.courseID = courseInYear.courseID\n" + 
					"    inner join course on course.courseID = courseInYear.courseID\n" + 
					"	inner join folder on folder.courseID = courseInYear.courseID and folder.folderYear = courseInYear.courseYear\n" + 
					"	where user.email = (?) and course.courseName = (?)\n" + 
					"	order by courseYear desc");
			statement.setString(1, this.email);
			statement.setString(2, courseID);
			System.out.println(statement);
			ResultSet rs = statement.executeQuery();
			String courseID1 = null;
			int year = 0;
			String folderName = null;
			boolean anonymousAllowed = false;
			while (rs.next()) {
				if(rs.getString("folder.folderName").equals(folder)) {
					courseID1 = rs.getString("courseInYear.courseID");
					year = rs.getInt("courseInYear.courseYear");
					folderName = rs.getString("folder.folderName");
					anonymousAllowed = rs.getBoolean("allowAnonumous");
					System.out.println(courseID1);
					System.out.println(year);
					System.out.println(folderName);
					System.out.println(anonymousAllowed);
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
			long milliseconds = System.currentTimeMillis();
			statement.setTime(8, new Time(milliseconds));
			statement.execute();
			statement = this.conn.prepareStatement("INSERT INTO threadInFolder VALUES ( (?), (?), (?), (?) )");
			statement.setInt(1, this.totalPosts);
			statement.setString(2, folderName);
			statement.setString(3, courseID1);
			statement.setInt(4, year);
			statement.execute();
		} catch(SQLException e) {
			throw new RuntimeException("Det oppsto en feil", e);
		}
		System.out.println(content + "\n" + "Din post er lagret");
		return true;
	}
	
	public boolean post(String content, String postName,  String folder, String courseID, boolean anonymous) {
		return this.post(content, postName, folder, null, courseID, anonymous);
	}
	
	
	
	
	
	
	public static void main(String[] args) {
		piazzaController controller = new piazzaController();
		controller.logInUser("bendik@gmail.com", "bendik");
		controller.post("Min første post", "første", "eksamen", "question", "TDT4100", false);
	}
	

//# insert into user values("bendik@gmail.com", "bendik", "bendik");
//-- insert into course values(1, "TDT4100", "vår");
//-- insert into courseInYear values(1, 2021, true); 
//-- insert into memberOfCourse values("bendik@gmail.com", 1, 2021, false);
//-- insert into folder values(1, 2021, "eksamen");
	
	
}