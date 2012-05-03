package db;

public class FilesTO {
	private int id;
	private String file_id;
	private String file_name;
	private String commit_id;
	private String raw_file;
	public FilesTO(String file_id, String file_name, String commit_id,
			String raw_file) {
		super();
		this.file_id = file_id;
		this.file_name = file_name;
		this.commit_id = commit_id;
		this.raw_file = raw_file;
	}

	
	public FilesTO(int id, String file_id, String file_name, String commit_id,
			String raw_file) {
		super();
		this.id = id;
		this.file_id = file_id;
		this.file_name = file_name;
		this.commit_id = commit_id;
		this.raw_file = raw_file;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getFile_id() {
		return file_id;
	}

	public void setFile_id(String file_id) {
		this.file_id = file_id;
	}

	public String getFile_name() {
		return file_name;
	}

	public void setFile_name(String file_name) {
		this.file_name = file_name;
	}

	public String getCommit_id() {
		return commit_id;
	}

	public void setCommit_id(String commit_id) {
		this.commit_id = commit_id;
	}

	public String getRaw_file() {
		return raw_file;
	}

	public void setRaw_file(String raw_file) {
		this.raw_file = raw_file;
	}
}
