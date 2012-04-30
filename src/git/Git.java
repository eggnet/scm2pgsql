package git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import db.DbConnection;

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

public class Git {
	public File repoDir;
	public Repository repoFile;
	public void parseRepo(String inDir) throws MissingObjectException, IOException
	{
		
		DbConnection db = DbConnection.getInstance();
		db.connect("jdbc:postgresql://142.104.21.212:5432/test");
		db.createDB("testing123");
		db.close();
		repoDir = new File(inDir);
		repoFile = new RepositoryBuilder() //
	        .setGitDir(repoDir) // --git-dir if supplied, no-op if null
	        .findGitDir() // scan up the file system tree
	        .build();
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
