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
	 * Hjelpemetode som sjekker om man er logget inn.
	 * */
	private boolean isLoggetIn() {
		return this.email != null;
	}
	

	/**
	 * Hjelpemetode som registrerer en post i databasen. Dvs et reply eller en thread(Den første posten).
	 * */
	private boolean makePost(int threadID, String content, String postName,  String folderName, String courseID, int courseYear, String tag, boolean anonymous) {
		if(!isLoggetIn()) {
			throw new IllegalStateException("Du må være logget inn for å poste.");
		}
		try {
			// Henter ut om man har lov å poste anonymt i emnet
			PreparedStatement statement = this.conn.prepareStatement("select allowAnonumous from user inner join memberOfCourse on user.email = memberOfCourse.email \n" + 
					"	inner join courseInYear on memberOfCourse.courseID = courseInYear.courseID\n" + 
					"	where user.email = (?) and courseInYear.courseID = (?) and courseInYear.courseYear = (?)");
			statement.setString(1, this.email);
			statement.setString(2, courseID);
			statement.setInt(3, courseYear);
			ResultSet rs = statement.executeQuery();
			boolean anonymousAllowed = false;
			// Sjekker om brukeren er oppmeldt i emnet 
			if(!rs.next()) {
				throw new IllegalArgumentException("Du er ikke oppmeldt i dette emnet.");
			}
			anonymousAllowed = rs.getBoolean("allowAnonumous");
		
			// Lagrer posten i databasen
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
		if(!isLoggetIn()) {
			throw new IllegalStateException("Du må være logget inn for å svare på en post.");
		}
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
			// Henter folderName, courseId og courseYear til posten vi svarer på
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
		if(!isLoggetIn()) {
			throw new IllegalStateException("Du må være logget inn for å søke etter innhold.");
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
			// Finner alle posts i dette emnet(courseInYear) som inneholder content. (Enten i navnet på posten eller i innholdet)
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
			throw new RuntimeException("Det oppsto en feil i searchFor", e);
		}
		return lst;
	}
	
	/**
	 * Hjelpeklasse som brukes for å lagre statistikk om en bruker
	 * */
	public static class UserStatistics {
		private String email;
		private int antallSett;
		private int antallLaget;
		
		public UserStatistics(String email, int antallSett, int antallLaget) {
			this.email = email;
			this.antallSett = antallSett;
			this.antallLaget = antallLaget;
		}
		
		public String getEmail() {
			return this.email;
		}
		
		public int getAntallSett() {
			return this.antallSett;
		}
		
		public int getAntallLaget() {
			return this.antallLaget;
		}
		
		public String toString() {
			return this.email + " har sett " + this.antallSett + " og har laget " + this.antallLaget + " poster";
		}
		
	}
	
	
	/**
	 * Metode som henter statistikk for alle brukere som er medlem av emnet for det gitte året.
	 * */
	public List<UserStatistics> getStatistics(String courseID, int courseYear) {
		// Skal sjekke om brukeren er en instruktør
		if(!isLoggetIn()) {
			throw new IllegalStateException("Du må være logget inn for å søke etter innhold.");
		}
		List<UserStatistics> statistics = new ArrayList<piazzaController.UserStatistics>();
		PreparedStatement statement;
		try {
			// Spøring som finner ut om brukeren er instuktør i det gitte emnet.
			statement = this.conn.prepareStatement("select isInstructor from user inner join memberOfCourse on memberOfCourse.email = user.email\n" +
					"    where memberofcourse.email = (?) and memberOfCourse.courseID = (?) and memberOfCourse.mocYear = (?)");
			statement.setString(1, this.email);
			statement.setString(2, courseID);
			statement.setInt(3, courseYear);
			ResultSet rs = statement.executeQuery();
			boolean isInstructor = false;
			if (!rs.next()) {
				throw new IllegalArgumentException("Du er ikke medlem av dette emnet");
			}
			isInstructor = rs.getBoolean("isInstructor");
			System.out.println(isInstructor);
			if(!isInstructor) {
				throw new IllegalStateException("Du må være en instuktør i emnet for å kunne se statistikk");
			}
			// Spørring som henter statistikk om alle brukerene som er medlem av det gitte emnet.
			statement = this.conn.prepareStatement("select A.email, antallSett, antallLaget from (select user.email as email, count(viewedPost.email) as antallSett from user left join viewedPost on viewedPost.email = user.email\n" + 
					"where user.email in (select user.email from user inner join memberOfCourse on memberOfCourse.email = user.email\n" + 
					"where memberOfCourse.courseID = (?) and memberOfCourse.mocYear = (?) )\n" + 
					"group by user.email) as A\n" + 
					"left join (select user.email as email, count(post.userEmail) as antallLaget from user left join post on post.userEmail = user.email\n" + 
					"where post.postID in (select post.postID from post inner join threadInFolder on threadInFolder.postID = post.postID\n" + 
					"inner join folder on folder.courseID = threadInFolder.folderCourseID and folder.folderYear = threadInFolder.folderCourseYear\n" + 
					"and folder.folderName = threadInFolder.folderName\n" + 
					"where folder.courseID = (?) and folder.folderYear = (?) )\n" + 
					"group by user.email) as B on A.email = B.email\n" + 
					"order by antallSett desc");
			statement.setString(1, courseID);
			statement.setInt(2, courseYear);
			statement.setString(3, courseID);
			statement.setInt(4, courseYear);
			rs = statement.executeQuery();
			// Lager en liste med statistikk for hver bruker
			while(rs.next()) {
				String email = rs.getString("email");
				int antallSett = rs.getInt("antallSett");
				int antallLaget = rs.getInt("antallLaget");
				statistics.add(new UserStatistics(email, antallSett, antallLaget));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Det oppsto en feil under henting av statistikk", e);
		}
		return statistics;
	}
	

	public static void main(String[] args) {
		// Husk å kjøre setUp filen først.
		piazzaController controller = new piazzaController();
		controller.logInUser("bendik@gmail.com", "bendik");
		controller.post("min første post", "første", "eksamen", "TDT4100", 2021, "question", true);
		controller.post("min andre post", "andre", "eksamen", "TDT4100", 2021, false);
		controller.post("min andre", "andre", "eksamen", "TDT4100", 2021, false);
		controller.post("min andre", "post", "eksamen", "TDT4100", 2021, false);
		controller.post("min andre post", "post", "eksamen", "TDT4145", 2021, false);
		controller.logOutUser();
		controller.logInUser("jarl@gmail.com", "jarl");
		controller.post("min første post", "første", "eksamen", "TDT4100", 2021, "question", true);
		controller.replyTo(1, "hei", "første reply", true);
		
		// Spør etter postene som inneholder "post"
		Collection<Integer> lst = controller.searchFor("post", "TDT4100", 2021);
		System.out.println("Disse postene inneholder post:");
		System.out.println(lst);
		
		// Henter statistikk
		List<piazzaController.UserStatistics> lst1 = controller.getStatistics("TDT4100", 2021);
		System.out.println(lst1);
	}
}