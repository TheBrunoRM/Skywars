package me.brunorm.skywars.NMS;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;

public class NMSHandler {

	String version = Skywars.get().getServerPackageVersion();
	String nms = "net.minecraft.server." + version;

	Class<?> chatSerializer = getChatSerializer();
	Class<?> iChatBaseComponent = getIChatBaseComponent();
	Class<?> packetPlayOutTitle = getPacketPlayOutTitle();
	Class<?> packetPlayOutChat = getPacketPlayOutChat();
	Class<?> enumTitleAction = getEnumTitleAction();
	Class<?> craftPlayerClass = getCraftPlayerClass();

	public Class<?> getChatSerializer() {
		try {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color("&7[&cSkywars-Debug&7] &6Loading &cold &bChatSerializer"));
			System.out.println();
			return getNMS("ChatSerializer");
		} catch (Exception e) {
			try {
				Bukkit.getConsoleSender().sendMessage(
						Messager.color("&7[&cSkywars-Debug&7] &6Loading &anew &bChatSerializer"));
				return getNMS("IChatBaseComponent$ChatSerializer");
			} catch (Exception e2) {
				Bukkit.getConsoleSender().sendMessage(
						Messager.color("&7[&cSkywars-Debug&7] &cCould not load &bChatSerializer"));
				return null;
			}
		}
	}

	public Class<?> getIChatBaseComponent() {
		try {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color("&7[&cSkywars-Debug&7] &6Loading &bChatBaseComponent Interface"));
			return getNMS("IChatBaseComponent");
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color("&7[&cSkywars-Debug&7] &cCould not load &bChatBaseComponent Interface"));
			return null;
		}
	}

	public Class<?> getPacketPlayOutTitle() {
		try {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color("&7[&cSkywars-Debug&7] &6Loading &bPacketPlayOutTitle"));
			return getNMS("PacketPlayOutTitle");
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color("&7[&cSkywars-Debug&7] &cCould not load &bPacketPlayOutTitle"));
			return null;
		}
	}

	public Class<?> getPacketPlayOutChat() {
		try {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color("&7[&cSkywars-Debug&7] &6Loading &bPacketPlayOutChat"));
			return getNMS("PacketPlayOutChat");
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color("&7[&cSkywars-Debug&7] &cCould not load &bPacketPlayOutChat"));
			return null;
		}
	}

	public Class<?> getEnumTitleAction() {
		try {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color("&7[&cSkywars-Debug&7] &6Loading &cold &bEnumTitleAction"));
			return getNMS("EnumTitleAction");
		} catch (Exception e) {
			try {
				Bukkit.getConsoleSender().sendMessage(
						Messager.color("&7[&cSkywars-Debug&7] &6Loading &aold &bEnumTitleAction"));
				return getNMS("PacketPlayOutTitle$EnumTitleAction");
			} catch (Exception e2) {
				Bukkit.getConsoleSender().sendMessage(
						Messager.color("&7[&cSkywars-Debug&7] &cCould not load &bEnumTitleAction"));
				return null;
			}
		}
	}

	public Class<?> getCraftPlayerClass() {
		try {
			return Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public Class<?> getNMS(String name) throws Exception {
		return Class.forName(nms + "." + name);
	}

	public void sendActionbar(Player player, String title) {
		title = Messager.color(title);
		try {
			Method a = chatSerializer.getMethod("a", String.class);
			Object chat = a.invoke(chatSerializer, "{\"text\":\"" + title + "\"}");

			Constructor<?> packetPlayOutChatConstructor;
			try {
				// 1.8
				packetPlayOutChatConstructor = packetPlayOutChat.getConstructor(iChatBaseComponent, byte.class);
			} catch (Exception e) {
				// 1.13
				packetPlayOutChatConstructor = packetPlayOutChat.getConstructor(iChatBaseComponent);
			}

			Object packet;
			try {
				// 1.8
				packet = packetPlayOutChatConstructor.newInstance(chat, (byte) 2);
			} catch (Exception e) {
				// 1.13
				packet = packetPlayOutChatConstructor.newInstance(chat);
			}

			Method getHandle = craftPlayerClass.getMethod("getHandle");
			// CraftPlayer.getHandle();

			Object craftPlayerHandle = getHandle.invoke(player);
			// (CraftPlayer) player.getHandle();

			Field playerConnectionField = craftPlayerHandle.getClass().getField("playerConnection");
			Object playerConnection = playerConnectionField.get(craftPlayerHandle);
			// (CraftPlayer) player.getHandle().playerConnection;

			Class<?> packetClass = Class.forName(nms + ".Packet");
			Method sendPacket = playerConnectionField.getType().getMethod("sendPacket", packetClass);

			sendPacket.invoke(playerConnection, packet);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void sendTitle(Player player, String title) {
		sendTitle(player, title, "");
	}

	public void sendTitle(Player player, String title, String subtitle) {
		sendTitle(player, title, subtitle, 10, 70, 20);
	}

	public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		title = Messager.color(title);
		subtitle = Messager.color(subtitle);
		try {

			Method a = chatSerializer.getMethod("a", String.class);
			Object chatTitle = a.invoke(chatSerializer, "{\"text\":\"" + title + "\"}");
			Object chatSubtitle = a.invoke(chatSerializer, "{\"text\":\"" + subtitle + "\"}");
			// IChatBaseComponent chatTitle = ChatSerializer.a("{\"text\":\"" + "hola" +
			// "\"}");

			Object enumTitleActionTitle = enumTitleAction.getEnumConstants()[0];
			Object enumTitleActionSubtitle = enumTitleAction.getEnumConstants()[1];

			Constructor<?> packetPlayOutTitleConstructor = packetPlayOutTitle.getConstructor(enumTitleAction,
					iChatBaseComponent);
			Constructor<?> packetPlayOutTitleLengthConstructor = packetPlayOutTitle.getConstructor(int.class, int.class,
					int.class);

			Object titlePacket = packetPlayOutTitleConstructor.newInstance(enumTitleActionTitle, chatTitle);
			Object subtitlePacket = packetPlayOutTitleConstructor.newInstance(enumTitleActionSubtitle, chatSubtitle);
			Object lengthPacket = packetPlayOutTitleLengthConstructor.newInstance(fadeIn, stay, fadeOut);
			// PacketPlayOutTitle titlePacket = new
			// PacketPlayOutTitle(EnumTitleAction.TITLE, chatTitle);

			Method getHandle = craftPlayerClass.getMethod("getHandle");
			// CraftPlayer.getHandle();

			Object craftPlayerHandle = getHandle.invoke(player);
			// (CraftPlayer) player.getHandle();

			Field playerConnectionField = craftPlayerHandle.getClass().getField("playerConnection");
			Object playerConnection = playerConnectionField.get(craftPlayerHandle);
			// (CraftPlayer) player.getHandle().playerConnection;

			Class<?> packetClass = Class.forName(nms + ".Packet");
			Method sendPacket = playerConnectionField.getType().getMethod("sendPacket", packetClass);

			sendPacket.invoke(playerConnection, titlePacket);
			sendPacket.invoke(playerConnection, subtitlePacket);
			sendPacket.invoke(playerConnection, lengthPacket);
			// ((CraftPlayer) player).getHandle().playerConnection.sendPacket(titlePacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
