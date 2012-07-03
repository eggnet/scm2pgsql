package git;

import db.TechnicalResources;

public class BlameResultRecord
{
	private String commitId;
	private String sourceCommitId;
	private String AuthorId;
	private String FileId;
	private TechnicalResources.ChangeType Type;
	private int charStart;
	private int charEnd;
	public BlameResultRecord() {}
	
	public String getCommitId()
	{
		return commitId;
	}
	public void setCommitId(String CommitId)
	{
		commitId = CommitId;
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
	public TechnicalResources.ChangeType getType()
	{
		return Type;
	}
	public void setType(TechnicalResources.ChangeType type)
	{
		Type = type;
	}
	public int getLineStart()
	{
		return charStart;
	}
	public void setLineStart(int lineStart)
	{
		charStart = lineStart;
	}
	public int getLineEnd()
	{
		return charEnd;
	}
	public void setLineEnd(int lineEnd)
	{
		charEnd = lineEnd;
	}

	public String getSourceCommitId()
	{
		return sourceCommitId;
	}

	public void setSourceCommitId(String sourceCommitId)
	{
		this.sourceCommitId = sourceCommitId;
	}
	
}
