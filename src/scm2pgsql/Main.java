package scm2pgsql;

import git.Git;

public class Main {
	public static void main(String[] args)
	{
		System.out.println("Scm2Pgsql tool developed by eggnet.");
		try {
			if (args.length < 2)
			{
				throw new ArrayIndexOutOfBoundsException();
			}
			else
			{
				if (args[1].equals("--convert"))
				{
					// TODO @jordan add the converter code here.
				}
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			System.out.println("Usage scm2pgsql [--convert repositoryType] <input repository>");
		}
	}
}
