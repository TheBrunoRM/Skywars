package me.brunorm.skywars.test;

import org.junit.Before;
import org.junit.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import me.brunorm.skywars.Skywars;

@SuppressWarnings("unused")
public class SkywarsTest {

	private ServerMock server;
	private Skywars plugin;

	@Before
	public void setUp() {
		this.server = MockBukkit.mock();
		this.plugin = MockBukkit.load(Skywars.class);
	}

	@Test
	public void test() {
		final PlayerMock player = MockBukkit.getOrCreateMock().addPlayer();
	}

}
