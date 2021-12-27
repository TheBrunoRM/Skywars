package me.brunorm.skywars.NMS;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerUtilities {
	
    private Class<?> getNMSClass(String nmsClassString) throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        String name = "net.minecraft.server." + version + "." + nmsClassString;
        Class<?> nmsClass = Class.forName(name);
        return nmsClass;
    }
    
    private Object getConnection(Player player) throws SecurityException, NoSuchMethodException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method getHandle = player.getClass().getMethod("getHandle");
        Object nmsPlayer = getHandle.invoke(player);
        Field conField = nmsPlayer.getClass().getField("playerConnection");
        Object con = conField.get(nmsPlayer);
        return con;
    }
    
    public void sendParticles(Player player, String particle, float x, float y, float z, float xOffset, float yOffset, float zOffset, float data, int amount) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        Class<?> packetClass = this.getNMSClass("PacketPlayOutWorldParticles");
        Constructor<?> packetConstructor = packetClass.getConstructor(String.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, int.class);
        Object packet = packetConstructor.newInstance(particle, x, y, z, xOffset, yOffset, zOffset, data, amount);
        Method sendPacket = getNMSClass("PlayerConnection").getMethod("sendPacket", this.getNMSClass("Packet"));
        sendPacket.invoke(this.getConnection(player), packet);
    }
    
}
