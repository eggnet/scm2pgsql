/*
 * This class is used to convert a SVN repository
 * to a git repository by calling a shell script
 */

package converters;

import java.io.InputStream;
import java.io.*;

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
	
	public boolean Convert(String URL)
	{
		ProcessBuilder pb = new ProcessBuilder("svntogit.sh", URL);
		try
		{
			// Capture the stderr
			pb.redirectErrorStream(true);
			Process shell = pb.start();
			// Capture the output
			InputStream shellIn = shell.getInputStream();
			// Capture the exit code
			//int shellExitStatus = shell.waitFor();
		
			// Transfer the output of the script to the output of the Java program.
			int c;
			while ((c = shellIn.read()) != -1) {System.out.write(c);}
			
			// Close the output stream
			try {shellIn.close();} catch (IOException ignore) {}
			
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
}
