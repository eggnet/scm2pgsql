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
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffAlgorithm.SupportedAlgorithm;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import scm2pgsql.Resources;
import db.BranchEntryTO;
import db.CommitsTO;
import db.DbConnection;
import db.FilesTO;
import db.FileDiffsTO;
import differ.diff_match_patch.Diff;
import differ.diff_match_patch;
import differ.filediffer;

public class GitParser {
	public File repoDir;
	public Repository repoFile;
	public DbConnection db = DbConnection.getInstance();
	public Git git;
	public PrintStream logger;
	public static ObjectId ROOT_COMMIT_ID;
	
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
		File log = new File("err.log");
		log.createNewFile();
		logger = new PrintStream(log);
	}
	
	public void parseRepo(String gitDir) throws MissingObjectException, IOException
	{
		initialize(gitDir);
		// Setup branches 
		List<Ref> branches = git.branchList().call();
		for (Ref branch : branches)
		{		
			try
			{
				// Checkout the branch and setup variables.
				git.checkout().setName(branch.getName()).call();
				System.out.println(repoFile.getFullBranch());
				RevWalk walk = new RevWalk(repoFile);
				AnyObjectId root = repoFile.resolve(Constants.HEAD);
				walk.sort(RevSort.REVERSE);
				walk.markStart(walk.parseCommit(root));
				RevCommit current, previous = null;
				Iterator<RevCommit> i = walk.iterator();

				// Safety
				if (!i.hasNext())
					return;
				
				// Walk our commits.
				current = walk.parseCommit(i.next());
				
				// Set the first commit Id while we are here
				ROOT_COMMIT_ID = current.getId();
				
				parseFirstCommit(current, branch);
				while(i.hasNext())
				{
					previous = current;
					current = walk.parseCommit(i.next());
					parseCommit(current, previous, branch);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			db.close();
		}
	}
	
	/**
	 * Parses the final commit in the walk.  This is different because
	 * there is no longer a notion of 'diffing', just adding all files 
	 * as changed.
	 * @param commit
	 * @param branch
	 * @throws GitAPIException
	 * @throws IOException
	 */
	public void parseFirstCommit(RevCommit commit, Ref branch) throws GitAPIException, IOException
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

		// Setup for transfer objects
		currentCommit.setAuthor(commit.getAuthorIdent().getName());
		currentCommit.setAuthor_email(commit.getAuthorIdent().getEmailAddress());
		currentCommit.setCommit_id(commit.getId().getName());
		currentCommit.setComment(commit.getFullMessage());
		currentCommit.setCommit_date(new Date(commit.getCommitTime() * 1000L));
		currentBranchEntry.setBranch_id(branch.getObjectId().getName());
		currentBranchEntry.setBranch_name(branch.getName());
		currentBranchEntry.setCommit_id(currentCommit.getCommit_id());
		
		// Add all the raw files in the tree
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
	
	/**
	 * Parses a given commit into the database.  First diffs the commit with the previous, and inserts records, 
	 * then it adds ownership to the files using git-blame.
	 * @param currentCommit
	 * @param nextCommit
	 * @param branch
	 * @throws GitAPIException
	 * @throws IOException
	 */
	public void parseCommit(RevCommit currentCommit, RevCommit prevCommit, Ref branch) throws GitAPIException, IOException
	{
		// initialize transfer objects
		CommitsTO currentCommitTO = new CommitsTO();
		BranchEntryTO currentBranchEntry = new BranchEntryTO();
		ObjectReader reader = repoFile.newObjectReader();

		// setup values for transfer objects
		currentCommitTO.setAuthor(currentCommit.getAuthorIdent().getName());
		currentCommitTO.setAuthor_email(currentCommit.getAuthorIdent().getEmailAddress());
		currentCommitTO.setCommit_id(currentCommit.getId().getName());
		currentCommitTO.setComment(currentCommit.getFullMessage());
		currentCommitTO.setCommit_date(new Date(currentCommit.getCommitTime() * 1000L));
		currentCommitTO.setBranch_id(branch.getObjectId().getName());
		currentBranchEntry.setBranch_id(branch.getObjectId().getName());
		currentBranchEntry.setBranch_name(branch.getName());
		currentBranchEntry.setCommit_id(currentCommitTO.getCommit_id());

		// Diff the commits and parse the files.
		ObjectId currentCommitTree = repoFile.resolve(currentCommit.getId().getName() + "^{tree}");
		ObjectId prevCommitTree = repoFile.resolve(prevCommit.getId().getName() + "^{tree}");
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
		structure.addTree(currentCommit.getTree());
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
			updateOwnership(currentCommit, currentFile);
		}
	}
	
	/*
	 * Diff two version of the file and store the diff into file_diffs table
	 */
	public void parseFileDiffByDiffer(String currentCommit, String prevCommit, String oldRawFile, String newRawFile, FilesTO file) throws MissingObjectException, IOException 
	{
		// Get Differ to diff two raw files
		filediffer differ = new filediffer(oldRawFile, newRawFile);
		differ.diffFilesLineMode();
		
		// Get list of diff objects
		for (Diff d : differ.getDiffObjects())
		{
			if(d.operation == diff_match_patch.Diff.operation.)
			FileDiffsTO filediff = new FileDiffsTO(file.getFile_id(), currentCommit, prevCommit, d.text, 0, 0, d.operation.toString());
			db.InsertFileDiff(filediff);
		}
	}

	/**
	 * Updates the ownership of a file with a range and inserts into the database.
	 * @param currentCommit
	 * @param currentFile
	 */
	public void updateOwnership(CommitsTO currentCommit, FilesTO currentFile)
	{
//		BlameResult blameRes = git.blame().setDiffAlgorithm(DiffAlgorithm.getAlgorithm(SupportedAlgorithm.MYERS)).setFilePath(currentFile.getFile_id()).setStartCommit(ROOT_COMMIT_ID).call();
//		System.out.println(blameRes.getResultContents().toString());
	}
}


