package com.partystrikes;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class StrikesOverlay extends OverlayPanel
{
	private final StrikesConfig config;
	private StrikesPlugin plugin;

	@Inject
	StrikesOverlay(StrikesConfig config)
	{
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	void setPlugin(StrikesPlugin plugin)
	{
		this.plugin = plugin;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (plugin == null || !config.showOverlay() || !plugin.isInParty())
		{
			return null;
		}

		long id = plugin.getLocalMemberId();
		if (id == -1)
		{
			return null;
		}
		int strikes = plugin.getStrikes(id);

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Party Strikes")
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Yours:")
			.right(String.valueOf(strikes))
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left(StrikesPlugin.titleFor(strikes))
			.build());

		return super.render(graphics);
	}
}
