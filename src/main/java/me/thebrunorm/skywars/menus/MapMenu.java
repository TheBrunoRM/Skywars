package me.thebrunorm.skywars.menus;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XMaterial;

import me.thebrunorm.skywars.Messager;
import me.thebrunorm.skywars.Skywars;
import me.thebrunorm.skywars.managers.ArenaManager;
import me.thebrunorm.skywars.structures.Arena;
import me.thebrunorm.skywars.structures.SkywarsMap;

public class MapMenu implements Listener {

	public static void open(Player player) {
		ArrayList<SkywarsMap> maps = Skywars.get().getMapManager().getMaps();
		
		// 检查是否只有一个已配置的地图
		if (maps.size() == 1) {
			// 如果只有一个地图，直接将玩家送入该游戏
			SkywarsMap singleMap = maps.get(0);
			if (!ArenaManager.joinMap(singleMap, player)) {
				player.sendMessage(Messager.get("CANT_JOIN_MAP"));
			}
			return;
		}
		
		// 如果有多个地图，显示地图选择菜单
		final Inventory inventory = Bukkit.createInventory(null, 9 * 6, Messager.color("&aMaps"));
		PlayerInventoryManager.setMenu(player, MenuType.MAP_SELECTION);

		int index = 10;
		for (final SkywarsMap map : maps) {
			// Skywars.get().sendDebugMessage("current index: " + index);
			if ((index + 1) % 9 == 0)
				index += 2;
			final List<String> lore = new ArrayList<String>();
			lore.clear();
			// lore.add(Messager.color("&7"));

			// lore.add(Messager.color("&8Solo Insane"));
			// lore.add(Messager.color("&7"));
			// lore.add(Messager.color("&7Servidores Disponibles: &a1"));
			// lore.add(Messager.color("&7Veces Unidas: &a0"));
			// lore.add(Messager.color("&7Selecciones de Mapa: &a1"));

			final ArrayList<Arena> arenas = ArenaManager.getArenasByMap(map);
			final int players = arenas.stream().map(arena -> arena.getAlivePlayerCount()).reduce(0, (a, b) -> a + b);

			lore.add(Messager.color("&eCurrent arenas: &a%s", ArenaManager.getArenasByMap(map).size()));
			lore.add(Messager.color("&eCurrent players: &a%s", players));
			lore.add(Messager.getMessage("CLICK_TO_PLAY"));
			// lore.add(Messager.color("&eClick derecho para alternarlo como favorito!"));

			final ItemStack item = new ItemStack(XMaterial.FIREWORK_STAR.parseItem());
			final ItemMeta meta = item.getItemMeta();
			meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);

			meta.setDisplayName(Messager.color("&a%s", map.getName()));
			meta.setLore(lore);
			item.setItemMeta(meta);
			inventory.setItem(index, item);
			index++;
		}

		player.openInventory(inventory);
	}

	@EventHandler
	void onClick(InventoryClickEvent event) {
		final Player player = (Player) event.getWhoClicked();
		final MenuType menu = PlayerInventoryManager.getCurrentMenu(player);
		if (menu != MenuType.MAP_SELECTION)
			return;
		event.setCancelled(true);
		final ItemStack clicked = event.getCurrentItem();
		if (clicked == null || clicked.getItemMeta() == null)
			return;
		final String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
		final SkywarsMap map = Skywars.get().getMapManager().getMap(name);
		if (!ArenaManager.joinMap(map, player))
			player.sendMessage(Messager.get("CANT_JOIN_MAP"));
	}

}
