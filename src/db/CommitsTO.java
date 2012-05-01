package db;

import java.util.Date;

public class CommitsTO {
	
	private int id;
	private String commit_id;
	private String author;
	private String author_email;
	private String comment;
	private Date commit_date;
	private String[] changed_files;
	private String revision_id;
	
	public CommitsTO(int id, String commit_id, String author, String author_email,
			String comment, Date commit_date, String[] changed_files, String revision_id)
	{
		this.id = id;
		this.commit_id = commit_id;
		this.author = author;
		this.author_email = author_email;
		this.comment = comment;
		this.commit_date = commit_date;
		this.changed_files = changed_files;
		this.revision_id = revision_id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCommit_id() {
		return commit_id;
	}

	public void setCommit_id(String commit_id) {
		this.commit_id = commit_id;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getAuthor_email() {
		return author_email;
	}

	public void setAuthor_email(String author_email) {
		this.author_email = author_email;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Date getCommit_date() {
		return commit_date;
	}

	public void setCommit_date(Date commit_date) {
		this.commit_date = commit_date;
	}

	public String[] getChanged_files() {
		return changed_files;
	}

	public void setChanged_files(String[] changed_files) {
		this.changed_files = changed_files;
	}

	public String getRevision_id() {
		return revision_id;
	}

	public void setRevision_id(String revision_id) {
		this.revision_id = revision_id;
	}
		

}
