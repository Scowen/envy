package me.xylum.envy;

import me.vagdedes.mysql.database.SQL;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
import java.util.*;
import java.util.logging.Level;

import static org.bukkit.Bukkit.*;

public class DeathJail implements Listener {
    static final int DEFAULT_DEATH_TIME = 10;

    public HashMap<String, Jail> jails;
    public HashMap<String, JailedPlayer> jailedPlayers;

    ConsoleCommandSender console = getServer().getConsoleSender();

    DeathJail() {
        jails = new HashMap<>();
        jailedPlayers = new HashMap<>();

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
                            result.getDouble("z"),
                            false
                    );
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

            player.sendMessage(Main.colours(Main.sConfig.getString("messages.death")));

            jailedPlayers.put(jailedPlayer.playerUuid, jailedPlayer);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        UUID playerUuid = player.getUniqueId();
        long unixTime = System.currentTimeMillis() / 1000L;


    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args[1].equalsIgnoreCase("create")) {
                if (sender.hasPermission("envy.jail.create")) {
                    if (args.length == 2) {
                        String jailName = args[1];

                        if (jails.containsKey(jailName))
                            jails.get(jailName).delete();

                        Jail jail = new Jail(
                                jailName,
                                player.getWorld().getName(),
                                player.getLocation().getX(),
                                player.getLocation().getY(),
                                player.getLocation().getZ(),
                                true
                        );
                        jails.put(jail.name, jail);
                        sender.sendMessage(Main.colours(Main.FTAG + "Jail &6" + jailName + "&F set &Asuccessfully&f."));
                    } else {
                        sender.sendMessage(Main.colours(Main.FTAG + "Invalid Syntax: &7/envy jail create <name>"));
                    }
                } else {
                    sender.sendMessage(Main.colours(Main.FTAG + "&cYou do not have the permission envy.jail.create"));
                }
                return true;
            }

            if (args[1].equalsIgnoreCase("delete")) {
                if (sender.hasPermission("envy.jail.delete")) {
                    if (args.length == 2) {
                        String jailName = args[1];

                        if (jails.containsKey(jailName)) {
                            jails.get(jailName).delete();

                            sender.sendMessage(Main.colours(Main.FTAG + "Jail &6" + jailName + "&F deleted &Asuccessfully&f."));
                        } else {
                            sender.sendMessage(Main.colours(Main.FTAG + "Jail &6" + jailName + "&c does not exist&f."));
                        }
                    } else {
                        sender.sendMessage(Main.colours(Main.FTAG + "Invalid Syntax: &7/envy jail delete <name>"));
                    }
                } else {
                    sender.sendMessage(Main.colours(Main.FTAG + "&cYou do not have the permission envy.jail.delete"));
                }
                return true;
            }

            if (args[1].equalsIgnoreCase("list")) {
                if (sender.hasPermission("envy.jail.list")) {
                    for (Map.Entry<String, Jail> entry : jails.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        // ...
                    }
                } else {
                    sender.sendMessage(Main.colours(Main.FTAG + "&cYou do not have the permission envy.jail.delete"));
                }
                return true;
            }

            sender.sendMessage(Main.colours(Main.FTAG + "&FAvailable &6Jail&F commands:"));
            sender.sendMessage(Main.colours("&4-&F create <name> &7- Creates jail with specified name."));
            sender.sendMessage(Main.colours("&4-&F delete <name> &7- Deleted jail with specified name."));
            sender.sendMessage(Main.colours("&4-&F list &7- Lists all jails."));
            sender.sendMessage(Main.colours("&4-&F player <player> <seconds> <jail name> &7- Jails player."));
            sender.sendMessage(Main.colours("&4-&F free <player> &7- Un-jails a player."));
        }
        return true;
    }
}

