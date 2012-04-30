package scm2pgsql;

import java.io.IOException;

import org.eclipse.jgit.errors.MissingObjectException;

import git.Git;

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
					// TODO @jordan add the converter code here.
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
	}
}
