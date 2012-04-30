package scm2pgsql;

import java.io.IOException;
import org.eclipse.jgit.errors.MissingObjectException;
import git.Git;
import converters.SVNConverter;

public class Main {
	public static Git gitParser = new Git();
	public static void main(String[] args)
	{
		System.out.println("Scm2Pgsql tool developed by eggnet.");
		try {
			System.out.println(args.length);
			if (args.length < 1)
			{
				throw new ArrayIndexOutOfBoundsException();
			}
			else
			{
				if (args[0].equals("--convert"))
				{
					if(args[2].equals("SVN") || args[2].equals("svn"))
					{
						SVNConverter converter = SVNConverter.getInstance();
						if(converter.Convert(args[3]))
							System.out.println("Conversion from SVN to Git was successfull");
						else
						{
							System.out.println("Conversion from SVN to Git has failed");
							throw new IOException();
						}
					}
					else if(args[2].equals("CVS") || args[2].equals("cvs"))
					{
						// TODO @triet add the CVS converter stuff here.
					}
					return;
				}
				else
				{
					try {
						gitParser.parseRepo(".git");
					} catch (MissingObjectException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			System.out.println("Usage scm2pgsql [--convert repositoryType] <input repository>");
		}
		catch (IOException e)
		{
			System.out.println("The process will terminate due to the failed conversion.");
		}
	}
}
