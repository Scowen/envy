package me.xylum.envy;

import me.vagdedes.mysql.basic.Config;
import me.vagdedes.mysql.database.MySQL;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Pattern;

public class Main extends JavaPlugin implements Listener {
    static final String TAG = "[Envy] ";
    static final String FTAG = "&2[&4Envy&2]&F ";

    static FileConfiguration sConfig;
    static DeathJail sDeathJail;
    static Permission perms = null;
    ConsoleCommandSender console = getServer().getConsoleSender();

    @Override
    public void onEnable() {
        sConfig = getConfig();

        sConfig.addDefault("mysql.host", "localhost");
        sConfig.addDefault("mysql.port", "3306");
        sConfig.addDefault("mysql.username", "root");
        sConfig.addDefault("mysql.password", "cjdann42");
        sConfig.addDefault("mysql.database", "envycraft");

        sConfig.addDefault("messages.jailed", "&FYou were killed by &4{killer_name}&F! Jail time remaining: &6{seconds_left}&F seconds");
        sConfig.addDefault("messages.unjail", "&AYou have been unjailed!");
        sConfig.options().copyDefaults(true);
        saveConfig();

        Config.setHost(sConfig.getString("mysql.host"));
        Config.setUser(sConfig.getString("mysql.username"));
        Config.setPassword(sConfig.getString("mysql.password"));
        Config.setDatabase(sConfig.getString("mysql.database"));
        Config.setPort(sConfig.getString("mysql.port"));

        if (!MySQL.isConnected())
            MySQL.connect();

        setupPermissions();

        sDeathJail = new DeathJail();
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(sDeathJail, this);

        Jail.load();
        JailTime.load();
        JailedPlayer.load();
        BlockedCommand.load();

        console.sendMessage(TAG + "EEEE N   N V     V Y   Y ");
        console.sendMessage(TAG + "E    NN  N V     V  Y Y  ");
        console.sendMessage(TAG + "EEE  N N N  V   V    Y   ");
        console.sendMessage(TAG + "E    N  NN   V V     Y   ");
        console.sendMessage(TAG + "EEEE N   N    V      Y   ");
        console.sendMessage(TAG + "Envy loaded successfully");
    }

    @Override
    public void onDisable(){
        if (MySQL.isConnected())
            MySQL.disconnect();
    }

    private boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            perms = permissionProvider.getProvider();
        }
        return (perms != null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent e){
        Player p = e.getPlayer();

        if (sDeathJail.jailedPlayers.containsKey(p.getUniqueId().toString())) {
            for (String command : sDeathJail.blockedCommands) {
                command = command.replaceAll(Pattern.quote("_"), " ");
                if (e.getMessage().startsWith("/" + command)) {
                    p.sendMessage(Utils.colours("&cYou may not use this command while in jail!"));
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("envy")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender instanceof Player) {
                        if (Main.perms.has((Player) sender, "envy.admin")  || sender.isOp()) {
                            this.reloadConfig();
                            sender.sendMessage(Utils.colours(FTAG + "Config reloaded."));
                            return true;
                        }
                    } else {
                        this.reloadConfig();
                        sender.sendMessage(TAG + "Config reloaded.");
                        return true;
                    }
                    return false;
                }

                if (args[0].equalsIgnoreCase("jail")) {
                    sDeathJail.onCommand(sender, cmd, commandLabel, args);
                    return true;
                }
            }

            if (sender instanceof Player) {
                sender.sendMessage(Utils.colours(FTAG + "&FAvailable commands:"));
                sender.sendMessage(Utils.colours("&4-&F reload &7- Reloads the config."));
                sender.sendMessage(Utils.colours("&4-&F jail &7- View the jail commands."));
            }
            return true;
        }
        return true;
    }
}
