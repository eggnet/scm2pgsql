package db;

import git.BlameResultRecord;
import db.util.ISetter;
import db.util.ISetter.IntSetter;
import db.util.ISetter.StringSetter;
import db.util.PreparedCallExecutionItem;
import db.util.PreparedStatementExecutionItem;

public class GitDb extends TechnicalDb
{
	public GitDb () {
		super();
	}
	
	public boolean InsertFileDiff(FileDiffsTO diff)
	{
		String query = "INSERT INTO file_diffs (file_id, new_commit_id, old_commit_id, diff_text, char_start, char_end, diff_type)" +
				" VALUES(?, ?, ?, ?, ?, ?, ?);";
		ISetter[] params = {new StringSetter(1, diff.getFile_id()),
				              new StringSetter(2, diff.getNewCommit_id()),
				              new StringSetter(3, diff.getOldCommit_id()),
				              new StringSetter(4, diff.getDiff_text()),
				              new IntSetter(5, diff.getChar_start()),
				              new IntSetter(6, diff.getChar_end()),
				              new StringSetter(7, diff.getDiff_type().toString())};
		PreparedStatementExecutionItem ei = new PreparedStatementExecutionItem(query, params);
	    addExecutionItem(ei);
	    return true;
	}
	
	public boolean InsertCommit(CommitsTO commit)
	{
		String query = "INSERT INTO commits (id, commit_id, author, author_email, comments, commit_date," +
				"branch_id) VALUES(" +
				"default, ?, ?, ?, ?, '" + commit.getCommit_date().toString() + "', ?);";
		ISetter[] params = {new StringSetter(1, commit.getCommit_id()), 
				              new StringSetter(2, commit.getAuthor()), 
				              new StringSetter(3, commit.getAuthor_email()), 
				              new StringSetter(4, commit.getComment()), 
				              new StringSetter(5, commit.getBranch_id())};
		PreparedStatementExecutionItem ei = new PreparedStatementExecutionItem(query, params);
	    addExecutionItem(ei);
	    return true;
	}
	
	public boolean InsertBranchEntry(BranchEntryTO branchEntry)
	{
		String query = "INSERT INTO branches (branch_id, branch_name, commit_id) VALUES(?, ?, ?);";
		ISetter[] params = {new StringSetter(1, branchEntry.getBranch_id()), 
				              new StringSetter(2, branchEntry.getBranch_name()), 
				              new StringSetter(3, branchEntry.getCommit_id())};
		PreparedStatementExecutionItem ei = new PreparedStatementExecutionItem(query, params);
		addExecutionItem(ei);
		return true;
	}
	
	public void insertOwnerRecord(BlameResultRecord rec)
	{
		String query = "{call upsert_owner_rec(?,?,?,?,?,?,?) } ";
		ISetter[] params = {new StringSetter(1,rec.getCommitId()), 
				              new StringSetter(2,rec.getSourceCommitId()), 
				              new StringSetter(3,rec.getAuthorId()),
				              new StringSetter(4,rec.getFileId()),
				              new IntSetter(5,rec.getLineStart()),
				              new IntSetter(6,rec.getLineEnd()),
				              new StringSetter(7,rec.getType().toString())};
		PreparedCallExecutionItem ei = new PreparedCallExecutionItem(query, params);
		addExecutionItem(ei);
	}			
			
	public void insertCommitFamilyEntry(String commit, String parentCommit)
	{
		String query = "INSERT INTO commit_family (parent, child) VALUES(?, ?);";
		ISetter[] params = {new StringSetter(1,parentCommit), new StringSetter(2,commit)};
		PreparedStatementExecutionItem ei = new PreparedStatementExecutionItem(query, params);
		addExecutionItem(ei);
	}
	
	public void InsertFiles(FilesTO files)
	 {
		String query = "INSERT INTO file_caches (commit_id, file_id, raw_file) VALUES(?, ?, ?);";
		ISetter[] params = {new StringSetter(1, files.getCommit_id()),
							new StringSetter(2, files.getFile_id()),
							new StringSetter(3, files.getRaw_file())};
		
		PreparedStatementExecutionItem ei = new PreparedStatementExecutionItem(query, params);
		addExecutionItem(ei);
	  }
}
