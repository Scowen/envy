package me.xylum.envy;

import org.bukkit.ChatColor;

import java.util.regex.Pattern;

public class Utils {
    static String colours(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    /**
     * Replace variables in a string with items from a Jail object.
     * @param string
     * @param jail
     * @return String
     */
    static String replaceVars(String string, Jail jail) {
        if (string == null) string = "";
        if (jail == null) return string;

        string = string.replace("{jail_name}", jail.name);
        string = string.replace("{jail_world}", jail.world);

        return string;
    }

    static String replaceVars(String string, JailTime jailTime) {
        if (string == null) string = "";
        if (jailTime == null) return string;

        string = string.replace("{group}", jailTime.group);
        string = string.replace("{seconds}", "" + jailTime.seconds);

        return string;
    }

    static String replaceVars(String string, JailedPlayer jailedPlayer) {
        if (string == null) string = "";
        if (jailedPlayer == null) return string;

        string = string.replace("{player_name}", jailedPlayer.playerName);
        string = string.replace("{killer_name}", jailedPlayer.killerName);
        string = string.replace("{seconds_left}", "" + jailedPlayer.secondsLeft());

        return string;
    }
}
