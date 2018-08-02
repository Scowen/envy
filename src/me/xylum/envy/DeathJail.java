package me.xylum.envy;

import me.vagdedes.mysql.database.SQL;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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
    static final int DEFAULT_DEATH_TIME = 60;

    public HashMap<String, Jail> jails;
    public HashMap<String, JailedPlayer> jailedPlayers;
    public HashMap<String, JailTime> jailTimes;
    public ArrayList<String> blockedCommands;

    DeathJail() {
        jails = new HashMap<>();
        jailedPlayers = new HashMap<>();
        jailTimes = new HashMap<>();
        blockedCommands = new ArrayList<>();

        if (MySQL.isConnected()) {
            // Create tables that don't exist.
            if (!SQL.tableExists(Jail.TABLE)) {
                SQL.createTable(Jail.TABLE,
                        "`name` VARCHAR(75)," +
                                "`world` VARCHAR(50)," +
                                "`x` DOUBLE," +
                                "`y` DOUBLE," +
                                "`z` DOUBLE");
            }

            if (!SQL.tableExists(JailTime.TABLE)) {
                SQL.createTable(JailTime.TABLE,
                        "`group` VARCHAR(75)," +
                                "`jail` VARCHAR(75)," +
                                "`seconds` INT(11)");
            }

            if (!SQL.tableExists(JailedPlayer.TABLE)) {
                SQL.createTable(JailedPlayer.TABLE,
                        "`player_name` VARCHAR(75)," +
                                "`player_uuid` VARCHAR(36)," +
                                "`killer_name` VARCHAR(75)," +
                                "`killer_uuid` VARCHAR(36)," +
                                "`jail` VARCHAR(75)," +
                                "`created` DOUBLE," +
                                "`expires` DOUBLE");
            }

            if (!SQL.tableExists(BlockedCommand.TABLE)) {
                SQL.createTable(BlockedCommand.TABLE,
                        "`command` VARCHAR(75)");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e){
        Player player = e.getEntity().getPlayer();
        Player killer = e.getEntity().getKiller();

        if (player != null) {// && killer != null) {
            long unixTime = System.currentTimeMillis() / 1000L;

            // Get the time that the user needs to be jailed for.
            String group = Main.perms.getPrimaryGroup(player);

            JailTime jailTime = null;
            // First try the users primary group.
            if (Main.sDeathJail.jailTimes.containsKey(group))
                jailTime = Main.sDeathJail.jailTimes.get(group);

            // If the primary group jail doesn't exist, try the secondary's.
            if (jailTime == null) {
                String[] groups = Main.perms.getPlayerGroups(player);

                for (String secondary : groups) {
                    if (jailTime == null && Main.sDeathJail.jailTimes.containsKey(secondary))
                        jailTime = Main.sDeathJail.jailTimes.get(secondary);
                }
            }

            if (jailTime != null) {
                long expiresTime = unixTime + jailTime.seconds;

                JailedPlayer jailedPlayer = new JailedPlayer(
                        player.getName(),
                        player.getUniqueId().toString(),
                        "Killer", // killer.getName(),
                        "killer_uuid", // killer.getUniqueId().toString(),
                        jailTime.jail,
                        unixTime,
                        expiresTime,
                        true
                );

                jailedPlayers.put(jailedPlayer.playerUuid, jailedPlayer);
            } else {
                String primary = Main.perms.getPrimaryGroup(player);

                for (Player onlinePlayer : getOnlinePlayers()) {
                    if (Main.perms.has(onlinePlayer, "envy.admin"))
                        player.sendMessage(Utils.colours(Main.FTAG + "Player &6" + player.getName() + "&F with group &c" + primary + "&f skipped jail"));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        UUID playerUuid = player.getUniqueId();
        long unixTime = System.currentTimeMillis() / 1000L;

        System.out.println(player.getName() + " respawned");

        if (!Main.sDeathJail.jailedPlayers.containsKey(playerUuid.toString()))
            return;

        System.out.println(player.getName() + " respawned and exists in jailedPlayers");

        Main.sDeathJail.jailedPlayers.get(playerUuid.toString()).jail(player);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        // /cmd  a0   a1     a2     a3  a4
        //length 1    2      3      4   5
        // /envy jail player Scowen 200 prison
        // /envy jail create testtt

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length >= 2 && args[1].equalsIgnoreCase("create")) {
                if (!Main.perms.has(player, "envy.jail.create")) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "&cYou do not have the permission envy.jail.create"));
                    return true;
                }

                if (args.length != 3) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "Invalid Syntax: &7/envy jail create <name>"));
                    return true;
                }

                String jailName = args[2];
                if (jailName.length() < 3) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "Jail name is too short (min 3 char)"));
                    return true;
                }

                if (jails.containsKey(jailName)) {
                    // Delete the jail if it exists already.
                    jails.get(jailName).delete();
                }

                // Now create the new jail.
                Jail jail = new Jail(
                        jailName,
                        player.getWorld().getName(),
                        player.getLocation().getX(),
                        player.getLocation().getY(),
                        player.getLocation().getZ(),
                        true
                );

                jails.put(jail.name, jail);
                sender.sendMessage(Utils.colours(Main.FTAG + "Jail &6" + jailName + "&F set &Asuccessfully&f."));
                return true;
            }

            if (args.length >= 2 && args[1].equalsIgnoreCase("delete")) {
                if (!Main.perms.has(player, "envy.jail.delete")) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "&cYou do not have the permission envy.jail.delete"));
                    return true;
                }

                if (args.length != 3) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "Invalid Syntax: &7/envy jail delete <name>"));
                    return true;
                }

                String jailName = args[2];
                if (jailName.length() < 1) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "You must include the name of the jail"));
                    return true;
                }

                if (!jails.containsKey(jailName)) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "Jail " + jailName + " does not exist"));
                    return true;
                }

                jails.get(jailName).delete();

                sender.sendMessage(Utils.colours(Main.FTAG + "Jail &6" + jailName + "&F deleted &Asuccessfully&f."));
                return true;
            }

            if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                if (!Main.perms.has(player, "envy.jail.list")) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "&cYou do not have the permission envy.jail.delete"));
                    return true;
                }

                sender.sendMessage(Utils.colours(Main.FTAG + "List of jails:"));
                sender.sendMessage(Utils.colours("&4-&F NAME &4-&F WORLD"));

                for (Map.Entry<String, Jail> entry : jails.entrySet()) {
                    String key = entry.getKey();
                    Jail value = entry.getValue();
                    sender.sendMessage(Utils.colours("&4-&F " + value.name + " &4-&F " + value.world));
                }
                return true;
            }

            if (args.length >= 2 && args[1].equalsIgnoreCase("block")) {
                if (!Main.perms.has(player, "envy.jail.commands.block")) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "&cYou do not have the permission envy.jail.commands.block"));
                    return true;
                }

                if (args.length != 3) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "Invalid Syntax: &7/envy jail block <command>"));
                    return true;
                }

                String command = args[2];
                if (command.length() < 1) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "You must include the command"));
                    return true;
                }

                if (blockedCommands.contains(command)) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "Command " + command + " is already blocked"));
                    return true;
                }

                blockedCommands.add(command);
                BlockedCommand.create(command);

                sender.sendMessage(Utils.colours(Main.FTAG + "Command &6" + command + "&F un-blocked &Asuccessfully&f."));
                return true;
            }

            if (args.length >= 2 && args[1].equalsIgnoreCase("unblock")) {
                if (!Main.perms.has(player, "envy.jail.commands.unblock")) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "&cYou do not have the permission envy.jail.commands.unblock"));
                    return true;
                }

                if (args.length != 3) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "Invalid Syntax: &7/envy jail unblock <command>"));
                    return true;
                }

                String command = args[2];
                if (command.length() < 1) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "You must include the command"));
                    return true;
                }

                if (!blockedCommands.contains(command)) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "Command " + command + " is not blocked"));
                    return true;
                }

                blockedCommands.remove(command);
                BlockedCommand.delete(command);

                sender.sendMessage(Utils.colours(Main.FTAG + "Command &6" + command + "&F un-blocked &Asuccessfully&f."));
                return true;
            }

            if (args.length >= 2 && args[1].equalsIgnoreCase("commands")) {
                if (!Main.perms.has(player, "envy.jail.commands.list")) {
                    sender.sendMessage(Utils.colours(Main.FTAG + "&cYou do not have the permission envy.jail.commands.list"));
                    return true;
                }

                sender.sendMessage(Utils.colours(Main.FTAG + "List of blocked commands:"));
                for (String command : blockedCommands) {
                    sender.sendMessage(Utils.colours("&4-&F " + command));
                }
                return true;
            }

            sender.sendMessage(Utils.colours(Main.FTAG + "&FAvailable &6Jail&F commands:"));
            sender.sendMessage(Utils.colours("&4-&F create <name> &7- Creates jail with specified name."));
            sender.sendMessage(Utils.colours("&4-&F delete <name> &7- Deleted jail with specified name."));
            sender.sendMessage(Utils.colours("&4-&F list &7- Lists all jails."));
            //sender.sendMessage(Utils.colours("&4-&F group <jail> <seconds> &7- Assigns group to jail."));
            sender.sendMessage(Utils.colours("&4-&F block <command> &7- Blocks command while in jail."));
            sender.sendMessage(Utils.colours("&4-&F unblock <command> &7- Unblocks the command."));
            sender.sendMessage(Utils.colours("&4-&F commands &7- Lists all blocked commands."));
            //sender.sendMessage(Utils.colours("&4-&F player <player> <seconds> <jail name> &7- Jails player."));
            //sender.sendMessage(Utils.colours("&4-&F free <player> &7- Un-jails a player."));
        }
        return true;
    }
}

