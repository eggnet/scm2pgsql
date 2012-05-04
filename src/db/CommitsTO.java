package db;

import java.util.Date;
import java.util.Set;

public class CommitsTO {
	
	private int id;
	private String commit_id;
	private String author;
	private String author_email;
	private String comment;
	private Date commit_date;
	private Set<String> changed_files;
	private Set<String> file_structure;

	private String branch_id;
	
	public CommitsTO() { }

	public CommitsTO(int id, String commit_id, String author, String author_email,
			String comment, Date commit_date, Set<String> changed_files)
	{
		this.id = id;
		this.commit_id = commit_id;
		this.author = author;
		this.author_email = author_email;
		this.comment = comment;
		this.commit_date = commit_date;
		this.changed_files = changed_files;
	}

	public CommitsTO(String commit_id, String author, String author_email,
			String comment, Date commit_date, Set<String> changed_files) {
		super();
		this.commit_id = commit_id;
		this.author = author;
		this.author_email = author_email;
		this.comment = comment;
		this.commit_date = commit_date;
		this.changed_files = changed_files;
	}
	
	public Set<String> getFile_structure() {
		return file_structure;
	}
	
	public void setFile_structure(Set<String> fileStructure) {
		file_structure = fileStructure;
	}

	public String getBranch_id() {
		return branch_id;
	}
	
	public void setBranch_id(String branchId) {
		branch_id = branchId;
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

	public Set<String> getChanged_files() {
		return changed_files;
	}

	public void setChanged_files(Set<String> changed_files) {
		this.changed_files = changed_files;
	}

	public String getChanged_filesAsString() {
		String str = "{";
		for (String filename: this.getChanged_files())
			str += filename + ",";
		str = str.substring(0, str.lastIndexOf(",")-1);
		str += "}";
		return str;
	}
}
