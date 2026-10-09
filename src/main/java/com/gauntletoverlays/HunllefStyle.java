package com.gauntletoverlays;

import java.awt.Color;
import net.runelite.api.gameval.InterfaceID;

enum HunllefStyle
{
	RANGE("Range", InterfaceID.Prayerbook.PRAYER14, new Color(0, 255, 0, 80), new Color(0, 255, 0)),
	MAGIC("Magic", InterfaceID.Prayerbook.PRAYER13, new Color(0, 128, 255, 80), new Color(0, 128, 255));

	private final String label;
	private final int componentId;
	private final Color fill;
	private final Color outline;

	HunllefStyle(String label, int componentId, Color fill, Color outline)
	{
		this.label = label;
		this.componentId = componentId;
		this.fill = fill;
		this.outline = outline;
	}

	String getLabel()
	{
		return label;
	}

	int getComponentId()
	{
		return componentId;
	}

	Color getFill()
	{
		return fill;
	}

	Color getOutline()
	{
		return outline;
	}

	HunllefStyle next()
	{
		return this == RANGE ? MAGIC : RANGE;
	}
}
