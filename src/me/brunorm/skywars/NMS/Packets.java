package me.brunorm.skywars.NMS;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Packets {

    private Class<?> craftPlayer;
    private Method craftHandle;
    public Class<?> iChat;
    private Class<?> chatSer;
    public Method chatSerA;
    public Class<?> packetClass;

    public String getServerVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().substring(23);
    }

    public Class<?> getNMS(String name) throws Exception {
        return Class.forName("net.minecraft.server." + getServerVersion() + "." + name);
    }

    public Packets() {
        try {
            this.craftPlayer = Class.forName("org.bukkit.craftbukkit." + getServerVersion() + ".entity.CraftPlayer");
            this.craftHandle = craftPlayer.getMethod ("getHandle");
            this.iChat = getNMS ("IChatBaseComponent");
            this.chatSer = iChat.getDeclaredClasses ()[0];
            this.chatSerA = chatSer.getMethod("a", String.class);
            this.packetClass = getNMS("Packet");
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }

    public Object getCraftPlayer(Player player) {
        try {
            return craftHandle.invoke(player);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public Field getConnection(Object craftPlayer) {
        try {
            return craftPlayer.getClass().getField("playerConnection");
        } catch (NoSuchFieldException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public Method getChatSerA() {
        return chatSerA;
    }

    public Class<?> getPacketClass() {
        return packetClass;
    }
}
 