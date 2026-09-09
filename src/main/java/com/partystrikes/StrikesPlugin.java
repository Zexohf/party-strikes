package com.partystrikes;

import com.google.inject.Provides;
import com.partystrikes.messages.StrikeMessage;
import com.partystrikes.messages.StrikeReset;
import com.partystrikes.messages.StrikeSyncRequest;
import com.partystrikes.messages.StrikeSyncResponse;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Party Strikes",
	description = "Give your party members strikes when they mess up. Three strikes and you're out!",
	tags = {"party", "strikes", "fun", "clan", "group", "meme"}
)
public class StrikesPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private PartyService partyService;
	@Inject private WSClient wsClient;
	@Inject private StrikesConfig config;
	@Inject private OverlayManager overlayManager;
	@Inject private ClientToolbar clientToolbar;
	@Inject private StrikesOverlay overlay;

	// targetMemberId -> strike count
	private final Map<Long, Integer> counts = new ConcurrentHashMap<>();
	// full log of every strike (used to sync late joiners); guarded by itself
	private final List<StrikeRecord> records = new ArrayList<>();
	// strike ids we've already processed, so echoes / re-syncs never double count
	private final Set<String> seen = ConcurrentHashMap.newKeySet();

	private StrikesPanel panel;
	private NavigationButton navButton;

	@Provides
	StrikesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(StrikesConfig.class);
	}

	@Override
	protected void startUp()
	{
		wsClient.registerMessage(StrikeMessage.class);
		wsClient.registerMessage(StrikeSyncRequest.class);
		wsClient.registerMessage(StrikeSyncResponse.class);
		wsClient.registerMessage(StrikeReset.class);

		panel = new StrikesPanel(this);
		navButton = NavigationButton.builder()
			.tooltip("Party Strikes")
			.icon(createIcon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		overlay.setPlugin(this);
		overlayManager.add(overlay);

		SwingUtilities.invokeLater(panel::update);

		// If we enabled the plugin while already in a party, ask everyone for the current tally.
		if (partyService.isInParty())
		{
			partyService.send(new StrikeSyncRequest());
		}
	}

	@Override
	protected void shutDown()
	{
		wsClient.unregisterMessage(StrikeMessage.class);
		wsClient.unregisterMessage(StrikeSyncRequest.class);
		wsClient.unregisterMessage(StrikeSyncResponse.class);
		wsClient.unregisterMessage(StrikeReset.class);

		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);

		counts.clear();
		seen.clear();
		synchronized (records)
		{
			records.clear();
		}
		panel = null;
	}

	// ---- Actions called from the side panel (EDT) ----

	public void giveStrike(long targetId, String reason)
	{
		if (!partyService.isInParty() || partyService.getLocalMember() == null)
		{
			addGameMessage("Party Strikes: you need to be in a party first (see the Party plugin).");
			return;
		}

		String r = reason == null ? "" : reason.trim();
		StrikeRecord rec = new StrikeRecord(
			UUID.randomUUID().toString(),
			targetId,
			partyService.getLocalMember().getMemberId(),
			r,
			Instant.now().toEpochMilli());

		apply(rec);                                 // update our own client immediately
		partyService.send(new StrikeMessage(rec));  // broadcast to the rest of the party
	}

	public void resetAll()
	{
		clearState();
		SwingUtilities.invokeLater(this::refreshPanel);
		if (partyService.isInParty())
		{
			partyService.send(new StrikeReset());
		}
		addGameMessage("Party Strikes: the slate has been wiped clean.");
	}

	// ---- Incoming party messages ----

	@Subscribe
	public void onStrikeMessage(StrikeMessage message)
	{
		apply(message.getRecord());
	}

	@Subscribe
	public void onStrikeSyncRequest(StrikeSyncRequest request)
	{
		broadcastFullState();
	}

	@Subscribe
	public void onStrikeSyncResponse(StrikeSyncResponse response)
	{
		if (response.getRecords() == null)
		{
			return;
		}
		for (StrikeRecord rec : response.getRecords())
		{
			mergeSilently(rec);
		}
		SwingUtilities.invokeLater(this::refreshPanel);
	}

	@Subscribe
	public void onStrikeReset(StrikeReset reset)
	{
		clearState();
		SwingUtilities.invokeLater(this::refreshPanel);
	}

	@Subscribe
	public void onUserJoin(UserJoin event)
	{
		// Someone new joined -> share what we know so their tally matches ours.
		broadcastFullState();
	}

	// ---- Core bookkeeping ----

	private void apply(StrikeRecord rec)
	{
		if (rec == null || !seen.add(rec.getStrikeId()))
		{
			return; // duplicate / echo
		}
		synchronized (records)
		{
			records.add(rec);
		}
		int total = counts.merge(rec.getTargetId(), 1, Integer::sum);
		announce(rec, total);
		SwingUtilities.invokeLater(this::refreshPanel);
	}

	private void mergeSilently(StrikeRecord rec)
	{
		if (rec == null || !seen.add(rec.getStrikeId()))
		{
			return;
		}
		synchronized (records)
		{
			records.add(rec);
		}
		counts.merge(rec.getTargetId(), 1, Integer::sum);
	}

	private void broadcastFullState()
	{
		List<StrikeRecord> all;
		synchronized (records)
		{
			all = new ArrayList<>(records);
		}
		if (!all.isEmpty())
		{
			partyService.send(new StrikeSyncResponse(all));
		}
	}

	private void clearState()
	{
		counts.clear();
		seen.clear();
		synchronized (records)
		{
			records.clear();
		}
	}

	private void announce(StrikeRecord rec, int total)
	{
		boolean iAmTarget = rec.getTargetId() == getLocalMemberId();

		if (config.announce() && (!config.onlyAnnounceMine() || iAmTarget))
		{
			String giver = nameFor(rec.getGiverId());
			String target = nameFor(rec.getTargetId());
			String reason = rec.getReason().isEmpty() ? "" : " for \"" + rec.getReason() + "\"";
			addGameMessage("Party Strikes: " + giver + " struck " + target + reason
				+ " -- that's " + total + " now.");
		}

		if (config.sound() && (!config.onlyAnnounceMine() || iAmTarget))
		{
			playSound(config.strikeSoundId());
		}

		int out = config.strikesToOut();
		if (out > 0 && total == out && config.announce())
		{
			addGameMessage("Party Strikes: >>> " + nameFor(rec.getTargetId())
				+ " is OUT! " + total + " strikes! <<<");
		}
	}

	// ---- Data accessors used by panel + overlay ----

	public boolean isInParty()
	{
		return partyService.isInParty();
	}

	public List<PartyMember> getMembers()
	{
		return new ArrayList<>(partyService.getMembers());
	}

	public int getStrikes(long memberId)
	{
		return counts.getOrDefault(memberId, 0);
	}

	public long getLocalMemberId()
	{
		PartyMember m = partyService.getLocalMember();
		return m == null ? -1 : m.getMemberId();
	}

	public String nameFor(long memberId)
	{
		PartyMember m = partyService.getMemberById(memberId);
		if (m != null && m.getDisplayName() != null && !m.getDisplayName().isEmpty())
		{
			return m.getDisplayName();
		}
		return "Unknown";
	}

	public static String titleFor(int strikes)
	{
		if (strikes <= 0) return "Squeaky Clean";
		if (strikes == 1) return "Rookie Mistake";
		if (strikes == 2) return "On Thin Ice";
		if (strikes == 3) return "Three Strikes!";
		if (strikes <= 5) return "Certified Menace";
		return "Walking Disaster";
	}

	// ---- Helpers ----

	private void refreshPanel()
	{
		if (panel != null)
		{
			panel.update();
		}
	}

	private void addGameMessage(String msg)
	{
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null);
			}
		});
	}

	private void playSound(int id)
	{
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				client.playSoundEffect(id);
			}
		});
	}

	private static BufferedImage createIcon()
	{
		BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(200, 45, 45));
		g.fillOval(1, 1, 22, 22);
		g.setColor(Color.WHITE);
		g.setFont(new Font("SansSerif", Font.BOLD, 17));
		g.drawString("!", 9, 18);
		g.dispose();
		return img;
	}
}
