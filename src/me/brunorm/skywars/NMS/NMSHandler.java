package me.brunorm.skywars.NMS;

import java.io.File;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;

public class NMSHandler {

	String prefix = "&7[&cSkywars-NMS-Debug&7] ";
	
	String version = Skywars.get().getServerPackageVersion();
	String nms = "net.minecraft.server." + version;

	Class<?> chatSerializer = getChatSerializer();
	Class<?> iChatBaseComponent = getIChatBaseComponent();
	Class<?> packetPlayOutTitle = getPacketPlayOutTitle();
	Class<?> packetPlayOutChat = getPacketPlayOutChat();
	Class<?> enumTitleAction = getEnumTitleAction();
	Class<?> craftPlayerClass = getCraftPlayerClass();
	Class<?> chatMessageTypeClass = getChatMessageTypeClass();
	
	public Class<?> getChatSerializer() {
		try {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color(prefix + "&6Loading &cold &bChatSerializer"));
			return getNMS("ChatSerializer");
		} catch (Exception e) {
			try {
				Bukkit.getConsoleSender().sendMessage(
						Messager.color(prefix + "&6Loading &anew &bChatSerializer"));
				return getNMS("IChatBaseComponent$ChatSerializer");
			} catch (Exception e2) {
				Bukkit.getConsoleSender().sendMessage(
						Messager.color(prefix + "&cCould not load &bChatSerializer"));
				return null;
			}
		}
	}
	
	public Class<?> getIChatBaseComponent() {
		try {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color(prefix + "&6Loading &bChatBaseComponent Interface"));
			return getNMS("IChatBaseComponent");
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color(prefix + "&cCould not load &bChatBaseComponent Interface"));
			return null;
		}
	}

	public Class<?> getPacketPlayOutTitle() {
		try {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color(prefix + "&6Loading &bPacketPlayOutTitle"));
			return getNMS("PacketPlayOutTitle");
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color(prefix + "&cCould not load &bPacketPlayOutTitle"));
			return null;
		}
	}

	public Class<?> getPacketPlayOutChat() {
		try {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color(prefix + "&6Loading &bPacketPlayOutChat"));
			return getNMS("PacketPlayOutChat");
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color(prefix + "&cCould not load &bPacketPlayOutChat"));
			return null;
		}
	}

	public Class<?> getEnumTitleAction() {
		try {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color(prefix + "&6Loading &cold &bEnumTitleAction"));
			return getNMS("EnumTitleAction");
		} catch (Exception e) {
			try {
				Bukkit.getConsoleSender().sendMessage(
						Messager.color(prefix + "&6Loading &anew &bEnumTitleAction"));
				return getNMS("PacketPlayOutTitle$EnumTitleAction");
			} catch (Exception e2) {
				Bukkit.getConsoleSender().sendMessage(
						Messager.color(prefix + "&cCould not load &bEnumTitleAction"));
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
	
	public Class<?> getChatMessageTypeClass() {
		try {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color(prefix + "&6Loading &bChatMessageType"));
			return getNMS("ChatMessageType");
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage(
					Messager.color(prefix + "&cCould not find &bChatMessageType"));
			return null;
		}
	}

	public Class<?> getNMS(String name) throws Exception {
		return Class.forName(nms + "." + name);
	}

    private Object getConnection(Player player) throws Exception{
		Method getHandle = craftPlayerClass.getMethod("getHandle");
		// CraftPlayer.getHandle();

		Object craftPlayerHandle = getHandle.invoke(player);
		// (CraftPlayer) player.getHandle();

		Field playerConnectionField = craftPlayerHandle.getClass().getField("playerConnection");
		Object playerConnection = playerConnectionField.get(craftPlayerHandle);
		// (CraftPlayer) player.getHandle().playerConnection;
		
        return playerConnection;
    }
    
    public void sendParticles(Player player, String particle, int amount) {
    	sendParticles(player, player.getLocation(), particle, amount);
    }
    
    public void sendParticles(Player player, Location loc, String particle, int amount) {
    	try {
			// 1.13+ particles
    		Class<?> particleClass = Class.forName("org.bukkit.Particle");
			Method spawnParticleMethod = player.getClass()
					.getMethod("spawnParticle", particleClass,
							double.class,double.class,double.class,int.class);
			spawnParticleMethod.invoke(player, particleClass
					.getMethod("valueOf", String.class)
					.invoke(particleClass, particle),
					loc.getX(), loc.getY(), loc.getZ(), 10);
		} catch(Exception e) {
			try {				
				// 1.8 - 1.12 particles
				Method playEffectMethod = player.getClass()
						.getMethod("playEffect", Location.class, Effect.class, int.class);
				playEffectMethod.invoke(player, loc, Effect.valueOf(particle), 10);
			} catch(Exception e2) {
				System.out.println("Could not spawn particles.");
			}
		}
    }
	
	public void sendActionbar(Player player, String text) {
		try {
			Method a = chatSerializer.getMethod("a", String.class);
			Object chat = a.invoke(chatSerializer, "{\"text\":\"" + Messager.color(text) + "\"}");

			Constructor<?> packetPlayOutChatConstructor;
			Object packet;
			
			try {
				// 1.8
				packetPlayOutChatConstructor = packetPlayOutChat.getConstructor(iChatBaseComponent, byte.class);
				packet = packetPlayOutChatConstructor.newInstance(chat, (byte) 2);
			} catch (Exception e) {
				// 1.13
				Method chatMessageTypeMethod;
				Object chatMessageTypeValue;
				packetPlayOutChatConstructor = packetPlayOutChat.getConstructor(iChatBaseComponent, chatMessageTypeClass);
                chatMessageTypeMethod = chatMessageTypeClass.getMethod("valueOf", String.class);
                chatMessageTypeValue = chatMessageTypeMethod.invoke(null, "GAME_INFO");
				packet = packetPlayOutChatConstructor.newInstance(chat, chatMessageTypeValue);
			}
			
			Object playerConnection = getConnection(player);
			
			Method sendPacket = playerConnection.getClass().getMethod("sendPacket", getNMS("Packet"));

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
		try {

			Method a = chatSerializer.getMethod("a", String.class);
			Object chatTitle = a.invoke(chatSerializer, "{\"text\":\"" + Messager.color(title) + "\"}");
			Object chatSubtitle = a.invoke(chatSerializer, "{\"text\":\"" + Messager.color(subtitle) + "\"}");
			
			Object enumTitleActionTitle = enumTitleAction.getEnumConstants()[0];
			Object enumTitleActionSubtitle = enumTitleAction.getEnumConstants()[1];

			Constructor<?> packetPlayOutTitleConstructor = packetPlayOutTitle.getConstructor(enumTitleAction,
					iChatBaseComponent);
			Constructor<?> packetPlayOutTitleLengthConstructor = packetPlayOutTitle.getConstructor(int.class, int.class,
					int.class);

			Object titlePacket = packetPlayOutTitleConstructor.newInstance(enumTitleActionTitle, chatTitle);
			Object subtitlePacket = packetPlayOutTitleConstructor.newInstance(enumTitleActionSubtitle, chatSubtitle);
			Object lengthPacket = packetPlayOutTitleLengthConstructor.newInstance(fadeIn, stay, fadeOut);

			Object playerConnection = getConnection(player);
			
			Method sendPacket = playerConnection.getClass().getMethod("sendPacket", getNMS("Packet"));

			sendPacket.invoke(playerConnection, titlePacket);
			sendPacket.invoke(playerConnection, subtitlePacket);
			sendPacket.invoke(playerConnection, lengthPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
