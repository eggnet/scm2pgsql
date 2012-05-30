package git;

import scm2pgsql.GitResources;

public class BlameResultRecord
{
	private String CommitId;
	private String AuthorId;
	private String FileId;
	private GitResources.ChangeType Type;
	private int LineStart;
	private int LineEnd;
	public BlameResultRecord() {}
	
	public String getCommitId()
	{
		return CommitId;
	}
	public void setCommitId(String commitId)
	{
		CommitId = commitId;
	}
	public String getAuthorId()
	{
		return AuthorId;
	}
	public void setAuthorId(String authorId)
	{
		AuthorId = authorId;
	}
	public String getFileId()
	{
		return FileId;
	}
	public void setFileId(String fileId)
	{
		FileId = fileId;
	}
	public GitResources.ChangeType getType()
	{
		return Type;
	}
	public void setType(GitResources.ChangeType type)
	{
		Type = type;
	}
	public int getLineStart()
	{
		return LineStart;
	}
	public void setLineStart(int lineStart)
	{
		LineStart = lineStart;
	}
	public int getLineEnd()
	{
		return LineEnd;
	}
	public void setLineEnd(int lineEnd)
	{
		LineEnd = lineEnd;
	}
	
}
