package com.gauntletoverlays;

import org.junit.Assert;
import org.junit.Test;

public class HunllefFightTest
{
	@Test
	public void startsOnRangeAtZero()
	{
		HunllefFight fight = new HunllefFight();
		Assert.assertEquals(HunllefStyle.RANGE, fight.getStyle());
		Assert.assertEquals("Range 0/4", fight.counterText());
	}

	@Test
	public void fourthAttackSwitchesStyleAndResetsCount()
	{
		HunllefFight fight = new HunllefFight();

		fight.countAttack();
		fight.countAttack();
		Assert.assertEquals("Range 2/4", fight.counterText());

		fight.countAttack();
		Assert.assertEquals("Range 3/4", fight.counterText());

		fight.countAttack();
		Assert.assertEquals(HunllefStyle.MAGIC, fight.getStyle());
		Assert.assertEquals("Magic 0/4", fight.counterText());

		fight.countAttack();
		fight.countAttack();
		fight.countAttack();
		fight.countAttack();
		Assert.assertEquals(HunllefStyle.RANGE, fight.getStyle());
		Assert.assertEquals("Range 0/4", fight.counterText());
	}

	@Test
	public void resetReturnsToRangeZero()
	{
		HunllefFight fight = new HunllefFight();
		fight.countAttack();
		fight.countAttack();
		fight.countAttack();
		fight.countAttack();
		fight.countAttack();
		fight.reset();
		Assert.assertEquals("Range 0/4", fight.counterText());
	}
}
