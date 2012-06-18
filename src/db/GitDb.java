package db;

import git.BlameResultRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.revplot.PlotCommit;

import scm2pgsql.GitResources;

public class GitDb extends DbConnection
{
	public boolean connect(String dbName)
	{
		super.connect(dbName);
		try {
			callableBatch = conn.prepareCall("{call upsert_owner_rec(?,?,?,?,?,?,?) } ");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public GitDb () {
		super();
	}
	
	public boolean execCallableBatch() {
		try {
			callableBatch.executeBatch();
			callableBatch.clearBatch();
			return true;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean InsertFileDiff(FileDiffsTO diff)
	{
		try { 
			PreparedStatement s = conn.prepareStatement(
					"INSERT INTO file_diffs (file_id, new_commit_id, old_commit_id, diff_text, char_start, char_end, diff_type)" +
					" VALUES(?, ?, ?, ?, ?, ?, ?);");
			s.setString(1, diff.getFile_id());
			s.setString(2, diff.getNewCommit_id());
			s.setString(3, diff.getOldCommit_id());
			s.setString(4, diff.getDiff_text());
			s.setInt(5, diff.getChar_start());
			s.setInt(6, diff.getChar_end());
			s.setString(7, diff.getDiff_type().toString());
			s.execute();
		}
		catch (SQLException e)
		{
			return false;
		}
		return true;
	}
	
	public boolean InsertCommit(CommitsTO commit)
	{
	    try {
		    PreparedStatement s = conn.prepareStatement(
					"INSERT INTO commits (id, commit_id, author, author_email, comments, commit_date," +
					"branch_id) VALUES(" +
					"default, ?, ?, ?, ?, '" + commit.getCommit_date().toString() + "', ?);");
			s.setString(1, commit.getCommit_id());
			s.setString(2, commit.getAuthor());
			s.setString(3, commit.getAuthor_email());
			s.setString(4, commit.getComment());
		    s.setString(5, commit.getBranch_id());
			s.execute();
	    }
	    catch (SQLException e)
	    {
	    	e.printStackTrace();
	    	return false;
	    }
	    return true;
	}
	
	public boolean InsertBranchEntry(BranchEntryTO branchEntry)
	{
		try { 
			PreparedStatement s = conn.prepareStatement(
					"INSERT INTO branches (branch_id, branch_name, commit_id)" +
					" VALUES(?, ?, ?);");
			s.setString(1, branchEntry.getBranch_id());
			s.setString(2, branchEntry.getBranch_name());
			s.setString(3, branchEntry.getCommit_id());
			s.execute();
		}
		catch (SQLException e)
		{
			return false;
		}
		return true;
	}
	
	public void insertOwnerRecord(BlameResultRecord rec)
	{
		try
		{
			callableBatch.setString(1, rec.getCommitId());
			callableBatch.setString(2, rec.getSourceCommitId());
			callableBatch.setString(3, rec.getAuthorId());
			callableBatch.setString(4, rec.getFileId());
			callableBatch.setInt(5, rec.getLineStart());
			callableBatch.setInt(6, rec.getLineEnd());
			callableBatch.setString(7, rec.getType().toString());
			callableBatch.addBatch();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}			
			
	/**
	 * Creates a db on the current connection.
	 * @param dbName
	 * @return true for success
	 */
	public boolean createDB(String dbName)
	{
		PreparedStatement s;
		try {
			// Drop the DB if it already exists
			s = conn.prepareStatement("DROP DATABASE IF EXISTS " + dbName);
			s.execute();
			
			// First create the DB.
			if (GitResources.SET_ENC)
				s = conn.prepareStatement("CREATE DATABASE " + dbName + " ENCODING 'UTF8' TEMPLATE template0 LC_COLLATE 'C' LC_CTYPE 'C';");
			else
				s = conn.prepareStatement("CREATE DATABASE " + dbName);
			
			s.execute();
			
			// Reconnect to our new database.
			connect(dbName.toLowerCase());
			
			// load our schema
			sr.runScript(new InputStreamReader(GitResources.class.getResourceAsStream("createdb.sql")));
			//--------------------------------------------------------------------------------------
			// Stored procedure for checking before inserting in a batch.											
			// http://stackoverflow.com/questions/1109061/insert-on-duplicate-update-postgresql			
			//--------------------------------------------------------------------------------------
			s = conn.prepareStatement(
					"CREATE OR REPLACE FUNCTION upsert_owner_rec(c_id varchar(255), s_c_id varchar(255), a_id varchar(255), f_id varchar(255), c_start INT, c_end INT, c_type varchar(12)) RETURNS VOID AS" +
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
					" LANGUAGE plpgsql;");
			s.execute();
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void insertCommitFamilyEntry(String commit, String parentCommit)
	{
		try { 
			PreparedStatement s = conn.prepareStatement(
					"INSERT INTO commit_family (parent, child)" +
					" VALUES(?, ?)");
			s.setString(1, parentCommit);
			s.setString(2, commit);
			currentBatch.addBatch(s.toString());
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
}
