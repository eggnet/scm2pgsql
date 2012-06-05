package scm2pgsql;

import git.GitParser;

import java.io.IOException;

import org.eclipse.jgit.errors.MissingObjectException;

public class Main {
	public static GitParser gitParser = new GitParser();
	public static void main(String[] args)	
	{
		System.out.println("Scm2Pgsql tool developed by eggnet at UVic.");
		try {
			if (args.length < 3)
			{
				throw new ArrayIndexOutOfBoundsException();
			}
			else
			{
				try 
				{
					if (args.length == 4) GitResources.JAVA_ONLY = Boolean.parseBoolean(args[3]);
					GitResources.startPoint = Float.parseFloat(args[1]);
					GitResources.endPoint = Float.parseFloat(args[2]);
					gitParser.parseRepo(args[0]);
				} 
				catch (MissingObjectException e) 
				{
					e.printStackTrace();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			System.out.println("Usage scm2pgsql <input repository> <start_percentage> <end_percentage> [java_only]");
		}
	}
}
