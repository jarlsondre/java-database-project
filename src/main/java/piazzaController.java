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
	

	/**
	 * Hjelpemetode som registrerer en post i databasen. Dvs et reply eller en thread(Den første posten).
	 * */
	private boolean makePost(int threadID, String content, String postName,  String folderName, String courseID, int courseYear, String tag, boolean anonymous) {
		if(this.email == null) {
			throw new IllegalStateException("Du må være logget inn for å poste.");
		}
		try {
			// Sjekker om brukeren er oppmeldt i emnet og om man har lov å poste anonymt i kurset som hører til folderen
			PreparedStatement statement = this.conn.prepareStatement("select allowAnonumous from user inner join memberOfCourse on user.email = memberOfCourse.email \n" + 
					"	inner join courseInYear on memberOfCourse.courseID = courseInYear.courseID\n" + 
					"	where user.email = (?) and courseInYear.courseID = (?) and courseInYear.courseYear = (?)");
			statement.setString(1, this.email);
			statement.setString(2, courseID);
			statement.setInt(3, courseYear);
			ResultSet rs = statement.executeQuery();
			boolean anonymousAllowed = false;
			if(!rs.next()) {
				throw new IllegalArgumentException("Du er ikke oppmeldt i dette emnet.");
			}
			anonymousAllowed = rs.getBoolean("allowAnonumous");
		
			// Setter inn posten
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
			
			// Sjekker om det er lovlig å være anonym.
			if (anonymous && !anonymousAllowed) {
				throw new IllegalArgumentException("Course doesn't allow anonymous posts.");
			}
			statement.setBoolean(7, anonymous);
			
			// Setter tiden
			long milliseconds = System.currentTimeMillis();
			statement.setTime(8, new Time(milliseconds));
			statement.execute();
		} catch(SQLException e) {
			throw new RuntimeException("Det oppsto en feil", e);
		}
		System.out.println(content + "\n" + "Din post er lagret");
		return true;
	}
	
	
	/**
	 * Metode som poster en ny thread.
	 * */
	public boolean post(String content, String postName,  String folderName, String courseID, int courseYear, String tag, boolean anonymous) {
		// Bruker hjelpemetoden makePost
		makePost(this.totalPosts + 1, content, postName, folderName, courseID, courseYear, tag, anonymous);
		PreparedStatement statement;
		try {
			statement = this.conn.prepareStatement("INSERT INTO threadInFolder VALUES ( (?), (?), (?), (?) )");
			statement.setInt(1, this.totalPosts);
			statement.setString(2, folderName);
			statement.setString(3, courseID);
			statement.setInt(4, courseYear);
			statement.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	/**
	 * Metode som poster en ny thread uten tag.
	 * */
	public boolean post(String content, String postName,  String folderName, String courseID, int courseYear, boolean anonymous) {
		return post(content, postName,  folderName, courseID, courseYear, null, anonymous);
	}
	
	
	/**
	 * Metode som lager et nytt reply til en tidligere thread.
	 * */
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
			// Henter folderName og courseName til posten vi svarer på
			statement = this.conn.prepareStatement("select folderName, courseInYear.courseID, courseInYear.courseYear\n" +
					" from post inner join threadInFolder on threadInFolder.postID = post.postID\n" + 
					" inner join courseInYear on courseInYear.courseID = threadInFolder.folderCourseID \n" + 
					" and courseInYear.courseYear = threadInFolder.folderCourseYear\n" + 
					" inner join course on course.courseID = courseInYear.courseID\n" + 
					" where post.postID = (?)");
			statement.setInt(1, threadID);
			rs = statement.executeQuery();
			rs.next();
			String folderName = rs.getString("folderName");
			String courseID = rs.getString("courseID");
			int courseYear = rs.getInt("courseYear");
			this.makePost(threadID, replyText, replyName,  folderName, courseID, courseYear, null, isAnonymous);
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

	/**
	 * Søker etter postene i emnet som inneholder "content"
	 * */
	public Collection<Integer> searchFor(String content, String courseID, int year) {
		// Sjekker om brukeren er innlogget
		if(this.email == null) {
			throw new IllegalStateException("Du må være logget inn for å poste.");
		}
		Collection<Integer> lst = new ArrayList<Integer>();
		// Sjekker at brukeren er medlem av emnet dette året.
		try {
			PreparedStatement statement = this.conn.prepareStatement("select courseID from user inner join memberOfCourse on user.email = memberOfCourse.email \n" + 
					"	where memberOfCourse.courseID = (?) and memberOfCourse.mocYear = (?)");
			statement.setString(1, courseID);
			statement.setInt(2, year);
			ResultSet rs = statement.executeQuery();
			if(!rs.next()) {
				throw new IllegalArgumentException("Du er ikke oppmeldt i dette faget.");
			}
			// Finner alle posts i dette emnet(courseInYear) som inneholder content
			statement = this.conn.prepareStatement("select post.postId from (select post.postID, courseInYear.courseID, courseInYear.courseYear from courseInYear inner join folder on folder.courseID = courseInYear.courseID\n" + 
					"	and folder.folderYear = courseInYear.courseYear\n" + 
					"	inner join threadInFolder on threadInFolder.folderCourseID = folder.courseID\n" + 
					"    and threadInFolder.folderCourseYear = folder.folderYear\n" + 
					"    and threadInFolder.folderName = folder.folderName \n" + 
					"    inner join post on post.postID = threadInFolder.postID) as A \n" + 
					"    inner join post on post.threadID = A.postID\n" + 
					"    where A.courseID = (?) and A.courseYear = (?) and (post.content like (?) or post.postName like (?))");
			statement.setString(1, courseID);
			statement.setInt(2, year);
			statement.setString(3, "%" + content + "%");
			statement.setString(4, "%" + content + "%");
			rs = statement.executeQuery();
			lst = new ArrayList<Integer>();
			while(rs.next()) {
				lst.add(rs.getInt("post.postID"));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lst;
	}

	public static void main(String[] args) {
		piazzaController controller = new piazzaController();
		controller.logInUser("bendik@gmail.com", "bendik");
		controller.post("min første post", "første", "eksamen", "TDT4100", 2021, "question", true);
		controller.post("min andre post", "andre", "eksamen", "TDT4100", 2021, false);
		controller.post("min andre", "andre", "eksamen", "TDT4100", 2021, false);
		controller.post("min andre", "post", "eksamen", "TDT4100", 2021, false);
		controller.post("min andre post", "post", "eksamen", "TDT4145", 2021, false);
		controller.logOutUser();
		controller.logInUser("jarl@gmail.com", "jarl");
		controller.replyTo(1, "hei", "første reply", true);
		// Spør etter postene som inneholder "post"
		Collection<Integer> lst = controller.searchFor("post", "TDT4100", 2021);
		System.out.println("Disse postene inneholder post:");
		System.out.println(lst);
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