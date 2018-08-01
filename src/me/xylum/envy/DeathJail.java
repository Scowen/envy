package me.xylum.envy;

import me.vagdedes.mysql.database.SQL;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import me.vagdedes.mysql.database.MySQL;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Level;

import static org.bukkit.Bukkit.*;

public class DeathJail implements Listener {
    static final int DEFAULT_DEATH_TIME = 10;

    private HashMap<String, Jail> jails = new HashMap<>();
    private HashMap<String, JailedPlayer> jailedPlayers = new HashMap<>();
    private FileConfiguration config;

    ConsoleCommandSender console = getServer().getConsoleSender();

    DeathJail(FileConfiguration config) {
        this.config = config;

        if (MySQL.isConnected()) {
            // Create tables that don't exist.
            if (!SQL.tableExists("envy_jails")) {
                SQL.createTable("envy_jails",
                        "`name` VARCHAR(75)," +
                                "`world` VARCHAR(50)," +
                                "`x` DOUBLE," +
                                "`y` DOUBLE," +
                                "`z` DOUBLE");
            }

            if (!SQL.tableExists("envy_jail_times")) {
                SQL.createTable("envy_jail_times",
                        "`group` VARCHAR(75)," +
                                "`seconds` INT(11)");
            }

            if (!SQL.tableExists("envy_player_jails")) {
                SQL.createTable("envy_player_jails",
                        "`player_name` VARCHAR(75)," +
                                "`player_uuid` VARCHAR(36)," +
                                "`killer_name` VARCHAR(75)," +
                                "`killer_uuid` VARCHAR(36)," +
                                "`created` DOUBLE," +
                                "`expires` DOUBLE");
            }

            // Load the jails.
            try {
                ResultSet result = MySQL.query("SELECT * FROM `envy_jails`");
                while (result.next()) {
                    Jail jail = new Jail(
                            result.getString("name"),
                            result.getString("world"),
                            result.getDouble("x"),
                            result.getDouble("y"),
                            result.getDouble("z"));

                    jails.put(jail.name, jail);
                }
            } catch (SQLException sqlExc) {
                sqlExc.printStackTrace();
            }
            // Log that the jails have been loaded.
            getLogger().log(Level.INFO, Main.TAG + jails.size() + " on death jails loaded");

            // Load the jailed players.
            try {
                long unixTime = System.currentTimeMillis() / 1000L;
                ResultSet result = MySQL.query("SELECT * FROM `envy_player_jails` WHERE `expires` >= " + unixTime);
                while (result.next()) {
                    JailedPlayer jailedPlayer = new JailedPlayer(
                            result.getString("player_name"),
                            result.getString("player_uuid"),
                            result.getString("killer_name"),
                            result.getString("killer_uuid"),
                            result.getDouble("created"),
                            result.getDouble("expires"),
                            false
                    );

                    jailedPlayers.put(jailedPlayer.playerUuid, jailedPlayer);
                }
            } catch (SQLException sqlExc) {
                sqlExc.printStackTrace();
            }
            // Log that the jailed players have been loaded.
            getLogger().log(Level.INFO, Main.TAG + jailedPlayers.size() + " jailed players loaded");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e){
        Player player = e.getEntity().getPlayer();
        Player killer = e.getEntity().getKiller();

        if (player != null) {// && killer != null) {
            long unixTime = System.currentTimeMillis() / 1000L;
            long expiresTime = unixTime + DEFAULT_DEATH_TIME;

            JailedPlayer jailedPlayer = new JailedPlayer(
                    player.getName(),
                    player.getUniqueId().toString(),
                    "Killer", // killer.getName(),
                    "killer_uuid", // killer.getUniqueId().toString(),
                    unixTime,
                    expiresTime,
                    true
            );

            player.sendMessage(Main.colours(config.getString("messages.death")));

            jailedPlayers.put(jailedPlayer.playerUuid, jailedPlayer);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        UUID playerUuid = player.getUniqueId();
        long unixTime = System.currentTimeMillis() / 1000L;


    }

    private class Jail {
        String name;
        String world;
        double x;
        double y;
        double z;

        Jail(String name, String world, double x, double y, double z) {
            this.name = name;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private class JailedPlayer {
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

            double delayMilli = (expires - created) * 1000;

            class RemovePlayerFromJail extends TimerTask {
                public void run() {
                    jailedPlayers.remove(playerUuid);

                    // Try get the player by their UUID:
                    Player player = Bukkit.getPlayer(playerUuid);
                    // Otherwise try their name:
                    if (player == null) player = Bukkit.getPlayer(playerName);
                    // Teleport the player to spawn.
                    if (player != null) {
                        player.sendMessage(Main.colours(config.getString("messages.unjail")));
                        player.performCommand("spawn");
                    }

                    getLogger().log(Level.INFO, Main.TAG + playerName + " has been unjailed");
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
    }
}

