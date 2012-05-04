package db;

public class BranchEntryTO {
	private String branch_id;
	private String branch_name;
	private String commit_id;	// references commits.commit_id
	public BranchEntryTO() { }
	public String getBranch_id() {
		return branch_id;
	}
	public void setBranch_id(String branchId) {
		branch_id = branchId;
	}
	public String getBranch_name() {
		return branch_name;
	}
	public void setBranch_name(String branchName) {
		branch_name = branchName;
	}
	public String getCommit_id() {
		return commit_id;
	}
	public void setCommit_id(String commitId) {
		commit_id = commitId;
	}
}
