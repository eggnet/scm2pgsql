package git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import db.DbConnection;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import scm2pgsql.Resources;

public class GitParser {
	public File repoDir;
	public Repository repoFile;
	public DbConnection db = DbConnection.getInstance();
	public Git git;
	
	private void initialize(String gitDir) throws IOException
	{
		repoDir = new File(gitDir + "/.git");
		repoFile = new RepositoryBuilder() //
			.setGitDir(repoDir)
			.findGitDir()
			.build();
		git = new Git(repoFile);
		String repoName = gitDir.substring(gitDir.lastIndexOf(File.separator)+1);
		db.connect(Resources.dbUrl);
		db.createDB(repoName);
	}
	
	public void parseRepo(String gitDir) throws MissingObjectException, IOException
	{	
		initialize(gitDir);
		// 	for each commit
		//		generate commit record
		//			insert into commits values (default, '12431asdfads', 'Braden Simpson', 'braden@uvic.ca', 'This is a comment', '1999-01-08 04:05:06 -8:00', '{file1.java, file2.java, file3.java}', '122341');
		//		generate file records
		// 		generate branch records
		try
		{
			RevWalk walk = new RevWalk(repoFile);
			RevCommit commit = null;
			Iterable<RevCommit> logs = git.log().call();
			Iterator<RevCommit> i = logs.iterator();
			while (i.hasNext())
			{
				commit = walk.parseCommit(i.next());
				System.out.println(commit.getFullMessage());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		// find the HEAD
		ObjectId lastCommitId = repoFile.resolve(Constants.HEAD);
		// now we have to get the commit
		RevWalk revWalk = new RevWalk(repoFile);
		RevCommit commit = revWalk.parseCommit(lastCommitId);
		// and using commit's tree find the path
		RevTree tree = commit.getTree();
		TreeWalk treeWalk = new TreeWalk(repoFile);
		treeWalk.addTree(tree);
		treeWalk.setFilter(PathFilter.create("README"));
		if (!treeWalk.next()) {
		  return;
		}
		ObjectId objectId = treeWalk.getObjectId(0);
		ObjectLoader loader = repoFile.open(objectId);

		// and then one can use either
		InputStream in = loader.openStream();
		// or
		loader.copyTo(System.out);
		
		db.close();
	}
}
