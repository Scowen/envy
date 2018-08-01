package me.xylum.envy;

import me.vagdedes.mysql.database.SQL;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

public class JailedPlayer {
    String playerName;
    String playerUuid;
    String killerName;
    String killerUuid;
    double created;
    double expires;

    JailedPlayer(String playerName, String playerUuid, String killerName, String killerUuid, double created, double expires, boolean insert) {
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.killerName = killerName;
        this.killerUuid = killerUuid;
        this.created = created;
        this.expires = expires;

        double delayMilli = secondsLeft() * 1000;

        class RemovePlayerFromJail extends TimerTask {
            public void run() {
                unJail();
            }
        }
        Timer timer = new Timer();
        timer.schedule(new RemovePlayerFromJail(), (long) delayMilli);

        if (insert) {
            SQL.insertData("player_name, player_uuid, killer_name, killer_uuid, created, expires",
                    "'" + playerName + "'," +
                            "'" + playerUuid + "'," +
                            "'" + killerName + "'," +
                            "'" + killerUuid + "'," +
                            created + "," +
                            expires,
                    "envy_player_jails"
            );
        }
    }

    public double secondsLeft() {
        return expires - created;
    }

    public void unJail() {
        Main.sDeathJail.jailedPlayers.remove(playerUuid);

        // Try get the player by their UUID:
        Player player = Bukkit.getPlayer(playerUuid);
        // Otherwise try their name:
        if (player == null) player = Bukkit.getPlayer(playerName);
        // Teleport the player to spawn.
        if (player != null) {
            player.sendMessage(Main.colours(Main.sConfig.getString("messages.unjail")));
            player.performCommand("spawn");
        }

        getLogger().log(Level.INFO, Main.TAG + playerName + " has been unjailed");
    }
}
