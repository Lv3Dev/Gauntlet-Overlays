package com.gauntletoverlays;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GauntletOverlaysPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GauntletOverlaysPlugin.class);
		RuneLite.main(args);
	}
}
