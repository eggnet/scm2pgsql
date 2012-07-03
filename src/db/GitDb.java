package db;

import git.BlameResultRecord;

import java.io.InputStreamReader;

import scm2pgsql.GitResources;

import db.util.ISetter;
import db.util.ISetter.StringSetter;
import db.util.ISetter.IntSetter;
import db.util.PreparedCallExecutionItem;
import db.util.PreparedStatementExecutionItem;

public class GitDb extends DbConnection
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
			
	/**
	 * Creates a db on the current connection.
	 * @param dbName
	 * @return true for success
	 */
	public boolean createDB(String dbName)
	{
		try {
			// Drop the DB if it already exists
			String query = "DROP DATABASE IF EXISTS " + dbName;
			PreparedStatementExecutionItem ei = new PreparedStatementExecutionItem(query, null);
			addExecutionItem(ei);
			ei.waitUntilExecuted();
			
			// First create the DB.
			if (GitResources.SET_ENC)
				query = "CREATE DATABASE " + dbName + " ENCODING 'UTF8' TEMPLATE template0 LC_COLLATE 'C' LC_CTYPE 'C';";
			else
				query = "CREATE DATABASE " + dbName;
			ei = new PreparedStatementExecutionItem(query, null);
			addExecutionItem(ei);
			ei.waitUntilExecuted();
			
			// Reconnect to our new database.
			close();
			connect(dbName.toLowerCase());
			
			// load our schema			
			runScript(new InputStreamReader(GitResources.class.getResourceAsStream("createdb.sql")));
			//--------------------------------------------------------------------------------------
			// Stored procedure for checking before inserting in a batch.											
			// http://stackoverflow.com/questions/1109061/insert-on-duplicate-update-postgresql			
			//--------------------------------------------------------------------------------------
			query = "CREATE OR REPLACE FUNCTION upsert_owner_rec(c_id varchar(255), s_c_id varchar(255), a_id varchar(255), f_id varchar(255), c_start INT, c_end INT, c_type varchar(12)) RETURNS VOID AS" +
						"'" +
						" DECLARE " + 
							"dummy integer;" + 
						" BEGIN " +
							" LOOP " +
								" select owners.char_start into dummy from owners where commit_id=c_id and source_commit_id=s_c_id and owner_id=a_id and file_id=f_id and char_start=c_start and char_end=c_end and change_type=c_type;" +
								" IF found THEN " +
									" RETURN ;" +
								" END IF;" +
								" BEGIN " +
									" INSERT INTO owners VALUES (c_id, s_c_id, a_id, f_id, c_start, c_end, c_type);" +
									" RETURN; " +
								" EXCEPTION WHEN unique_violation THEN " +
								" END; " +
							" END LOOP;" +
						" END; " +
						"'" +
					" LANGUAGE plpgsql;";
			ei = new PreparedStatementExecutionItem(query, null);
			addExecutionItem(ei);
			ei.waitUntilExecuted();
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
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
