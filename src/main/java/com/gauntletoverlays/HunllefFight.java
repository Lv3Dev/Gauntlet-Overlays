package com.gauntletoverlays;

/**
 * Hunllef starts on Ranged. Every 4th counted attack switches style and the
 * displayed count returns to 0, so the counter never shows 4/4.
 */
final class HunllefFight
{
	static final int ATTACKS_PER_STYLE = 4;

	private HunllefStyle style = HunllefStyle.RANGE;
	private int attackCount;

	void reset()
	{
		style = HunllefStyle.RANGE;
		attackCount = 0;
	}

	void countAttack()
	{
		attackCount++;
		if (attackCount >= ATTACKS_PER_STYLE)
		{
			style = style.next();
			attackCount = 0;
		}
	}

	HunllefStyle getStyle()
	{
		return style;
	}

	int getAttackCount()
	{
		return attackCount;
	}

	String counterText()
	{
		return style.getLabel() + " " + attackCount + "/" + ATTACKS_PER_STYLE;
	}
}
