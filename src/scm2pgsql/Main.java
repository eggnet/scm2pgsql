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
			if (args.length < 1)
			{
				throw new ArrayIndexOutOfBoundsException();
			}
			else
			{
				try 
				{
					if (args.length == 2) Resources.JAVA_ONLY = Boolean.parseBoolean(args[1]);
					gitParser.parseRepo2(args[0]);
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
			System.out.println("Usage scm2pgsql <input repository>");
		}
	}
}
