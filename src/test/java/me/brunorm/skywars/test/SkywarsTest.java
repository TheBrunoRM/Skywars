package me.brunorm.skywars.test;

import org.junit.Before;
import org.junit.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import me.brunorm.skywars.Skywars;
import me.brunorm.skywars.managers.ArenaManager;
import me.brunorm.skywars.structures.Arena;
import me.brunorm.skywars.structures.SkywarsMap;

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
		final SkywarsMap map = Skywars.get().getRandomMap();
		final Arena arena = ArenaManager.getArenaAndCreateIfNotFound(map);
	}

}
