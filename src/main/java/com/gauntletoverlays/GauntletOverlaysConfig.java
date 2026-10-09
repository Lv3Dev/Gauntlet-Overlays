package com.gauntletoverlays;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("gauntletoverlays")
public interface GauntletOverlaysConfig extends Config
{
	@ConfigItem(
		keyName = "highlightPrayer",
		name = "Highlight prayer tab",
		description = "Highlight the overhead prayer to use in the open prayer book while in the Hunllef room",
		position = 0
	)
	default boolean highlightPrayer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCounter",
		name = "Show counter",
		description = "Show Hunllef's current attack style and count",
		position = 1
	)
	default boolean showCounter()
	{
		return true;
	}
}
