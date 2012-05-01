package db;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import db.ScriptRunner;

import scm2pgsql.Resources;

public class DbConnection {
	public static Connection conn = null;
	public static DbConnection ref = null;
	public static ScriptRunner sr;
	private DbConnection() 
	{
		try 
		{
			Class.forName("org.postgresql.Driver").newInstance();
		} 
		catch (InstantiationException e) 
		{
			e.printStackTrace();
		} 
		catch (IllegalAccessException e) 
		{
			e.printStackTrace();
		} 
		catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
		}
	}
	
	public static DbConnection getInstance() 
	{
		if (ref == null)
			return (ref = new DbConnection());
		else
			return ref;
	}
	
	/**
	 * Executes a string of SQL on the current databse
	 * @param sql
	 * @return true if successful
	 */
	public boolean exec(String sql)
	{
		//TODO @braden
		return true;
	}
	
	public boolean execScript(String absPath)
	{
		//TODO @braden
		return true;
	}
	
	/**
	 * Connects to the given database.  
	 * @param connectionString
	 * @return true if successful
	 */
	public boolean connect(String connectionString)
	{
		try {
			conn = DriverManager.getConnection(connectionString, Resources.dbUser, Resources.dbPassword);
			sr = new ScriptRunner(conn, false, true);
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
		return true;
	}
	
	/**
	 * Creates a db on the current connection.
	 * @param dbName
	 * @return true for success
	 */
	public boolean createDB(String dbName)
	{
		PreparedStatement s;
		try {
			// Drop the DB if it already exists
			s = conn.prepareStatement("DROP DATABASE IF EXISTS " + dbName + ";");
			s.execute();
			
			// First create the DB.
			s = conn.prepareStatement("CREATE DATABASE " + dbName + ";");
			s.execute();
			
			// Reconnect to our new database.
			connect(Resources.dbUrl + dbName.toLowerCase());
			
			// Now load our default schema in.
			sr.runScript(new FileReader(this.getClass().getResource("scripts/createdb.sql").getPath()));
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public boolean close()
	{
		try {
			conn.close();
			return true;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean InsertCommit(CommitsTO commit)
	{
		// Build the insertion statement
		String insert = "INSERT INTO commits (id, commit_id, author, author_email, comments, commit_date," +
				" changed_files, revision_id) VALUES(" +
				"default, " +
				commit.getCommit_id() + ", " + 
				commit.getAuthor() + ", " + 
				commit.getAuthor_email() + ", " + 
				commit.getComment() + ", " + 
				commit.getCommit_date() + ", " + 
				commit.getChanged_files() + ", " + 
				commit.getRevision_id() + 
				");";
		
		// Run the query
		return exec(insert);
	}
	
	public boolean InsertFiles(FilesTO files)
	{
		// Build the insertion statement
		String insert = "INSERT INTO file (id, file_id, file_name, commit_id, raw_file, revision_id)" +
				" VALUES(" +
				"default, " +
				files.getFile_id() + ", " + 
				files.getFile_name() + ", " + 
				files.getCommit_id() + ", " + 
				files.getRaw_file() + ", " + 
				files.getRevision_id() + 
				");";
		
		return exec(insert);
	}
}
