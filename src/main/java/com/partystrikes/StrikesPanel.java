package com.partystrikes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import net.runelite.client.party.PartyMember;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class StrikesPanel extends PluginPanel
{
	private final StrikesPlugin plugin;
	private final JTextField reasonField = new JTextField();
	private final JPanel listPanel = new JPanel();
	private final JLabel statusLabel = new JLabel();

	StrikesPanel(StrikesPlugin plugin)
	{
		this.plugin = plugin;
		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

		JLabel title = new JLabel("Party Strikes");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel hint = new JLabel("Type a reason, then hit Strike!");
		hint.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		hint.setFont(FontManager.getRunescapeSmallFont());
		hint.setAlignmentX(Component.LEFT_ALIGNMENT);

		reasonField.setToolTipText("Optional reason for the strike");
		reasonField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		reasonField.setAlignmentX(Component.LEFT_ALIGNMENT);

		top.add(title);
		top.add(Box.createVerticalStrut(4));
		top.add(hint);
		top.add(Box.createVerticalStrut(6));
		top.add(reasonField);

		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

		JButton reset = new JButton("Reset party strikes");
		reset.setFocusable(false);
		reset.addActionListener(e -> plugin.resetAll());

		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setBorder(new EmptyBorder(6, 0, 6, 0));

		JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(statusLabel, BorderLayout.NORTH);
		bottom.add(reset, BorderLayout.SOUTH);

		add(top, BorderLayout.NORTH);
		add(listPanel, BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);
	}

	/** Rebuild the member list. Must be called on the Swing EDT. */
	void update()
	{
		listPanel.removeAll();

		if (!plugin.isInParty())
		{
			statusLabel.setText("Not in a party.");
			JLabel none = new JLabel("Join a party (Party plugin) to start.");
			none.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			none.setFont(FontManager.getRunescapeSmallFont());
			listPanel.add(none);
			listPanel.revalidate();
			listPanel.repaint();
			return;
		}

		List<PartyMember> members = new ArrayList<>(plugin.getMembers());
		// Wall of shame first: most strikes at the top.
		members.sort(Comparator.comparingInt((PartyMember m) -> plugin.getStrikes(m.getMemberId())).reversed());

		statusLabel.setText(members.size() + " member(s) in party");

		for (PartyMember m : members)
		{
			listPanel.add(buildRow(m));
			listPanel.add(Box.createVerticalStrut(4));
		}

		listPanel.revalidate();
		listPanel.repaint();
	}

	private JPanel buildRow(PartyMember m)
	{
		final long id = m.getMemberId();
		int strikes = plugin.getStrikes(id);
		String name = (m.getDisplayName() == null || m.getDisplayName().isEmpty())
			? "Unknown" : m.getDisplayName();

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBorder(new EmptyBorder(6, 6, 6, 6));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

		JPanel textCol = new JPanel();
		textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
		textCol.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel nameLabel = new JLabel(name + "  \u00b7  " + strikes);
		nameLabel.setForeground(strikes >= 3 ? new Color(220, 90, 90) : Color.WHITE);

		JLabel titleLabel = new JLabel(StrikesPlugin.titleFor(strikes));
		titleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		titleLabel.setFont(FontManager.getRunescapeSmallFont());

		textCol.add(nameLabel);
		textCol.add(titleLabel);

		JButton strikeBtn = new JButton("Strike!");
		strikeBtn.setFocusable(false);
		strikeBtn.addActionListener(e ->
		{
			plugin.giveStrike(id, reasonField.getText());
			reasonField.setText("");
		});

		row.add(textCol, BorderLayout.CENTER);
		row.add(strikeBtn, BorderLayout.EAST);
		return row;
	}
}
