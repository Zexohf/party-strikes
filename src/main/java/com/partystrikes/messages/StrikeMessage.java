package com.partystrikes.messages;

import com.partystrikes.StrikeRecord;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * Broadcast when one party member strikes another.
 */
public class StrikeMessage extends PartyMemberMessage
{
	private final StrikeRecord record;

	public StrikeMessage(StrikeRecord record)
	{
		this.record = record;
	}

	public StrikeRecord getRecord()
	{
		return record;
	}
}
