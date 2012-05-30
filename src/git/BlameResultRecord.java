package git;

import scm2pgsql.Resources;

public class BlameResultRecord
{
	private String CommitId;
	private String AuthorId;
	private String FileId;
	private Resources.ChangeType Type;
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
	public Resources.ChangeType getType()
	{
		return Type;
	}
	public void setType(Resources.ChangeType type)
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
