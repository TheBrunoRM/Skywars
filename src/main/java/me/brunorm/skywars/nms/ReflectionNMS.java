package me.brunorm.skywars.nms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import me.brunorm.skywars.Messager;
import me.brunorm.skywars.Skywars;

public class ReflectionNMS implements NMS {

	String prefix = "&7[&cSkywars-NMS-Debug&7]";

	String version = Skywars.get().getServerPackageVersion();
	String nms = "net.minecraft.server." + this.version;

	Class<?> chatSerializer = this.getChatSerializer();
	Class<?> iChatBaseComponent = this.getIChatBaseComponent();
	Class<?> packetPlayOutTitle = this.getPacketPlayOutTitle();
	Class<?> packetPlayOutChat = this.getPacketPlayOutChat();
	Class<?> enumTitleAction = this.getEnumTitleAction();
	Class<?> craftPlayerClass = this.getCraftPlayerClass();
	Class<?> chatMessageTypeClass = this.getChatMessageTypeClass();

	// getters

	public Class<?> getChatSerializer() {
		try {
			Skywars.get().sendDebugMessageWithPrefix(this.prefix, "&6Loading &cold &bChatSerializer");
			return this.getNMS("ChatSerializer");
		} catch (final Exception e) {
			try {
				Skywars.get().sendDebugMessageWithPrefix(this.prefix, "&6Loading &anew &bChatSerializer");
				return this.getNMS("IChatBaseComponent$ChatSerializer");
			} catch (final Exception e2) {
				Skywars.get().sendDebugMessageWithPrefix(this.prefix, "&cCould not load &bChatSerializer");
				return null;
			}
		}
	}

	public Class<?> getIChatBaseComponent() {
		try {
			Skywars.get().sendDebugMessageWithPrefix(this.prefix, "&6Loading &bChatBaseComponent Interface");
			return this.getNMS("IChatBaseComponent");
		} catch (final Exception e) {
			Skywars.get().sendDebugMessageWithPrefix(this.prefix, "&cCould not load &bChatBaseComponent Interface");
			return null;
		}
	}

	public Class<?> getPacketPlayOutTitle() {
		try {
			Skywars.get().sendDebugMessageWithPrefix(this.prefix, "&6Loading &bPacketPlayOutTitle");
			return this.getNMS("PacketPlayOutTitle");
		} catch (final Exception e) {
			Skywars.get().sendDebugMessageWithPrefix(this.prefix, "&cCould not load &bPacketPlayOutTitle");
			return null;
		}
	}

	public Class<?> getPacketPlayOutChat() {
		try {
			Skywars.get().sendDebugMessageWithPrefix(this.prefix, "&6Loading &bPacketPlayOutChat");
			return this.getNMS("PacketPlayOutChat");
		} catch (final Exception e) {
			Skywars.get().sendDebugMessageWithPrefix(this.prefix, "&cCould not load &bPacketPlayOutChat");
			return null;
		}
	}

	public Class<?> getEnumTitleAction() {
		try {
			Skywars.get().sendDebugMessageWithPrefix(this.prefix, "&6Loading &cold &bEnumTitleAction");
			return this.getNMS("EnumTitleAction");
		} catch (final Exception e) {
			try {
				Skywars.get().sendDebugMessageWithPrefix(this.prefix, "&6Loading &anew &bEnumTitleAction");
				return this.getNMS("PacketPlayOutTitle$EnumTitleAction");
			} catch (final Exception e2) {
				Skywars.get().sendDebugMessageWithPrefix(this.prefix, "&cCould not load &bEnumTitleAction");
				return null;
			}
		}
	}

	public Class<?> getCraftPlayerClass() {
		try {
			return Class.forName("org.bukkit.craftbukkit." + this.version + ".entity.CraftPlayer");
		} catch (final ClassNotFoundException e) {
			return null;
		}
	}

	public Class<?> getChatMessageTypeClass() {
		try {
			Skywars.get().sendDebugMessageWithPrefix(this.prefix, "&6Loading &bChatMessageType");
			return this.getNMS("ChatMessageType");
		} catch (final Exception e) {
			Skywars.get().sendDebugMessageWithPrefix(this.prefix, "&cCould not find &bChatMessageType");
			return null;
		}
	}

	public Class<?> getNMS(String name) throws Exception {
		return Class.forName(this.nms + "." + name);
	}

	private Object getConnection(Player player) throws Exception {
		final Method getHandle = this.craftPlayerClass.getMethod("getHandle");
		// CraftPlayer.getHandle();

		final Object craftPlayerHandle = getHandle.invoke(player);
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
			final Object chat = a.invoke(this.chatSerializer, "{\"text\":\"" + Messager.color(text) + "\"}");

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

				final Object newInstanceOfThis = tc.getConstructor(String.class).newInstance(Messager.color(text));

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
			final Method m = player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class,
					int.class);
			m.invoke(player, Messager.color(title), Messager.color(subtitle), fadeIn, stay, fadeOut);
		} catch (final Exception e) {
			// 1.8
			try {

				final Method a = this.chatSerializer.getMethod("a", String.class);
				final Object chatTitle = a.invoke(this.chatSerializer, "{\"text\":\"" + Messager.color(title) + "\"}");
				final Object chatSubtitle = a.invoke(this.chatSerializer,
						"{\"text\":\"" + Messager.color(subtitle) + "\"}");

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
}
