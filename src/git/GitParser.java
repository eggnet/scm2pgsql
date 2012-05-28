package git;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import scm2pgsql.Resources;
import db.BranchEntryTO;
import db.CommitsTO;
import db.DbConnection;
import db.FilesTO;

public class GitParser {
	public File repoDir;
	public Repository repoFile;
	public DbConnection db = DbConnection.getInstance();
	public Git git;
	public PrintStream logger;
	
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
		// open up our error log.
		File log = new File("err.log");
		log.createNewFile();
		logger = new PrintStream(log);
	}
	
	public void parseRepo2(String gitDir) throws MissingObjectException, IOException
	{
		initialize(gitDir);

		// Setup branches 
		List<Ref> branches = git.branchList().call();
		for (Ref branch : branches)
		{		
			try
			{
				git.checkout().setName(branch.getName()).call();
				System.out.println(repoFile.getFullBranch());
				RevWalk walk = new RevWalk(repoFile);
				RevCommit current, previous = null;
				Iterable<RevCommit> logs = git.log().call();
				Iterator<RevCommit> i = logs.iterator();
				
				// Safety
				if (!i.hasNext())
					return;
				
				current = walk.parseCommit(i.next());
				while(i.hasNext())
				{
					previous = current;
					current = walk.parseCommit(i.next());
					parseCommit(current, previous, branch);
				}
				parseLastCommit(previous, branch);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			db.close();
		}
	}
	
	public void parseLastCommit(RevCommit commit, Ref branch) throws GitAPIException, IOException
	{
		// Need to finish the last commit -- Treat every file in this tree as a changed file.
		CommitsTO currentCommit = new CommitsTO();
		BranchEntryTO currentBranchEntry = new BranchEntryTO();
		FilesTO currentFile = new FilesTO();
		TreeWalk initialCommit = new TreeWalk(repoFile);
		initialCommit.addTree(commit.getTree());
		initialCommit.setRecursive(true);
		
		if (Resources.JAVA_ONLY)
			initialCommit.setFilter(PathSuffixFilter.create(".java"));

		currentCommit.setAuthor(commit.getAuthorIdent().getName());
		currentCommit.setAuthor_email(commit.getAuthorIdent().getEmailAddress());
		currentCommit.setCommit_id(commit.getId().getName());
		currentCommit.setComment(commit.getFullMessage());
		currentCommit.setCommit_date(new Date(commit.getCommitTime() * 1000L));
		currentBranchEntry.setBranch_id(branch.getObjectId().getName());
		currentBranchEntry.setBranch_name(branch.getName());
		currentBranchEntry.setCommit_id(currentCommit.getCommit_id());
		
		Set<String> filenames = new HashSet<String>();
		while(initialCommit.next())
		{
			currentFile = new FilesTO();
			ObjectLoader objectL = repoFile.open(initialCommit.getObjectId(0));
			objectL.openStream();
			ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			objectL.copyTo(out);
			String raw = out.toString("UTF-8");
			filenames.add(initialCommit.getPathString());
			currentFile.setCommit_id(currentCommit.getCommit_id());				
			currentFile.setFile_id(initialCommit.getPathString());
			currentFile.setRaw_file(raw);
			currentFile.setFile_name(initialCommit.getNameString());
			db.InsertFiles(currentFile);
		}
		System.out.println("Number of changed files: " + filenames.size());
		for (String f: filenames)
		{
			db.InsertChangeEntry(currentCommit.getCommit_id(), f, ChangeType.ADD);
			db.InsertFileTreeEntry(currentCommit.getCommit_id(), f);
		}
		db.execBatch();
		db.InsertCommit(currentCommit);
		db.InsertBranchEntry(currentBranchEntry);
	}
	
	public void parseCommit(RevCommit commit, RevCommit previousCommit, Ref branch) throws GitAPIException, IOException
	{
		CommitsTO currentCommitTO = new CommitsTO();
		BranchEntryTO currentBranchEntry = new BranchEntryTO();
		ObjectReader reader = repoFile.newObjectReader();
		currentCommitTO.setAuthor(commit.getAuthorIdent().getName());
		currentCommitTO.setAuthor_email(commit.getAuthorIdent().getEmailAddress());
		currentCommitTO.setCommit_id(commit.getId().getName());
		currentCommitTO.setComment(commit.getFullMessage());
		currentCommitTO.setCommit_date(new Date(commit.getCommitTime() * 1000L));
		currentCommitTO.setBranch_id(branch.getObjectId().getName());
		currentBranchEntry.setBranch_id(branch.getObjectId().getName());
		currentBranchEntry.setBranch_name(branch.getName());
		currentBranchEntry.setCommit_id(currentCommitTO.getCommit_id());

		ObjectId currentCommitTree = repoFile.resolve(commit.getId().getName() + "^{tree}");
		ObjectId prevCommitTree = repoFile.resolve(previousCommit.getId().getName() + "^{tree}");
		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
		oldTreeIter.reset(reader, prevCommitTree);
		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
		newTreeIter.reset(reader, currentCommitTree);
		List<DiffEntry> diffs;
		diffs = git.diff()
			.setOutputStream(logger)
		    .setNewTree(newTreeIter)
		    .setOldTree(oldTreeIter)
		    .call();
		System.out.println("Number of changed files: " + diffs.size());
		
		parseDiffs(currentCommitTO, diffs);
		db.execBatch();
		
		TreeWalk structure = new TreeWalk(repoFile);
		structure.addTree(commit.getTree());
		structure.setRecursive(true);
		if (Resources.JAVA_ONLY)
			structure.setFilter(PathSuffixFilter.create(".java"));
		
		while(structure.next())
			db.InsertFileTreeEntry(currentCommitTO.getCommit_id(), structure.getPathString());
		
		db.execBatch();
		db.InsertCommit(currentCommitTO);
		db.InsertBranchEntry(currentBranchEntry);
	}
	
	public void parseDiffs(CommitsTO currentCommit, List<DiffEntry> diffs) throws MissingObjectException, IOException 
	{
		FilesTO currentFile;
		for (DiffEntry d : diffs)
		{
			if (Resources.JAVA_ONLY && !d.getNewPath().endsWith(".java"))
				continue;
			currentFile = new FilesTO();
			if (d.getChangeType() != DiffEntry.ChangeType.DELETE) {
				ObjectLoader objectL = repoFile.open(d.getNewId().toObjectId());
				objectL.openStream();
				ByteArrayOutputStream out = new ByteArrayOutputStream(); 
				objectL.copyTo(out);
				String raw = out.toString("UTF-8");
				currentFile.setRaw_file(raw);
				out.flush();
			}
			currentFile.setCommit_id(currentCommit.getCommit_id());				
			if (d.getChangeType() == DiffEntry.ChangeType.DELETE)
				currentFile.setFile_id(d.getOldPath());
			else 
				currentFile.setFile_id(d.getNewPath());
			currentFile.setFile_name(d.getNewPath().substring(
					d.getNewPath().lastIndexOf(File.separatorChar) != -1 ? 
							d.getNewPath().lastIndexOf(File.separatorChar)+1 : 
								0, d.getNewPath().length()));
			db.InsertFiles(currentFile);
			db.InsertChangeEntry(currentCommit.getCommit_id(), currentFile.getFile_id(), d.getChangeType());
		}
	}
}


