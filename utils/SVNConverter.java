/*
 * This class is used to convert a SVN repository
 * to a git repository by calling a shell script
 */

import java.io.InputStream;

public class SVNConverter {
	
	private static SVNConverter instance = null;
	
	protected SVNConverter() 
	{
	   // Exists only to defeat instantiation.
	}
	
	public static SVNConverter getInstance() 
	{
	   if(instance == null)
	      instance = new SVNConverter();
	   return instance;
	}
}
