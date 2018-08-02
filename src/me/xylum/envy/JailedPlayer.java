package me.xylum.envy;

import me.vagdedes.mysql.database.MySQL;
import me.vagdedes.mysql.database.SQL;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getOnlinePlayers;
import static org.bukkit.Bukkit.getPlayer;

public class JailedPlayer {
    public static final String TABLE = "envy_player_jails";

    String playerName;
    String playerUuid;
    String killerName;
    String killerUuid;
    Jail jail;
    double created;
    double expires;

    JailedPlayer(String playerName, String playerUuid, String killerName, String killerUuid, String jailName, double created, double expires, boolean insert) {
        if (!Main.sDeathJail.jails.containsKey(jailName))
            throw new IllegalArgumentException("Jail '" + jailName + "' does not exist.");

        Jail jail = Main.sDeathJail.jails.get(jailName);

        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.killerName = killerName;
        this.killerUuid = killerUuid;
        this.jail = jail;
        this.created = created;
        this.expires = expires;

        create(insert);
    }

    JailedPlayer(String playerName, String playerUuid, String killerName, String killerUuid, Jail jail, double created, double expires, boolean insert) {
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.killerName = killerName;
        this.killerUuid = killerUuid;
        this.jail = jail;
        this.created = created;
        this.expires = expires;

        create(insert);
    }

    public static void load() {
        // Load the jailed players.
        try {
            long unixTime = System.currentTimeMillis() / 1000L;
            Main.sDeathJail.jailedPlayers = new HashMap<>();

            ResultSet result = MySQL.query("SELECT * FROM `" + TABLE +"` WHERE `expires` >= " + unixTime);
            while (result.next()) {
                JailedPlayer jailedPlayer = new JailedPlayer(
                        result.getString("player_name"),
                        result.getString("player_uuid"),
                        result.getString("killer_name"),
                        result.getString("killer_uuid"),
                        result.getString("jail"),
                        result.getDouble("created"),
                        result.getDouble("expires"),
                        false
                );

                Main.sDeathJail.jailedPlayers.put(jailedPlayer.playerUuid, jailedPlayer);
            }
        } catch (SQLException sqlExc) {
            sqlExc.printStackTrace();
        }
        // Log that the jailed players have been loaded.
        getLogger().log(Level.INFO, Main.TAG + Main.sDeathJail.jailedPlayers.size() + " jailed players loaded");
    }

    public void create(boolean insert) {
        double delayMilli = secondsLeft() * 1000;

        class RemovePlayerFromJail extends TimerTask {
            public void run() {
                unJail();
            }
        }
        Timer timer = new Timer();
        timer.schedule(new RemovePlayerFromJail(), (long) delayMilli);

        if (insert) {
            SQL.insertData("player_name, player_uuid, killer_name, killer_uuid, jail, created, expires",
                    "'" + playerName + "'," +
                            "'" + playerUuid + "'," +
                            "'" + killerName + "'," +
                            "'" + killerUuid + "'," +
                            "'" + jail.name + "'," +
                            created + "," +
                            expires,
                    TABLE
            );
        }
    }

    public double secondsLeft() {
        return expires - created;
    }

    public Player getPlayer() {
        // Try get the player by their UUID:
        Player player = Bukkit.getPlayer(playerUuid);
        // Otherwise try their name:
        if (player == null) player = Bukkit.getPlayer(playerName);

        return player;
    }

    public void jail(Player player) {
        if (player == null)
            player = this.getPlayer();

        if (player == null)
            return;

        Location jailLocation = jail.getLocation();

        if (jailLocation == null) {
            for (Player onlinePlayer : getOnlinePlayers()) {
                if (Main.perms.has(onlinePlayer, "envy.admin"))
                    player.sendMessage(Utils.colours(Main.FTAG + "Failed to jail &6" + player.getName() + "&F, jail &c" + jail.name + "."));
            }
            return;
        }

        class PlacePlayerInJail extends TimerTask {
            private Player player;
            PlacePlayerInJail(Player player) { this.player = player; }
            public void run() {
                player.teleport(jailLocation);
            }
        }
        Timer timer = new Timer();
        timer.schedule(new PlacePlayerInJail(player), (long) 100);

        String message = Main.sConfig.getString("messages.jailed");
        message = Utils.replaceVars(message, jail);
        message = Utils.replaceVars(message, this);
        message = Utils.colours(message);
        if (message.length() < 1) message = "You have died and have been sent to jail!";
        player.sendMessage(message);
    }

    public void unJail() {
        Main.sDeathJail.jailedPlayers.remove(playerUuid);

        Player player = this.getPlayer();

        // Teleport the player to spawn.
        if (player != null) {
            String message = Main.sConfig.getString("messages.unjail");
            message = Utils.replaceVars(message, this);
            message = Utils.replaceVars(message, this.jail);
            message = Utils.colours(message);

            if (message.length() < 1) message = "You have been unjailed!";

            player.sendMessage(message);
            player.performCommand("spawn");
        }

        getLogger().log(Level.INFO, Main.TAG + playerName + " has been unjailed");
    }
}
