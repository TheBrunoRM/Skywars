/* (C) 2021 Bruno */
package me.thebrunorm.skywars.nms;

import me.thebrunorm.skywars.MessageUtils;
import me.thebrunorm.skywars.Skywars;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionNMS implements NMS {

	String prefix = "&7[&cSkywars-NMS-Debug&7]";

	String version = Skywars.get().getServerPackageVersion();
	String nms = "net.minecraft.server." + this.version;

	// classes

	Class<?> chatSerializer = this.getNMSSafe("ChatSerializer", "IChatBaseComponent$ChatSerializer");
	Class<?> iChatBaseComponent = this.getNMSSafe("IChatBaseComponent");
	Class<?> packetPlayOutTitle = this.getNMSSafe("PacketPlayOutTitle");
	Class<?> packetPlayOutChat = this.getNMSSafe("PacketPlayOutChat");
	Class<?> enumTitleAction = this.getNMSSafe("EnumTitleAction", "PacketPlayOutTitle$EnumTitleAction");
	Class<?> craftPlayerClass = this.getClassSafe("org.bukkit.craftbukkit." + this.version + ".entity.CraftPlayer");
	Class<?> chatMessageTypeClass = this.getNMSSafe("ChatMessageType");
	Class<?> packetPlayerListClass = this.getNMSSafe("PacketPlayOutPlayerListHeaderFooter");
	Class<?> packetClass = this.getNMSSafe("Packet");

	// methods

	Method sendPacketMethod = this.getMethodSafe(this.getNMSSafe("PlayerConnection"), "sendPacket", this.packetClass);
	Method chatSerializerA = this.getMethodSafe(this.chatSerializer, "a", String.class);
	Method sendTitleMethod = this.getMethodSafe(Player.class, "sendTitle", String.class, String.class, int.class,
			int.class, int.class);
	Method getHandleMethod = this.getMethodSafe(this.craftPlayerClass, "getHandle");
	Method setPlayerListMethod = this.getMethodSafe(Player.class, "setPlayerListHeaderFooter", String.class,
			String.class);

	public Method getMethodSafe(Class<?> c, String name, Class<?>... params) {
		try {
			return c.getMethod(name, params);
		} catch (final Exception e) {
			return null;
		}
	}

	public Class<?> getClassSafe(String name) {
		try {
			return Class.forName(name);
		} catch (final ClassNotFoundException e) {
			return null;
		}
	}

	public Class<?> getNMSSafe(String... names) {
		for (final String name : names) {
			try {
				return this.getNMS(name);
			} catch (final Exception e) {
				continue;
			}
		}
		return null;
	}

	public Class<?> getNMS(String name) throws Exception {
		return Class.forName(this.nms + "." + name);
	}

	private Object getConnection(Player player) throws Exception {
		// CraftPlayer.getHandle();

		final Object craftPlayerHandle = this.getHandleMethod.invoke(player);
		// (CraftPlayer) player.getHandle();

		final Field playerConnectionField = craftPlayerHandle.getClass().getField("playerConnection");
		final Object playerConnection = playerConnectionField.get(craftPlayerHandle);
		// (CraftPlayer) player.getHandle().playerConnection;

		return playerConnection;
	}

	// methods

	public void sendParticles(Location loc, String particle, int amount) {
		// TODO instead of a try-catch, check version
		final World world = loc.getWorld();
		try {
			// 1.13
			final Class<?> particleClass = Class.forName("org.bukkit.Particle");
			final Method spawnParticleMethod = world.getClass().getMethod("spawnParticle", particleClass, int.class);
			spawnParticleMethod.invoke(particleClass.getMethod("valueOf", String.class).invoke(particleClass, particle),
					loc, amount);
		} catch (final Exception e) {
			try {
				// 1.8
				final Method playEffectMethod = world.getClass().getMethod("playEffect", Location.class, Effect.class,
						int.class);
				playEffectMethod.invoke(loc.getWorld(), loc, Effect.valueOf(particle), amount);
			} catch (final Exception e2) {
				Skywars.get().sendDebugMessage("Could not spawn particles.");
			}
		}
	}

	public void sendParticles(Player player, String particle, int amount) {
		this.sendParticles(player, player.getLocation(), particle, amount);
	}

	@Override
	public void sendParticles(Player player, Location loc, String particle, int amount) {
		// TODO instead of a try-catch, check version
		try {
			// 1.13
			final Class<?> particleClass = Class.forName("org.bukkit.Particle");
			final Method spawnParticleMethod = player.getClass().getMethod("spawnParticle", particleClass, double.class,
					double.class, double.class, int.class);
			spawnParticleMethod.invoke(player,
					particleClass.getMethod("valueOf", String.class).invoke(particleClass, particle), loc.getX(),
					loc.getY(), loc.getZ(), amount);
		} catch (final Exception e) {
			try {
				// 1.8
				final Method playEffectMethod = player.getClass().getMethod("playEffect", Location.class, Effect.class,
						int.class);
				playEffectMethod.invoke(player, loc, Effect.valueOf(particle), amount);
			} catch (final Exception e2) {
				Skywars.get().sendDebugMessage("Could not spawn particles.");
			}
		}
	}

	@Override
	public void sendActionbar(Player player, String text) {
		// TODO instead of a try-catch, check version
		try {
			final Method a = this.chatSerializer.getMethod("a", String.class);
			final Object chat = a.invoke(this.chatSerializer, "{\"text\":\"" + MessageUtils.color(text) + "\"}");

			Constructor<?> packetPlayOutChatConstructor;
			Object packet;

			try {
				// 1.8
				packetPlayOutChatConstructor = this.packetPlayOutChat.getConstructor(this.iChatBaseComponent,
						byte.class);
				packet = packetPlayOutChatConstructor.newInstance(chat, (byte) 2);
			} catch (final Exception e) {
				// 1.13
				Method chatMessageTypeMethod;
				Object chatMessageTypeValue;
				packetPlayOutChatConstructor = this.packetPlayOutChat.getConstructor(this.iChatBaseComponent,
						this.chatMessageTypeClass);
				chatMessageTypeMethod = this.chatMessageTypeClass.getMethod("valueOf", String.class);
				chatMessageTypeValue = chatMessageTypeMethod.invoke(null, "GAME_INFO");
				packet = packetPlayOutChatConstructor.newInstance(chat, chatMessageTypeValue);
			}

			final Object playerConnection = this.getConnection(player);

			final Method sendPacket = playerConnection.getClass().getMethod("sendPacket", this.getNMS("Packet"));

			sendPacket.invoke(playerConnection, packet);
		} catch (final Exception e) {
			if (Bukkit.getServer().getVersion().toLowerCase().contains("bukkit"))
				return;
			try {
				// these classes are included in spigot
				// this doesn't work in bukkit
				final Class<?> cmt = Class.forName("net.md_5.bungee.api.ChatMessageType");
				final Class<?> tc = Class.forName("net.md_5.bungee.api.chat.TextComponent");
				final Class<?> bc = Class.forName("net.md_5.bungee.api.chat.BaseComponent");

				final Method m_s = player.getClass().getMethod("spigot");
				final Object cp = m_s.invoke(player);

				final Method m_sm = cp.getClass().getMethod("sendMessage", cmt, bc);

				final Object value_actionbar = cmt.getMethod("valueOf", String.class).invoke(cmt, "ACTION_BAR");

				final Object newInstanceOfThis = tc.getConstructor(String.class).newInstance(MessageUtils.color(text));

				m_sm.setAccessible(true); // make the method public
				m_sm.invoke(cp, cmt.cast(value_actionbar), bc.cast(newInstanceOfThis));
			} catch (final Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	public void sendTitle(Player player, String title) {
		this.sendTitle(player, title, "");
	}

	@Override
	public void sendTitle(Player player, String title, String subtitle) {
		this.sendTitle(player, title, subtitle, 10, 70, 20);
	}

	@Override
	public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		// TODO instead of a try-catch, check version
		try {
			// player.sendTitle();
			this.sendTitleMethod.invoke(player, MessageUtils.color(title), MessageUtils.color(subtitle), fadeIn, stay, fadeOut);
		} catch (final Exception e) {
			// 1.8
			try {

				final Method a = this.chatSerializer.getMethod("a", String.class);
				final Object chatTitle = a.invoke(this.chatSerializer, "{\"text\":\"" + MessageUtils.color(title) + "\"}");
				final Object chatSubtitle = a.invoke(this.chatSerializer,
					"{\"text\":\"" + MessageUtils.color(subtitle) + "\"}");

				final Object enumTitleActionTitle = this.enumTitleAction.getEnumConstants()[0];
				final Object enumTitleActionSubtitle = this.enumTitleAction.getEnumConstants()[1];

				final Constructor<?> packetPlayOutTitleConstructor = this.packetPlayOutTitle
						.getConstructor(this.enumTitleAction, this.iChatBaseComponent);
				final Constructor<?> packetPlayOutTitleLengthConstructor = this.packetPlayOutTitle
						.getConstructor(int.class, int.class, int.class);

				final Object titlePacket = packetPlayOutTitleConstructor.newInstance(enumTitleActionTitle, chatTitle);
				final Object subtitlePacket = packetPlayOutTitleConstructor.newInstance(enumTitleActionSubtitle,
						chatSubtitle);
				final Object lengthPacket = packetPlayOutTitleLengthConstructor.newInstance(fadeIn, stay, fadeOut);

				final Object playerConnection = this.getConnection(player);

				final Method sendPacket = playerConnection.getClass().getMethod("sendPacket", this.getNMS("Packet"));

				sendPacket.invoke(playerConnection, titlePacket);
				sendPacket.invoke(playerConnection, subtitlePacket);
				sendPacket.invoke(playerConnection, lengthPacket);
			} catch (final Exception e2) {
				// e2.printStackTrace();
			}
		}
	}

	public Object getSerializedChatComponent(Object o) {
		try {
			return this.chatSerializerA.invoke(this.chatSerializer, o);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void sendTablist(Player player, String header, String footer) {
		if (this.packetPlayerListClass != null) {
			try {
				final Object playerListPacket = this.packetPlayerListClass.getConstructor().newInstance();

				final Object headerComponent = this
					.getSerializedChatComponent("{\"text\":\"" + MessageUtils.color(header) + "\"}");
				final Object footerComponent = this
					.getSerializedChatComponent("{\"text\":\"" + MessageUtils.color(footer) + "\"}");
				final Field a = playerListPacket.getClass().getDeclaredField("a");
				a.setAccessible(true);
				a.set(playerListPacket, headerComponent);
				final Field b = playerListPacket.getClass().getDeclaredField("b");
				b.setAccessible(true);
				b.set(playerListPacket, footerComponent);
				this.sendPacket(player, playerListPacket);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			return;
		}
		try {
			this.setPlayerListMethod.invoke(player, header, footer);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void sendPacket(Player player, Object packet) {
		try {
			final Object playerConnection = this.getConnection(player);
			// final Method sendPacket = playerConnection.getClass().getMethod("sendPacket",
			// this.getNMS("Packet"));
			this.sendPacketMethod /* sendPacket */.invoke(playerConnection, packet);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
