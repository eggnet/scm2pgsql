/*
 * This class is used to convert a SVN repository
 * to a git repository by calling a shell script
 */

package converters;

import java.io.InputStream;
import java.io.*;
import java.io.File;

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
		// Get current working directory
		File directory = new File ("");
		ProcessBuilder pb;
		if(!IsWindows())
			pb = new ProcessBuilder(directory.getAbsolutePath() + "\\utils\\svntogit.sh", URL);
		else
			pb = new ProcessBuilder(directory.getAbsolutePath() + "\\utils\\svntogit.sh", URL);
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
			System.out.println(e.getLocalizedMessage());
			return false;
		}
	}
	
	private boolean IsWindows()
	{
		String os = System.getProperty("os.name").toLowerCase();
		// windows
		return (os.indexOf("win") >= 0);
	}
}
