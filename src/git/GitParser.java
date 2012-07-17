package git;

import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameGenerator;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
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
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.NullOutputStream;

import db.BranchEntryTO;
import db.CommitsTO;
import db.FileDiffsTO;
import db.FilesTO;
import db.GitDb;
import db.TechnicalResources;
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
	public NullOutputStream logger;
	public Map<String, PlotCommit> plotCommitMap;
	public static ObjectId ROOT_COMMIT_ID;
	public static int CACHE_RAWFILE_THRESHOLD;

	private void initialize(String gitDir) throws IOException
	{
		repoDir = new File(gitDir + "/.git");
		repoFile = new RepositoryBuilder()
			.setGitDir(repoDir)
			.findGitDir()
			.build();
		git = new Git(repoFile);
		String repoName = gitDir.substring(gitDir.lastIndexOf(File.separator)+1);
		db.connect(Resources.EGGNET_DB_NAME);
		
		if (repoName.endsWith("/"))
		{
			// Chop off the trailing slash
			repoName = repoName.substring(0, repoName.length()-1);
		}
		
		db.createDB(repoName);
		File log = new File("err.log");
		log.createNewFile();
		logger = NullOutputStream.INSTANCE;	
		this.CACHE_RAWFILE_THRESHOLD = 100;
	}
	
	public void parseRepo(String gitDir) throws MissingObjectException, IOException
	{
		initialize(gitDir);
		int commitCounter = 0;

		// Setup branches 
		List<Ref> branches = git.branchList().call();
		for (Ref branch : branches)
		{		
			try
			{
				// Checkout the branch and setup variables.
				git.checkout().setName(branch.getName()).call();
				System.out.println(repoFile.getFullBranch());
				
				// Set up the walk and initialize variables
				PlotWalk revWalk = new PlotWalk(repoFile);
				revWalk.sort(RevSort.REVERSE);
				AnyObjectId root = repoFile.resolve(Constants.HEAD);
				revWalk.markStart(revWalk.parseCommit(root));
				PlotCommitList<PlotLane> plotCommitList = new PlotCommitList<PlotLane>();
				plotCommitList.source(revWalk);
				plotCommitList.fillTo(Integer.MAX_VALUE);
				
				// Convert the list into hashmap
				plotCommitMap =  new HashMap<String, PlotCommit>();
				for(PlotCommit plotCom : plotCommitList)
					plotCommitMap.put(plotCom.getId().getName(), plotCom);
				
				Iterator<PlotCommit<PlotLane>> iter = plotCommitList.iterator();
				// Safety
				if (!iter.hasNext())
					return;
				
				PlotCommit<PlotLane> pc = iter.next();
				// Set the first commit Id while we are here
				ROOT_COMMIT_ID = pc.getId();
				
				parseFirstCommit(pc, branch);
				while(iter.hasNext())
				{
					pc = iter.next();
					parseCommit(pc, branch);
					
					//Every 100 commits, cached all raw files
					if(commitCounter % CACHE_RAWFILE_THRESHOLD == 0)
						cacheRawFiles(pc, branch);
					commitCounter++;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Exception in commit: " + commitCounter);
				return;
			}
		}
		db.close();
	}
	
	public void cacheRawFiles(PlotCommit commit, Ref branch)
	{
		try 
		{
			// Get all files in this commit
			TreeWalk initialCommit = new TreeWalk(repoFile);
			initialCommit.addTree(commit.getTree());
			initialCommit.setRecursive(true);
			if (TechnicalResources.JAVA_ONLY)
				initialCommit.setFilter(PathSuffixFilter.create(".java"));
	
			// For each file, insert raw file to file_caches.
			while(initialCommit.next())
			{
				FilesTO currentFile = new FilesTO();
				byte[] b = repoFile.open(initialCommit.getObjectId(0).toObjectId(), OBJ_BLOB).getCachedBytes();
				String newText = new String(b, "UTF-8");
				currentFile.setCommit_id(commit.getId().getName());				
				currentFile.setFile_id	(initialCommit.getPathString());
				currentFile.setRaw_file	(newText);
				currentFile.setFile_name(initialCommit.getNameString());
				
				// insert to file_caches
				db.InsertFiles(currentFile);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Exception in saving RawFiles for commit: " + commit.getId().getName());
			return;
		}
	}
	
	/**
	 * parseFirstCommit
	 * Parses the first commit in the walk. This is different because
	 * there is no longer a notion of 'diffing', just adding all files 
	 * as changed.
	 * 1. For each file
	 * 		Insert file_diffs entry as DIFF_ADD
	 * 		Update onwership
	 * 2. ParseCommit as normal to go through all the children
	 * @param commit
	 * @param branch
	 * @throws GitAPIException
	 * @throws IOException
	 */
	public void parseFirstCommit(PlotCommit commit, Ref branch) throws GitAPIException, IOException
	{
		// Get all files in first commit
		TreeWalk initialCommit = new TreeWalk(repoFile);
		initialCommit.addTree(commit.getTree());
		initialCommit.setRecursive(true);
		
		if (TechnicalResources.JAVA_ONLY)
			initialCommit.setFilter(PathSuffixFilter.create(".java"));

		CommitsTO currentCommit = new CommitsTO();
		currentCommit.setAuthor		 (commit.getAuthorIdent().getName());
		currentCommit.setAuthor_email(commit.getAuthorIdent().getEmailAddress());
		currentCommit.setCommit_id	 (commit.getId().getName());
		currentCommit.setComment	 (commit.getFullMessage());
		currentCommit.setCommit_date (new Date(commit.getCommitTime() * 1000L));
		
		// For each file, insert DIFF_ADD to file_diffs and update ownership.
		while(initialCommit.next())
		{
			FilesTO currentFile = new FilesTO();
			byte[] b = repoFile.open(initialCommit.getObjectId(0).toObjectId(), OBJ_BLOB).getCachedBytes();
			String newText = new String(b, "UTF-8");
			currentFile.setCommit_id(commit.getId().getName());				
			currentFile.setFile_id	(initialCommit.getPathString());
			currentFile.setRaw_file	(newText);
			currentFile.setFile_name(initialCommit.getNameString());
			
			// insert to file_diffs table as DIFF_ADD
			FileDiffsTO filediff = new FileDiffsTO(initialCommit.getPathString(), commit.getId().getName(), "", newText, 0, newText.length(), diff_types.DIFF_ADD);
			db.InsertFileDiff(filediff);
			
			// update Ownership
			updateOwnership(currentCommit, currentFile, ChangeType.ADD);
		}
		
		// Insert relationships and files for its children
		parseCommit(commit, branch);
	}
	
	/**
	 * Parses a given commit:
	 * 		1. Insert it to Commits table
	 * 		2. Insert it to Branches - (branch,commit)
	 * 		3. For each children commit
	 * 			 1. Insert it to Commit_family - (Parent, Child)
	 * 			 2. For each File change
	 * 			 	   1.Insert the changes to file_diffs (between parent and child)
	 * 				   2.Update ownership for the file
	 * @param currentCommit
	 * @param branch
	 * @throws GitAPIException
	 * @throws IOException
	 */
	public void parseCommit(PlotCommit<PlotLane> currentCommit, Ref branch) throws GitAPIException, IOException
	{
		// initialize transfer objects
		CommitsTO currentCommitTO = new CommitsTO();
		ObjectReader reader = repoFile.newObjectReader();

		// setup values for transfer objects
		currentCommitTO.setAuthor		(currentCommit.getAuthorIdent().getName());
		currentCommitTO.setAuthor_email (currentCommit.getAuthorIdent().getEmailAddress());
		currentCommitTO.setCommit_id	(currentCommit.getId().getName());
		currentCommitTO.setComment		(currentCommit.getFullMessage());
		currentCommitTO.setCommit_date	(new Date(currentCommit.getCommitTime() * 1000L));
		currentCommitTO.setBranch_id	(branch.getObjectId().getName());
		db.InsertCommit(currentCommitTO);
		
		// BranchEntry
		BranchEntryTO currentBranchEntry = new BranchEntryTO();
		currentBranchEntry.setBranch_id	 (branch.getObjectId().getName());
		currentBranchEntry.setBranch_name(branch.getName());
		currentBranchEntry.setCommit_id	 (currentCommitTO.getCommit_id());
		db.InsertBranchEntry(currentBranchEntry);
		
		System.out.println("Doing commit " + currentCommitTO.getCommit_id() + " at date " + currentCommitTO.getCommit_date()); 
		
		// insert children
		for (int i = 0;i < currentCommit.getChildCount();i++)
		{
			// Set commit tree for Parent
			String parentId = currentCommit.getName();
			ObjectId prevCommitTree = repoFile.resolve(parentId + "^{tree}");
			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, prevCommitTree);
			
			// Get Plot commit for children
			String childId  = currentCommit.getChild(i).getName();
			PlotCommit<PlotLane> childCommit = plotCommitMap.get(childId);
			if(childCommit == null)
				continue;
			
			// commit_family
			db.insertCommitFamilyEntry(childId, parentId);
			
			// Get commit tree for child, compare the two trees to get list of changed files
			ObjectId currentCommitTree = repoFile.resolve(childId  + "^{tree}");
			CanonicalTreeParser newTreeIter= new CanonicalTreeParser();
			newTreeIter.reset(reader, currentCommitTree);
			List<DiffEntry> diffs = git.diff().setOutputStream(logger).setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
			
			System.out.println("Number of changed files: " + diffs.size());
			
			// setup childCommit for parse diffs
			CommitsTO childCommitTO = new CommitsTO();
			childCommitTO.setAuthor		 (childCommit.getAuthorIdent().getName());
			childCommitTO.setAuthor_email(childCommit.getAuthorIdent().getEmailAddress());
			childCommitTO.setCommit_id	 (childCommit.getId().getName());
			childCommitTO.setComment	 (childCommit.getFullMessage());
			childCommitTO.setCommit_date (new Date(childCommit.getCommitTime() * 1000L));
			childCommitTO.setBranch_id	 (branch.getObjectId().getName());
			parseDiffs(childCommitTO, currentCommitTO, diffs);
		}
		
	}

	/**
	 * Function to parse the diffs from a specific commit.
	 * For each file
	 * 1. Compare the file between two commits and put them into file_diffs
	 * 2. Update ownership
	 * 
	 * @author braden, Infiro
	 * @param currentCommit
	 * @param prevCommit
	 * @param diffs - list of FILES that are different between two commits (not the diff object)
	 * @throws MissingObjectException
	 * @throws IOException
	 */
	public void parseDiffs(CommitsTO currentCommit, CommitsTO prevCommit, List<DiffEntry> diffs) throws MissingObjectException, IOException  
	{
		// For each file
		for (DiffEntry d : diffs)
		{
			String newPathName = d.getNewPath();
			String oldPathName = d.getOldPath();
			
			// skip non-java file
			if (TechnicalResources.JAVA_ONLY)
			{ 
				if(!(newPathName.endsWith(".java") || oldPathName.endsWith(".java")))
					continue;
			}

			// Insert to file_diffs based on change type
			FilesTO currentFile = new FilesTO();
			if (d.getChangeType() == DiffEntry.ChangeType.COPY)
			{
				byte[] b = repoFile.open(d.getNewId().toObjectId(), OBJ_BLOB).getCachedBytes();
				String newText = new String(b, "UTF-8");
				
				FileDiffsTO filediff = new FileDiffsTO(d.getNewPath(), currentCommit.getCommit_id(), prevCommit.getCommit_id(), newText, 0, newText.length(), diff_types.DIFF_COPY);
				db.InsertFileDiff(filediff);
				currentFile.setRaw_file(newText);
				currentFile.setFile_id(d.getNewPath());
			}
			else
			if (d.getChangeType() == DiffEntry.ChangeType.RENAME)
			{
				byte[] b = repoFile.open(d.getNewId().toObjectId(), OBJ_BLOB).getCachedBytes();
				String newText = new String(b, "UTF-8");
				
				FileDiffsTO filediff = new FileDiffsTO(d.getNewPath(), currentCommit.getCommit_id(), prevCommit.getCommit_id(), newText, 0, newText.length(), diff_types.DIFF_RENAME);
				db.InsertFileDiff(filediff);
				currentFile.setRaw_file(newText);
				currentFile.setFile_id(d.getNewPath());
			}
			else
			if (d.getChangeType() == DiffEntry.ChangeType.DELETE)
			{
				FileDiffsTO filediff = new FileDiffsTO(d.getOldPath(), currentCommit.getCommit_id(), prevCommit.getCommit_id(), "", 0, 0, diff_types.DIFF_DELETE);
				db.InsertFileDiff(filediff);
				currentFile.setRaw_file("");
				currentFile.setFile_id(d.getOldPath());
			}
			else
			if (d.getChangeType() == DiffEntry.ChangeType.ADD)
			{
				byte[] b = repoFile.open(d.getNewId().toObjectId(), OBJ_BLOB).getCachedBytes();
				String newText = new String(b, "UTF-8");
				
				FileDiffsTO filediff = new FileDiffsTO(d.getNewPath(), currentCommit.getCommit_id(), prevCommit.getCommit_id(), newText, 0, newText.length(), diff_types.DIFF_ADD);
				db.InsertFileDiff(filediff);
				currentFile.setRaw_file(newText);
				currentFile.setFile_id(d.getNewPath());
			}
			else // File changed, compare two files
			if (d.getChangeType() == DiffEntry.ChangeType.MODIFY)
			{
				byte[] b = repoFile.open(d.getNewId().toObjectId(), OBJ_BLOB).getCachedBytes();
				String newText = new String(b, "UTF-8");
				
				b = repoFile.open(d.getOldId().toObjectId(), OBJ_BLOB).getCachedBytes();
				String oldText = new String(b, "UTF-8");
				
				parseFileDiffByDiffer(currentCommit.getCommit_id(), prevCommit.getCommit_id(), oldText, newText, d.getNewPath());
				
				currentFile.setRaw_file(newText);
				currentFile.setFile_id(d.getNewPath());
			}
			
			// Update ownership
			currentFile.setCommit_id(currentCommit.getCommit_id());
			String newFilePath = d.getNewPath();
			currentFile.setFile_name(d.getNewPath().substring(newFilePath.lastIndexOf(File.separatorChar) != -1 ? newFilePath.lastIndexOf(File.separatorChar)+1 :	0,
															  newFilePath.length()));
			
			updateOwnership(currentCommit, currentFile, d.getChangeType());
		}
	}
	
	/**
	 * @author Infiro
	 * Diff file from two commits
	 * Store the diffs into file_diffs
	 * Note: the end index is actually off by 1 char. For example, if insert.end is 10, the really end index is 9.
	 * This is on purpose, do not change it
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
			rec.setAuthorId		 (currentCommit.getAuthor_email());
			rec.setCommitId		 (currentCommit.getCommit_id());
			rec.setSourceCommitId(currentCommit.getCommit_id());
			rec.setFileId		 (currentFile.getFile_id());
			rec.setLineEnd  (-1);
			rec.setLineStart(-1);
			rec.setType(TechnicalResources.ChangeType.valueOf(change.toString()));
			db.insertOwnerRecord(rec);
			return;
		}
		
		// Construct the blame command
		BlameGenerator bg = new BlameGenerator(repoFile, currentFile.getFile_id());
		bg.push(null, repoFile.resolve(currentCommit.getCommit_id()));
		bg.setFollowFileRenames(true);
		BlameResult blameRes = bg.computeBlameResult();
		
		// For each line in the file
		for(int i = 0;i < blameRes.getResultContents().size();i++)
		{
			PersonIdent sourceAuthor = blameRes.getSourceAuthor(i);
			String sourceCommitId = blameRes.getSourceCommit(i).getName();
			
			if (sourceCommitId == "" || sourceAuthor == null)
			{
				continue;	// safety
			}
			else if (rec == null)
			{
				rec = new BlameResultRecord();
				rec.setAuthorId		 (sourceAuthor.getEmailAddress());
				rec.setSourceCommitId(blameRes.getSourceCommit(i).getName());
				rec.setFileId		 (currentFile.getFile_id());
				rec.setCommitId		 (currentCommit.getCommit_id());
				rec.setLineStart(i+1);
			}
			else if (!rec.getSourceCommitId().equals(sourceCommitId))
			{
				// finish off the last record
				rec.setLineEnd(i);
				rec.setType(TechnicalResources.ChangeType.valueOf(change.toString()));
				
				// Convert from line -> char
				rec.setLineStart(Resources.convertLineStartToCharStart(rec.getLineStart(), currentFile.getRaw_file()));
				rec.setLineEnd	(Resources.convertLineEndToCharEnd	  (rec.getLineEnd()  , currentFile.getRaw_file()));
				db.insertOwnerRecord(rec);
				
				// we have a new owner
				rec = new BlameResultRecord();
				rec.setAuthorId		 (sourceAuthor.getEmailAddress());
				rec.setSourceCommitId(blameRes.getSourceCommit(i).getName());
				rec.setFileId		 (currentFile.getFile_id());
				rec.setCommitId		 (currentCommit.getCommit_id());
				rec.setLineStart(i+1);
			}
			else if (rec.getSourceCommitId().equals(sourceCommitId))
			{
				// we have the same owner
				rec.setLineEnd(i+1); 				
			}
		}
		
		// Insert the last record
		if (rec != null)
		{
			rec.setType(TechnicalResources.ChangeType.valueOf(change.toString()));
			
			// Convert from line -> char
			rec.setLineStart(Resources.convertLineStartToCharStart(rec.getLineStart(), currentFile.getRaw_file()));
			rec.setLineEnd  (Resources.convertLineEndToCharEnd    (rec.getLineEnd()  , currentFile.getRaw_file()));
			db.insertOwnerRecord(rec);
		}
	}
}
