package com.gauntletoverlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.Player;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class GauntletOverlaysOverlay extends Overlay
{
	private static final Color BACKDROP = new Color(0, 0, 0, 160);
	private static final Point CORNER = new Point(16, 40);

	private final Client client;
	private final GauntletOverlaysPlugin plugin;
	private final GauntletOverlaysConfig config;

	@Inject
	GauntletOverlaysOverlay(Client client, GauntletOverlaysPlugin plugin, GauntletOverlaysConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (graphics == null || client == null || plugin == null || config == null)
		{
			return null;
		}
		if (client.getGameState() != GameState.LOGGED_IN || plugin.getHunllef() == null)
		{
			return null;
		}
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		if (config.showCounter())
		{
			renderCounter(graphics);
		}
		if (config.highlightPrayer())
		{
			renderPrayer(graphics);
		}
		return null;
	}

	private void renderCounter(Graphics2D graphics)
	{
		NPC hunllef = plugin.getHunllef();
		String text = plugin.getCounterText();
		if (hunllef == null || text == null)
		{
			return;
		}

		Font base = graphics.getFont();
		graphics.setFont(base == null
			? new Font(Font.SANS_SERIF, Font.BOLD, 14)
			: base.deriveFont(Font.BOLD, 14f));

		Point point = hunllef.getCanvasTextLocation(graphics, text, hunllef.getLogicalHeight() + 40);
		if (point == null)
		{
			point = CORNER;
		}
		drawLabel(graphics, point, text, plugin.getStyle().getOutline());
	}

	private void drawLabel(Graphics2D graphics, Point point, String text, Color color)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		int textWidth = metrics.stringWidth(text);
		int top = point.getY() - metrics.getAscent() - 3;
		int height = metrics.getHeight() + 4;
		graphics.setColor(BACKDROP);
		graphics.fillRoundRect(point.getX() - 5, top, textWidth + 10, height, 8, 8);
		OverlayUtil.renderTextLocation(graphics, point, text, color);
	}

	private void renderPrayer(Graphics2D graphics)
	{
		// Hunllef is loaded for the whole gauntlet. This varbit is 1 only in the boss room.
		if (client.getVarbitValue(VarbitID.GAUNTLET_BOSS_STARTED) != 1)
		{
			return;
		}

		HunllefStyle style = plugin.getStyle();
		if (style == null)
		{
			return;
		}

		Widget widget = client.getWidget(style.getComponentId());
		if (widget == null || widget.isHidden())
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return;
		}

		graphics.setColor(style.getFill());
		graphics.fill(bounds);
		Stroke previous = graphics.getStroke();
		graphics.setStroke(new BasicStroke(2f));
		graphics.setColor(style.getOutline());
		graphics.draw(bounds);
		graphics.setStroke(previous);
	}
}
