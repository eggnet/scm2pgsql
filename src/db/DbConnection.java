package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import scm2pgsql.Resources;

public class DbConnection {
	public static Connection conn = null;
	public static DbConnection ref = null;
	public static String DriverString = "org.postgresql.jdbc4";
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
			s = conn.prepareStatement("CREATE DATABASE " + dbName + ";"); // TODO SQL escaping?
			s.execute();
			return true;
		} catch (SQLException e) {
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
}
