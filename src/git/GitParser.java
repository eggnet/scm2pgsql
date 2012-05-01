package git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
	private void initDb(String dbName)
	{
		db.connect(Resources.dbUrl);
		db.createDB(dbName);
	}
	
	public void parseRepo(String gitDir) throws MissingObjectException, IOException
	{	
		// Initialize the repo directory
		repoDir = new File(gitDir);
		repoFile = new RepositoryBuilder() //
	        .setGitDir(repoDir) // --git-dir if supplied, no-op if null
	        .findGitDir() // scan up the file system tree
	        .build();
		git = new Git(repoFile);
		System.out.println(repoFile.getDirectory().getCanonicalPath());
		String repoName =repoFile.getDirectory().getAbsolutePath().substring(repoFile.getDirectory().getAbsolutePath().lastIndexOf(File.separator)+1);
		initDb(repoName);
		
		
		
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
	}
}
