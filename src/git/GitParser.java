package git;

import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;

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
import org.eclipse.jgit.blame.BlameGenerator;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import scm2pgsql.GitResources;
import db.BranchEntryTO;
import db.CommitsTO;
import db.FileDiffsTO;
import db.FilesTO;
import db.GitDb;
import db.Resources;
import db.FileDiffsTO.diff_types;
import differ.diffObjectResult;
import differ.filediffer;

/**
 * Parses a git repo and adds the information to a PostgreSQL database.
 * <p>
 * The github repo for this project can be found at <a href='http://github.com/eggnet/scm2pgsql'>http://github.com/eggnet/scm2pgsql</a> 
 * <p>
 * This requires a git repo and takes the full path to the repo folder (ex. /home/user/testrepo)
 * as the command line argument.
 * <p>
 * The Database schema that is the resulting output can be found here <a href='https://github.com/eggnet/scm2pgsql/wiki/Database-Schema'>https://github.com/eggnet/scm2pgsql/wiki/Database-Schema</a>
 * @author braden
 *
 */
public class GitParser {
	public File repoDir;
	public Repository repoFile;
	public GitDb db = new GitDb();
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
		db.connect("");
		if (GitResources.startPoint == 0)
			db.createDB(repoName);
		else
			db.connect(repoName);
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
				
				Iterator<RevCommit> it = git.log().all().call().iterator();
				while (it.next() != null)
					GitResources.totalCommits++;
				
				// Turn percentages into numbers
				double endCommitNumber = Math.floor((GitResources.endPoint / 100) * GitResources.totalCommits);
				double startCommitNumber = Math.ceil((GitResources.startPoint/100)*GitResources.totalCommits);
				
				// Set up the walk and initialize variables
				PlotWalk revWalk = new PlotWalk(repoFile);
				revWalk.sort(RevSort.REVERSE);
				AnyObjectId root = repoFile.resolve(Constants.HEAD);
				revWalk.markStart(revWalk.parseCommit(root));
				PlotCommitList<PlotLane> plotCommitList = new PlotCommitList<PlotLane>();
				plotCommitList.source(revWalk);
				plotCommitList.fillTo(Integer.MAX_VALUE);
				Iterator<PlotCommit<PlotLane>> iter = plotCommitList.iterator();

				// Safety
				if (!iter.hasNext())
					return;
				
				PlotCommit<PlotLane> pc = iter.next();
				PlotCommit<PlotLane> pcPrev = null;

				// Set the first commit Id while we are here
				ROOT_COMMIT_ID = pc.getId();
				if (startCommitNumber == 0)
					parseFirstCommit(pc, branch);
				
				for(int currentCount = 1;iter.hasNext() && currentCount <= endCommitNumber;currentCount++)
				{
					pcPrev = pc;
					pc = iter.next();
					System.out.println(new Date().getTime());
					
					if (currentCount >= startCommitNumber)
						parseCommit(pc, pcPrev, branch);
					System.out.println(new Date().getTime());
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		db.close();
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
	public void parseFirstCommit(PlotCommit commit, Ref branch) throws GitAPIException, IOException
	{
		// Need to finish the last commit -- Treat every file in this tree as a changed file.
		CommitsTO currentCommit = new CommitsTO();
		BranchEntryTO currentBranchEntry = new BranchEntryTO();
		FilesTO currentFile = new FilesTO();
		TreeWalk initialCommit = new TreeWalk(repoFile);
		initialCommit.addTree(commit.getTree());
		initialCommit.setRecursive(true);
		
		if (GitResources.JAVA_ONLY)
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

		// insert children
		for (int i = 0;i < commit.getChildCount();i++)
			db.insertCommitFamilyEntry(commit.getChild(i).getName(), commit.getName());
		
		// Add all the raw files in the tree
		Set<String> filenames = new HashSet<String>();
		while(initialCommit.next())
		{
			currentFile = new FilesTO();
			byte[] b = repoFile.open(initialCommit.getObjectId(0).toObjectId(), OBJ_BLOB).getCachedBytes();
			String newText = new String(b, "UTF-8");
			filenames.add(initialCommit.getPathString());
			currentFile.setCommit_id(currentCommit.getCommit_id());				
			currentFile.setFile_id(initialCommit.getPathString());
			currentFile.setRaw_file(newText);
			currentFile.setFile_name(initialCommit.getNameString());
			db.InsertFiles(currentFile);
		}
		System.out.println("Number of changed files: " + filenames.size());
		for (String f: filenames)
		{
			db.InsertChangeEntry(currentCommit.getCommit_id(), f, ChangeType.ADD);
			updateOwnership(currentCommit, currentFile, ChangeType.ADD);
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
	public void parseCommit(PlotCommit currentCommit, PlotCommit prevCommit, Ref branch) throws GitAPIException, IOException
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
		
		System.out.println("Doing commit " + currentCommitTO.getCommit_id() + " at date " + currentCommitTO.getCommit_date()); 
		
		// insert children
		for (int i = 0;i < currentCommit.getChildCount();i++)
			db.insertCommitFamilyEntry(currentCommit.getChild(i).getName(), currentCommit.getName());
		
		// Diff the commits and parse the files.
		ObjectId currentCommitTree = repoFile.resolve(currentCommit.getId().getName() + "^{tree}");
		ObjectId prevCommitTree = repoFile.resolve(prevCommit.getId().getName() + "^{tree}");
		String prevCommitID = prevCommit.getId().getName();
		
		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
		oldTreeIter.reset(reader, prevCommitTree);
		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
		newTreeIter.reset(reader, currentCommitTree);
		List<DiffEntry> diffs;
		diffs = git.diff()
		    .setNewTree(newTreeIter)
		    .setOldTree(oldTreeIter)
		    .call();
		System.out.println("Number of changed files: " + diffs.size());
		
		parseDiffs(currentCommitTO, prevCommitID, diffs);
		db.execBatch();
		
		TreeWalk structure = new TreeWalk(repoFile);
		structure.addTree(currentCommit.getTree());
		structure.setRecursive(true);
		
		if (GitResources.JAVA_ONLY)
			structure.setFilter(PathSuffixFilter.create(".java"));
		
		while(structure.next())
			db.InsertFileTreeEntry(currentCommitTO.getCommit_id(), structure.getPathString());
		
		db.execBatch();
		db.InsertCommit(currentCommitTO);
		db.InsertBranchEntry(currentBranchEntry);
	}
	/** 
	 * Function to parse the diffs from a specific commit.
	 * @author braden
	 * @param currentCommit
	 * @param diffs
	 * @throws MissingObjectException
	 * @throws IOException
	 */
	public void parseDiffs(CommitsTO currentCommit, String prevCommitID, List<DiffEntry> diffs) throws MissingObjectException, IOException  
	{
		for (DiffEntry d : diffs)
		{
			if (GitResources.JAVA_ONLY && !d.getNewPath().endsWith(".java"))
				continue;
			FilesTO currentFile = new FilesTO();
			
			// Not delete, store the raw file
			if (d.getChangeType() != DiffEntry.ChangeType.DELETE) {
				byte[] b = repoFile.open(d.getNewId().toObjectId(), OBJ_BLOB).getCachedBytes();
				String newText = new String(b, "UTF-8");
				currentFile.setRaw_file(newText);
				
				// Get previous rawfile
				if(d.getChangeType() != DiffEntry.ChangeType.ADD)
				{
					b = repoFile.open(d.getOldId().toObjectId(), OBJ_BLOB).getCachedBytes();
					String oldText = new String(b, "UTF-8");
					// Insert diff to db
					parseFileDiffByDiffer(currentCommit.getCommit_id(), prevCommitID, oldText, newText, d.getNewPath());
				}
				else
				{
					// add empty text as the old version
					FileDiffsTO filediff = new FileDiffsTO(d.getNewPath(), currentCommit.getCommit_id(), prevCommitID, newText, 0, newText.length(), diff_types.DIFF_ADD);
					db.InsertFileDiff(filediff);
				}
			}
			currentFile.setCommit_id(currentCommit.getCommit_id());				
			
			// Set file path: deleted file with old commit; new or modified file with new commit 
			if (d.getChangeType() == DiffEntry.ChangeType.DELETE)
				currentFile.setFile_id(d.getOldPath());
			else 
				currentFile.setFile_id(d.getNewPath());
			
			// Set file name
			currentFile.setFile_name(d.getNewPath().substring(
					d.getNewPath().lastIndexOf(File.separatorChar) != -1 ? 
							d.getNewPath().lastIndexOf(File.separatorChar)+1 : 
								0, d.getNewPath().length()));
			
			// Store file and Change entry
			db.InsertFiles(currentFile);
			db.InsertChangeEntry(currentCommit.getCommit_id(), currentFile.getFile_id(), d.getChangeType());
			updateOwnership(currentCommit, currentFile, d.getChangeType());
		}
	}
	
	/**
	 * Diff two version of the file and store the diff into file_diffs table
	 */
	public void parseFileDiffByDiffer(String currentCommit, String prevCommit, String oldRawFile, String newRawFile, String fileID) throws MissingObjectException, IOException 
	{
		// Get Differ to diff two raw files
		filediffer differ = new filediffer(oldRawFile, newRawFile);
		differ.diffFilesLineMode();
		
		// Insert objects
		for(diffObjectResult insert : differ.getInsertObjects())
		{
			FileDiffsTO.diff_types type = diff_types.DIFF_MODIFYINSERT;
			FileDiffsTO filediff = new FileDiffsTO(fileID, currentCommit, prevCommit, insert.diffObject.text, insert.start, insert.end, type);
			db.InsertFileDiff(filediff);		
		}
		
		// Delete objects
		for(diffObjectResult delete : differ.getDeleteObjects())
		{
			FileDiffsTO.diff_types type = diff_types.DIFF_MODIFYDELETE;
			FileDiffsTO filediff = new FileDiffsTO(fileID, currentCommit, prevCommit, delete.diffObject.text, delete.start, delete.end, type);
			db.InsertFileDiff(filediff);		
		}
	}

	/**
	 * Updates the ownership of a file with a range and inserts into the database.
	 * @param currentCommit Commit that is being updated
	 * @param currentFile
	 * @throws IOException 
	 */
	public void updateOwnership(CommitsTO currentCommit, FilesTO currentFile, ChangeType change) throws IOException
	{
		// init
		BlameResultRecord rec = null;
		
		// DELETE is special case
		if (change == ChangeType.DELETE)
		{
			rec = new BlameResultRecord();
			rec.setAuthorId(currentCommit.getAuthor_email());
			rec.setCommitId(currentCommit.getCommit_id());
			rec.setSourceCommitId(currentCommit.getCommit_id());
			rec.setFileId(currentFile.getFile_id());
			rec.setLineEnd(-1);
			rec.setLineStart(-1);
			rec.setType(GitResources.ChangeType.valueOf(change.toString()));
			db.insertOwnerRecord(rec);
			return;
		}
		
		// Construct the blame command
		BlameGenerator bg = new BlameGenerator(repoFile, currentFile.getFile_id());
		bg.push(null, repoFile.resolve(currentCommit.getCommit_id()));
		bg.setFollowFileRenames(true);
		BlameResult blameRes = bg.computeBlameResult();
		for(int i = 0;i < blameRes.getResultContents().size();i++)
		{
			PersonIdent sourceAuthor = blameRes.getSourceAuthor(i);
			if (sourceAuthor == null)
				continue;	// safety
			else if (rec == null)
			{
				rec = new BlameResultRecord();
				rec.setAuthorId(sourceAuthor.getEmailAddress());
				rec.setSourceCommitId(blameRes.getSourceCommit(i).getName());
				rec.setFileId(currentFile.getFile_id());
				rec.setCommitId(currentCommit.getCommit_id());
				rec.setLineStart(i+1);
			}
			else if (!rec.getAuthorId().equals(sourceAuthor.getEmailAddress()))
			{
				// finish off the last record
				rec.setLineEnd(i);
				rec.setType(GitResources.ChangeType.valueOf(change.toString()));
				
				// Convert from line -> char
				rec.setLineStart(Resources.convertLineStartToCharStart(rec.getLineStart(), currentFile.getRaw_file()));
				rec.setLineEnd(Resources.convertLineEndToCharEnd(rec.getLineEnd(), currentFile.getRaw_file()));
				db.insertOwnerRecord(rec);
				
				// we have a new owner
				rec = new BlameResultRecord();
				rec.setAuthorId(sourceAuthor.getEmailAddress());
				rec.setSourceCommitId(blameRes.getSourceCommit(i).getName());
				rec.setFileId(currentFile.getFile_id());
				rec.setCommitId(currentCommit.getCommit_id());
				rec.setLineStart(i+1);
			}
			else if (rec.getAuthorId().equals(sourceAuthor.getEmailAddress()))
				rec.setLineEnd(i+1); 				// we have the same owner
		}
		if (rec != null)
		{
			rec.setType(GitResources.ChangeType.valueOf(change.toString()));
			
			// Convert from line -> char
			rec.setLineStart(Resources.convertLineStartToCharStart(rec.getLineStart(), currentFile.getRaw_file()));
			rec.setLineEnd(Resources.convertLineEndToCharEnd(rec.getLineEnd(), currentFile.getRaw_file()));
			db.insertOwnerRecord(rec);
		}
		db.execCallableBatch();
	}
}
