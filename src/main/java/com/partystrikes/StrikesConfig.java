package com.partystrikes;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("partystrikes")
public interface StrikesConfig extends Config
{
	@ConfigItem(
		keyName = "announce",
		name = "Chat announcements",
		description = "Print a game message whenever a strike is handed out.",
		position = 0
	)
	default boolean announce()
	{
		return true;
	}

	@ConfigItem(
		keyName = "onlyAnnounceMine",
		name = "Only announce/sound for me",
		description = "Only react (chat + sound) when the strike is aimed at you.",
		position = 1
	)
	default boolean onlyAnnounceMine()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sound",
		name = "Play a sound",
		description = "Play a sound effect when a strike lands.",
		position = 2
	)
	default boolean sound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "strikeSoundId",
		name = "Strike sound ID",
		description = "In-game sound effect ID to play. 200 = teleport whoosh. Try 2266, 2277, 3813 for other vibes.",
		position = 3
	)
	default int strikeSoundId()
	{
		return 200;
	}

	@ConfigItem(
		keyName = "strikesToOut",
		name = "Strikes to \"out\"",
		description = "How many strikes triggers the big OUT announcement. Set 0 to disable.",
		position = 4
	)
	default int strikesToOut()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show overlay",
		description = "Show a small overlay with your own strike count and title.",
		position = 5
	)
	default boolean showOverlay()
	{
		return true;
	}
}
