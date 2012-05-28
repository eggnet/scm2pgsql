package db;

public class FileDiffsTO {
	private String file_id;
	private String commit_id;
	private String diff_text;
	private int char_start;
	private int char_end;
	private String diff_type;
	
	public FileDiffsTO() { }
	public FileDiffsTO(String file_id, String commit_id, String diff_text, int chart_start, int char_end, String diff_type) {
		super();
		this.file_id = file_id;
		this.commit_id = commit_id;
		this.diff_text = diff_text;
		this.char_start = chart_start;
		this.char_end = char_end;
		this.diff_type = diff_type;
	}
	public String getFile_id() {
		return file_id;
	}
	public void setFile_id(String file_id) {
		this.file_id = file_id;
	}
	public String getCommit_id() {
		return commit_id;
	}
	public void setCommit_id(String commit_id) {
		this.commit_id = commit_id;
	}
	public String getDiff_text() {
		return diff_text;
	}
	public void setDiff_text(String diff_text) {
		this.diff_text = diff_text;
	}
	public int getChar_start() {
		return char_start;
	}
	public void setChar_start(int char_start) {
		this.char_start = char_start;
	}
	public int getChar_end() {
		return char_end;
	}
	public void setChar_end(int char_end) {
		this.char_end = char_end;
	}
	public String getDiff_type() {
		return diff_type;
	}
	public void setDiff_type(String diff_type) {
		this.diff_type = diff_type;
	}
	
	
	
}
