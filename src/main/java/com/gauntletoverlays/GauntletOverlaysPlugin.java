package com.gauntletoverlays;

import com.google.inject.Provides;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Gauntlet Overlays",
	description = "Counts Hunllef attacks and highlights the overhead prayer to use",
	tags = {"gauntlet", "hunllef", "prayer"}
)
public class GauntletOverlaysPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private GauntletOverlaysOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	private final HunllefFight fight = new HunllefFight();
	private final Set<Projectile> seenProjectiles = Collections.newSetFromMap(new IdentityHashMap<Projectile, Boolean>());

	private NPC hunllef;
	private boolean fighting;
	private boolean pendingAttack;
	private int regionId = -1;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		findExistingHunllef();
		log.debug("Gauntlet Overlays started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		reset();
		log.debug("Gauntlet Overlays stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event == null || event.getGameState() != GameState.LOGGED_IN)
		{
			reset();
			return;
		}
		findExistingHunllef();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (event == null)
		{
			return;
		}
		trackIfHunllef(event.getNpc());
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		if (event == null)
		{
			return;
		}
		NPC npc = event.getNpc();
		if (npc == null)
		{
			return;
		}
		if (isHunllef(npc.getId()))
		{
			if (!fighting || hunllef == null || hunllef == npc)
			{
				trackIfHunllef(npc);
			}
			return;
		}
		if (npc == hunllef)
		{
			hunllef = null;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (event != null && hunllef != null && event.getNpc() == hunllef)
		{
			hunllef = null;
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		if (event == null || event.getProjectile() == null)
		{
			return;
		}
		Projectile projectile = event.getProjectile();
		int spotanimId = projectile.getId();
		// Impacts are a second graphic for the same attack and must not count.
		if (isImpactGraphic(spotanimId) || !isCountedTravelGraphic(spotanimId))
		{
			return;
		}
		// ProjectileMoved fires for every step. Count each travel graphic once.
		if (seenProjectiles.contains(projectile))
		{
			return;
		}
		Actor source = projectile.getSourceActor();
		if (source instanceof NPC)
		{
			trackIfHunllef((NPC) source);
			if (source != hunllef)
			{
				return;
			}
		}
		else if (hunllef == null)
		{
			findExistingHunllef();
		}
		if (hunllef == null)
		{
			return;
		}
		seenProjectiles.add(projectile);
		pendingAttack = true;
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (event == null)
		{
			return;
		}
		Actor actor = event.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}
		NPC npc = (NPC) actor;
		if (!isHunllef(npc.getId()))
		{
			return;
		}
		if (hunllef == null)
		{
			trackIfHunllef(npc);
		}
		if (npc != hunllef)
		{
			return;
		}
		// Tornado summon. The melee animation is the stomp and is not counted.
		if (npc.getAnimation() == AnimationID.HUNLLEF_ATTACK_SPECIAL)
		{
			pendingAttack = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client == null || client.getGameState() != GameState.LOGGED_IN)
		{
			pendingAttack = false;
			reset();
			return;
		}
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			pendingAttack = false;
			reset();
			return;
		}
		if (hunllef == null || hunllef.isDead())
		{
			pendingAttack = false;
			if (fighting)
			{
				reset();
			}
			return;
		}

		WorldPoint location = player.getWorldLocation();
		if (location == null)
		{
			pendingAttack = false;
			reset();
			return;
		}
		if (regionId == -1)
		{
			regionId = location.getRegionID();
		}
		else if (location.getRegionID() != regionId)
		{
			pendingAttack = false;
			reset();
			return;
		}

		if (pendingAttack && !isStomp())
		{
			fight.countAttack();
			log.debug("Hunllef attack -> {}", fight.counterText());
		}
		else if (pendingAttack)
		{
			log.debug("Ignored Hunllef stomp");
		}
		pendingAttack = false;
		seenProjectiles.removeIf(projectile -> projectile == null || projectile.getRemainingCycles() <= 0);
	}

	@Provides
	GauntletOverlaysConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GauntletOverlaysConfig.class);
	}

	NPC getHunllef()
	{
		return hunllef;
	}

	HunllefStyle getStyle()
	{
		return fight.getStyle();
	}

	String getCounterText()
	{
		return fight.counterText();
	}

	private void findExistingHunllef()
	{
		if (client == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null || worldView.npcs() == null)
		{
			return;
		}
		for (NPC npc : worldView.npcs())
		{
			if (npc != null && isHunllef(npc.getId()))
			{
				trackIfHunllef(npc);
				return;
			}
		}
	}

	private void trackIfHunllef(NPC npc)
	{
		if (npc == null || !isHunllef(npc.getId()))
		{
			return;
		}
		if (!fighting)
		{
			hunllef = npc;
			fighting = true;
			pendingAttack = false;
			fight.reset();
			regionId = currentRegionId();
			log.debug("Tracking Hunllef {}", npc.getId());
			return;
		}
		hunllef = npc;
	}

	private void reset()
	{
		boolean wasFighting = fighting || hunllef != null;
		hunllef = null;
		fighting = false;
		pendingAttack = false;
		regionId = -1;
		seenProjectiles.clear();
		fight.reset();
		if (wasFighting)
		{
			log.debug("Reset Hunllef attack counter");
		}
	}

	private int currentRegionId()
	{
		if (client == null)
		{
			return -1;
		}
		Player player = client.getLocalPlayer();
		if (player == null || player.getWorldLocation() == null)
		{
			return -1;
		}
		return player.getWorldLocation().getRegionID();
	}

	/**
	 * Hunllef plays {@link AnimationID#HUNLLEF_ATTACK_MELEE} to stomp when the player
	 * is standing under it. That attack does not advance the counter. Tornado summons
	 * use {@link AnimationID#HUNLLEF_ATTACK_SPECIAL} and still count.
	 */
	private boolean isStomp()
	{
		return hunllef != null && hunllef.getAnimation() == AnimationID.HUNLLEF_ATTACK_MELEE;
	}

	private static boolean isHunllef(int npcId)
	{
		switch (npcId)
		{
			case NpcID.CRYSTAL_HUNLLEF_MELEE:
			case NpcID.CRYSTAL_HUNLLEF_RANGED:
			case NpcID.CRYSTAL_HUNLLEF_MAGIC:
			case NpcID.CRYSTAL_HUNLLEF_DEATH:
			case NpcID.CRYSTAL_HUNLLEF_MELEE_HM:
			case NpcID.CRYSTAL_HUNLLEF_RANGED_HM:
			case NpcID.CRYSTAL_HUNLLEF_MAGIC_HM:
			case NpcID.CRYSTAL_HUNLLEF_DEATH_HM:
				return true;
			default:
				return false;
		}
	}

	private static boolean isCountedTravelGraphic(int spotanimId)
	{
		switch (spotanimId)
		{
			case SpotanimID.CRYSTAL_HUNLLEF_MAGIC_TRAVEL:
			case SpotanimID.CRYSTAL_HUNLLEF_MAGIC_TRAVEL_HM:
			case SpotanimID.CRYSTAL_HUNLLEF_RANGE_TRAVEL:
			case SpotanimID.CRYSTAL_HUNLLEF_RANGE_TRAVEL_HM:
			case SpotanimID.CRYSTAL_HUNLLEF_PRAYER_TRAVEL:
			case SpotanimID.CRYSTAL_HUNLLEF_PRAYER_TRAVEL_HM:
				return true;
			default:
				return false;
		}
	}

	private static boolean isImpactGraphic(int spotanimId)
	{
		switch (spotanimId)
		{
			case SpotanimID.CRYSTAL_HUNLLEF_MAGIC_IMPACT:
			case SpotanimID.CRYSTAL_HUNLLEF_MAGIC_IMPACT_HM:
			case SpotanimID.CRYSTAL_HUNLLEF_PRAYER_IMPACT:
			case SpotanimID.CRYSTAL_HUNLLEF_PRAYER_IMPACT_HM:
				return true;
			default:
				return false;
		}
	}
}
