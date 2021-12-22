package me.brunorm.skywars.API;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TitleBar {

    Integer fadeIn = 5; Integer stay = 5; Integer fadeOut = 5;
    String title = null; String subTitle = null;

    public TitleBar setFadeOut(Integer time) {
        try {
            if (time >= 0)
                this.fadeOut = time;
            else {
                this.fadeOut = 5;
                Bukkit.getServer().getLogger().warning("Sorry, but the number you have entered is less than 0. We have set it to the default setting.");
            }
            return this;
        } catch (NumberFormatException exception) {
            Bukkit.getServer().getLogger().warning("Sorry, the number you entered \"" + exception.getCause().toString() + "\" is an invalid number!");
            return null;
        }
    }

    public TitleBar setStay(Integer stay) {
        try {
            if (stay >= 0)
                this.stay = stay;
            else {
                this.stay = 5;
                Bukkit.getServer().getLogger().warning("Sorry, but the number you have entered is less than 0. We have set it to the default setting.");
            }
            return this;
        } catch (NumberFormatException exception) {
            Bukkit.getServer().getLogger().warning("Sorry, the number you entered \"" + exception.getCause().toString() + "\" is an invalid number!");
            return null;
        }
    }

    public TitleBar setFadeIn(Integer fadeIn) {
        try {
            if (fadeIn >= 0)
                this.fadeIn = fadeIn;
            else {
                this.fadeIn = 5;
                Bukkit.getServer().getLogger().warning("Sorry, but the number you have entered is less than 0. We have set it to the default setting.");
            }
            return this;
        } catch (NumberFormatException exception) {
            Bukkit.getServer().getLogger().warning("Sorry, the number you entered \"" + exception.getCause().toString() + "\" is an invalid number!");
            return null;
        }
    }

    public TitleBar setTitle(String title) {
        if (title != null)
            this.title = title;
        else {
            this.title = "Default Title";
            Bukkit.getServer().getLogger().warning("Sorry, but the title entered returned null. We have set it to the default setting.");
        }
        return this;
    }

    public TitleBar setSubTitle(String subTitle) {
        if (subTitle != null)
            this.subTitle = subTitle;
        else {
            this.subTitle = "Default SubTitle";
            Bukkit.getServer().getLogger().warning("Sorry, but the subtitle entered returned null. We have set it to the default setting.");
        }
        return this;
    }

    public void sendTitleToPlayer(Player player) {
        Packets packets = new Packets();
        try {
            Field connectionField = packets.getConnection(packets.getCraftPlayer(player));
            Object connection = connectionField.get(packets.getCraftPlayer(player));
            Class<?> titlePacket = packets.getNMS("PacketPlayOutTitle");
            Method send = connection.getClass ().getMethod ("sendPacket", packets.packetClass);
            if (this.title != null) {
                Object title = packets.getChatSerA().invoke(this.title);
                Constructor<?> titleC = titlePacket.getConstructor(titlePacket.getDeclaredClasses ()[0], packets.iChat, int.class, int.class, int.class);
                Object sendTitle = titleC.newInstance (titlePacket.getDeclaredClasses ()[0].getEnumConstants ()[0], title, this.fadeIn, this.stay, this.fadeOut);
                send.invoke(connection, sendTitle);
            }
            if (this.subTitle != null) {
                Object subTitle = packets.chatSerA.invoke (this.subTitle);
                Constructor<?> subTitleC = titlePacket.getConstructor(titlePacket.getDeclaredClasses()[0], packets.iChat);
                Object sendSubTitle = subTitleC.newInstance (titlePacket.getDeclaredClasses ()[0].getEnumConstants ()[1], subTitle);
                send.invoke(connection, sendSubTitle);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

}