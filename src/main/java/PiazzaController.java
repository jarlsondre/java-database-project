import javax.management.relation.RelationTypeNotFoundException;
import java.sql.*;
import java.util.*;

public class PiazzaController extends DBConn {
    private String email;
    private int totalPosts;

	/**
	 * Metode som logger en bruker inn.
	 * @param email brukerens email
	 * @param passord brukerens passord
	 * */
	public void logInUser(String email, String passord) {
		try {
			PreparedStatement statement = this.conn.prepareStatement(
					"select email from user where user.email=(?) and user.userPassword=(?)");
			statement.setString(1, email);
			statement.setString(2, passord);
			ResultSet rs = statement.executeQuery();
			if(rs.next()) {  // Hvis next er true, finnes en bruker med den eposten og det passordet som er oppgitt.
				this.email = rs.getString("email");
				System.out.println("Du er logget inn som " + this.email);
			}
			else {				
				System.out.println("Ugyldig brukernavn eller passord. \n");
			}
			
		} catch(SQLException e) {
			throw new RuntimeException("kan ikke logge inn", e);
		}
	}
	
	/**
	 * Metode som logger en bruker ut.
	 * */
	public void logOutUser() {
		if(this.email != null) {
			this.email = null;
			System.out.println("Du er nå logget ut\n");
		}
		else {
			throw new IllegalStateException("Du er ikke logget inn\n");
		}
	}
	
	/**
	 * Hjelpemetode som sjekker om man er logget inn.
	 * */
	private boolean isLoggedIn() {
		return this.email != null;
	}

	/**
	 * Hjelpemetode som registrerer en post i databasen. Dvs et reply eller en thread(Den første posten).
	 *
     * @param threadID id-en til threaden posten skal høre til
	 * @param content innholdet i posten
	 * @param postName navnet til posten
	 * @param courseID id-en til kurset som posten skal legges i
	 * @param courseYear året til kurset som posten skal legges i
	 * @param tag hvilken type post det er
	 * @param anonymous boolsk verdi som forteller om posten skal være anonym eller ikke
	 * */
	private void makePost(int threadID, String content, String postName, String courseID, int courseYear,
						  String tag, boolean anonymous) {

		if(!isLoggedIn()) {
			throw new IllegalStateException("Du må være logget inn for å poste.");
		}
		try {
			// Henter ut om man har lov å poste anonymt i emnet
			PreparedStatement statement = this.conn.prepareStatement(
					"select allowAnonumous from user inner join memberOfCourse on user.email = memberOfCourse.email \n" +
					"	inner join courseInYear on memberOfCourse.courseID = courseInYear.courseID\n" + 
					"	where user.email = (?) and courseInYear.courseID = (?) and courseInYear.courseYear = (?)");
			statement.setString(1, this.email);
			statement.setString(2, courseID);
			statement.setInt(3, courseYear);
			ResultSet rs = statement.executeQuery();
			boolean anonymousAllowed;

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
			Timestamp timestamp = new Timestamp(milliseconds);
			statement.setTimestamp(8, timestamp);
			statement.execute();
		} catch(SQLException e) {
			throw new RuntimeException("makePost feilet: ", e);
		}
		System.out.println("Din post er lagret");
	}

	/**
	 * Metode for å lage en ny thread
	 *
	 * @param content innholdet til den første posten i threaden
	 * @param postName navnet på den første posten i threaden
	 * @param folderName navnet på folderen som threaden skal høre til
	 * @param courseID id-en til kurset som threaden skal høre til
	 * @param courseYear året til kurset som threaden skal høre til
	 * @param tag tagen til threaden
	 * @param anonymous verdi som forteller om threaden skal være anonym eller ikke
	 */
	public void makeThread(String content, String postName, String folderName, String courseID, int courseYear,
							  String tag, boolean anonymous) {
		// Lager en post
		makePost(this.totalPosts + 1, content, postName, courseID, courseYear, tag, anonymous);
		PreparedStatement statement;
		try {
			// Plasserer posten som en thread inne i en folder
			statement = this.conn.prepareStatement("INSERT INTO threadInFolder VALUES ( (?), (?), (?), (?) )");
			statement.setInt(1, this.totalPosts);
			statement.setString(2, folderName);
			statement.setString(3, courseID);
			statement.setInt(4, courseYear);
			statement.execute();
		} catch (SQLException e) {
			throw new RuntimeException("utføringen av SQL-delen i makeThread feilet", e);
		}
	}

	/**
	 * Metode for å svare på en post
	 *
	 * @param postID id-en til posten som skal svares på
	 * @param replyText innholdet til svaret
	 * @param replyName navnet på svaret
	 * @param isAnonymous verdi som forteller om posten er anonym eller ikke
	 */
	public void makeReply(int postID, String replyText, String replyName, boolean isAnonymous) {
		if(!isLoggedIn()) {
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

			// Henter courseId og courseYear til posten vi svarer på
			statement = this.conn.prepareStatement("select courseInYear.courseID, courseInYear.courseYear\n" +
					" from post inner join threadInFolder on threadInFolder.postID = post.postID\n" + 
					" inner join courseInYear on courseInYear.courseID = threadInFolder.folderCourseID \n" + 
					" and courseInYear.courseYear = threadInFolder.folderCourseYear\n" + 
					" inner join course on course.courseID = courseInYear.courseID\n" + 
					" where post.postID = (?)");
			statement.setInt(1, threadID);
			rs = statement.executeQuery();
			rs.next();
			String courseID = rs.getString("courseID");
			int courseYear = rs.getInt("courseYear");

			// Lager posten
			this.makePost(threadID, replyText, replyName, courseID, courseYear, null, isAnonymous);

			// Lagrer at det er et svar
			statement = this.conn.prepareStatement("insert into replyTo values ( (?), (?) )");
			statement.setInt(1, this.totalPosts);
			statement.setInt(2, threadID);
			statement.execute();

			System.out.println("Du har svart på post med id: " + postID);
		} catch (SQLException throwables) {
			throw new RuntimeException("Det oppsto en feil i replyTo", throwables);
		}
	}

	/**
	 * Metode som finner alle poster som inneholder nøkkelfrasen.
	 *
	 * @param content nøkkelfrasen som skal søkes etter
	 * @param courseID id-en til kurset som det skal søkes i
	 * @param year året til kurset som det skal søkes i
	 * @return en Collection som inneholder id-ene til alle treffene på søket
	 */
	public Collection<Integer> searchFor(String content, String courseID, int year) {
		// Sjekker om brukeren er innlogget
		if(!isLoggedIn()) {
			throw new IllegalStateException("Du må være logget inn for å søke etter innhold.");
		}
		Collection<Integer> resultList;

		// Sjekker at brukeren er medlem av emnet dette året.
		try {
			PreparedStatement statement = this.conn.prepareStatement(
					"select courseID from user inner join memberOfCourse on user.email = memberOfCourse.email \n" +
					"	where memberOfCourse.courseID = (?) and memberOfCourse.mocYear = (?)");
			statement.setString(1, courseID);
			statement.setInt(2, year);
			ResultSet rs = statement.executeQuery();
			if(!rs.next()) {
				throw new IllegalArgumentException("Du er ikke oppmeldt i dette faget.");
			}
			// Finner alle posts i dette emnet som inneholder content. (Enten i navnet på posten eller i innholdet)
			statement = this.conn.prepareStatement(
					"select post.postId from (select post.postID, courseInYear.courseID, courseInYear.courseYear " +
					"from courseInYear inner join folder on folder.courseID = courseInYear.courseID\n" +
					"and folder.folderYear = courseInYear.courseYear\n" +
					"inner join threadInFolder on threadInFolder.folderCourseID = folder.courseID\n" +
					" and threadInFolder.folderCourseYear = folder.folderYear\n" +
					"    and threadInFolder.folderName = folder.folderName \n" + 
					"    inner join post on post.postID = threadInFolder.postID) as A \n" + 
					"    inner join post on post.threadID = A.postID\n" + 
					"    where A.courseID = (?) and A.courseYear = (?) " +
							"and (post.content like (?) or post.postName like (?))");
			statement.setString(1, courseID);
			statement.setInt(2, year);
			statement.setString(3, "%" + content + "%");
			statement.setString(4, "%" + content + "%");
			rs = statement.executeQuery();

			// Legger til alle resultatene i en liste
			resultList = new ArrayList<>();
			while(rs.next()) {
				resultList.add(rs.getInt("post.postID"));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Det oppsto en feil med SQL-spørringen i searchFor", e);
		}
		return resultList;
	}

	/**
	 * Hjelpeklasse som brukes for å lagre statistikk om en bruker
	 * */
	public static class UserStatistics {
		private final String email;
		private final int antallSett;
		private final int antallLaget;
		
		public UserStatistics(String email, int antallSett, int antallLaget) {
			this.email = email;
			this.antallSett = antallSett;
			this.antallLaget = antallLaget;
		}
		
		public String toString() {
			return this.email + " har sett " + this.antallSett + " og har laget " + this.antallLaget + " poster";
		}
	}

	/**
	 * Metode som brukes for å hente ut statistikken fra brukerne
	 *
	 * @param courseID id-en til kurset statistikken skal hentes fra
	 * @param courseYear året til kurset statistikken skal hentes fra
	 * @return en liste med UserStatistics som inneholder all statistikken
	 */
	public List<UserStatistics> getStatistics(String courseID, int courseYear) {
		// Sjekker om brukeren er logget inn
		if(!isLoggedIn()) {
			throw new IllegalStateException("Du må være logget inn for å søke etter innhold.");
		}
		List<UserStatistics> statistics = new ArrayList<>();
		PreparedStatement statement;
		try {
			// Sjekker om brukeren er en instruktør
			statement = this.conn.prepareStatement("select isInstructor " +
					"from user inner join memberOfCourse on memberOfCourse.email = user.email\n" +
					"where memberofcourse.email = (?) and memberOfCourse.courseID = (?) and memberOfCourse.mocYear = (?)");
			statement.setString(1, this.email);
			statement.setString(2, courseID);
			statement.setInt(3, courseYear);
			ResultSet rs = statement.executeQuery();
			if (!rs.next()) {
				throw new IllegalArgumentException("Du er ikke medlem av dette emnet");
			}
			boolean isInstructor = rs.getBoolean("isInstructor");
			if(!isInstructor) {
				throw new IllegalStateException("Du må være en instuktør i emnet for å kunne se statistikk");
			}

			// Spørring som henter statistikk om alle brukerene som er medlem av det gitte emnet.
			statement = this.conn.prepareStatement(
					"select A.email, antallSett, antallLaget " +
							"from (select user.email as email, count(viewedPost.email) as antallSett "
							+ "from user left join viewedPost on viewedPost.email = user.email\n"
							+ "where user.email in (select user.email from user "
							+ "inner join memberOfCourse on memberOfCourse.email = user.email\n"
							+ "where memberOfCourse.courseID = (?) and memberOfCourse.mocYear = (?) )\n"
							+ "group by user.email) as A\n"
							+ "left join (select user.email as email, count(post.userEmail) as antallLaget "
							+ "from user left join post on post.userEmail = user.email\n"
							+ "where post.postID in "
							+ "(select allValidPosts.postID from (select post.postID as threadPostID "
							+ "from post inner join threadInFolder on threadInFolder.postID = post.postID\n"
							+ "inner join folder on folder.courseID = threadInFolder.folderCourseID "
							+ "and folder.folderYear = threadInFolder.folderCourseYear\n"
							+ "and folder.folderName = threadInFolder.folderName\n"
							+ "where folder.courseID = (?) and folder.folderYear = (?)) as threads "
							+ "inner join post as allValidPosts on allValidPosts.threadID = threads.threadPostID)\n"
							+ "group by user.email) as B on A.email = B.email\n"
							+ "order by antallSett desc");
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
}
