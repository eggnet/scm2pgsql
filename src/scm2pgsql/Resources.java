package scm2pgsql;

import java.text.SimpleDateFormat;

public class Resources {
	public static final String dbUser = "postgres";
	public static final String dbPassword = "scr4mbled";
	public static final String dbUrl = "jdbc:postgresql://142.104.21.212/";
	public static final SimpleDateFormat DBDateFormat = new SimpleDateFormat("yyyy-mm-dd kk:mm:ss Z");
	public static boolean JAVA_ONLY = true;
	public enum ChangeType {
		MODIFYINSERT, MODIFYDELETE, DELETE, ADD, MODIFY, MOVE, RENAME
	}
}
