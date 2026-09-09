package com.partystrikes;

/**
 * A single strike event. Serialized with Gson and sent across the party WebSocket,
 * so keep the fields simple. The strikeId makes each event unique, which lets every
 * client de-duplicate echoes and re-syncs and still arrive at the same tally.
 */
public class StrikeRecord
{
	private final String strikeId;
	private final long targetId;
	private final long giverId;
	private final String reason;
	private final long timestamp;

	public StrikeRecord(String strikeId, long targetId, long giverId, String reason, long timestamp)
	{
		this.strikeId = strikeId;
		this.targetId = targetId;
		this.giverId = giverId;
		this.reason = reason == null ? "" : reason;
		this.timestamp = timestamp;
	}

	public String getStrikeId()
	{
		return strikeId;
	}

	public long getTargetId()
	{
		return targetId;
	}

	public long getGiverId()
	{
		return giverId;
	}

	public String getReason()
	{
		return reason == null ? "" : reason;
	}

	public long getTimestamp()
	{
		return timestamp;
	}
}
