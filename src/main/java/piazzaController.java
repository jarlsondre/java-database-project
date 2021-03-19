import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class piazzaController extends DBConn {
	
    // private PreparedStatement regStatement;
    private String email;
    private int totalPosts;

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
	
	
	public boolean post(String content, String postName,  String folder, String tag, String courseName, boolean anonymous, int threadID) {
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
			statement.setString(2, courseName);
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
				}
			}
			if(year == 0) {
				throw new IllegalArgumentException("Du har ikke tilgang til denne folderen.");
			}
			statement = this.conn.prepareStatement("INSERT INTO post VALUES ( (?), (?), (?), (?), (?), (?), (?), (?))");
			this.totalPosts += 1;
			statement.setInt(1, this.totalPosts);
			statement.setInt(2, threadID);
			statement.setString(3, this.email);
			statement.setString(4, postName);
			statement.setString(5, content);
			if(tag != null) {
				statement.setString(6, tag);
			}
			else {
				statement.setNull(6, 0);
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
	
	public boolean post(String content, String postName,  String folder, String courseName, boolean anonymous, int threadID) {
		return this.post(content, postName, folder, null, courseName, anonymous, threadID);
	}

	public boolean replyTo(int postID, String replyText, String replyName, boolean isAnonymous) {
		try {
			// Vi må først finne threadID til posten vi skal svare på
			PreparedStatement statement = this.conn.prepareStatement(" select threadID from post\n" + 
					" where post.postID = (?)");
			statement.setInt(1, postID);
			ResultSet rs = statement.executeQuery();
			if (!rs.next()) {
				throw new IllegalArgumentException("Det finnes ikke post med denne postID-en");
			}
			int threadID = rs.getInt("threadID");
			statement = this.conn.prepareStatement("select folderName, courseName\n" + 
					" from post inner join threadInFolder on threadInFolder.postID = post.postID\n" + 
					" inner join courseInYear on courseInYear.courseID = threadInFolder.folderCourseID \n" + 
					" and courseInYear.courseYear = threadInFolder.folderCourseYear\n" + 
					" inner join course on course.courseID = courseInYear.courseID\n" + 
					" where post.threadID = (?)");
			statement.setInt(1, threadID);
			rs = statement.executeQuery();
			rs.next();
			String folderName = rs.getString("folderName");
			String courseName = rs.getString("courseName");
			this.post(replyText, replyName, folderName, courseName, isAnonymous, threadID);
			statement = this.conn.prepareStatement("insert into replyTo values ( (?), (?) )");
			statement.setInt(1, this.totalPosts);
			statement.setInt(2, threadID);
			statement.execute();
			System.out.println("Du har svart på post med id: " + this.totalPosts);
		} catch (SQLException throwables) {
			throw new RuntimeException("Det oppsto en feil i replyTo", throwables);
		}
		return true;
	}

	public boolean searchFor(String content) {
		return true;
	}

	public static void main(String[] args) {
		piazzaController controller = new piazzaController();
		controller.logInUser("bendik@gmail.com", "bendik");
		controller.post("Min første post", "første", "eksamen", "question", "TDT4100", false, 1);
		controller.post("Min andre post", "andre", "eksamen", "question", "TDT4100", false, 2);
		controller.replyTo(1, "hei", "første reply", true);
	}
	

//# insert into user values("bendik@gmail.com", "bendik", "bendik");
//-- insert into course values(1, "TDT4100", "vår");
//-- insert into courseInYear values(1, 2021, true); 
//-- insert into memberOfCourse values("bendik@gmail.com", 1, 2021, false);
//-- insert into folder values(1, 2021, "eksamen");

	// Skal sjekke om brukeren er en instruktør
	/*PreparedStatement statement = this.conn.prepareStatement("select isInstructor from post \n" +
			"\tinner join threadinfolder on post.threadID = threadinfolder.postID\n" +
			"    inner join courseinyear on folderCourseID = courseinyear.courseID\n" +
			"    inner join memberofcourse on courseinyear.courseID = memberofcourse.courseID\n" +
			"    where memberofcourse.email = (?) and post.postID = (?)");
			statement.setString(1, "jarl@gmail.com");
			statement.setInt(2, postID);
	ResultSet rs = statement.executeQuery();
	boolean isInstructor = false;
			if (rs.next()) {
		isInstructor = rs.getBoolean("isInstructor");
		System.out.println(isInstructor);
	}
			else {
		throw new RuntimeException("Det oppsto en feil i sjekking om brukeren er en instruktør ");
	}*/
}