package com.partystrikes.messages;

import com.partystrikes.StrikeRecord;
import java.util.List;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * The full log of known strikes, sent in reply to a sync request or when a new member joins.
 * Receivers merge by strikeId, so overlapping histories converge to the same tally.
 */
public class StrikeSyncResponse extends PartyMemberMessage
{
	private final List<StrikeRecord> records;

	public StrikeSyncResponse(List<StrikeRecord> records)
	{
		this.records = records;
	}

	public List<StrikeRecord> getRecords()
	{
		return records;
	}
}
