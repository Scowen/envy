package me.xylum.envy;

import me.vagdedes.mysql.basic.Config;
import me.vagdedes.mysql.database.MySQL;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    static final String TAG = "[Envy] ";
    static final String FTAG = "&2[&4Envy&2]&F ";

    static FileConfiguration sConfig;
    static DeathJail sDeathJail;

    @Override
    public void onEnable() {
        sConfig = getConfig();

        sConfig.addDefault("mysql.host", "localhost");
        sConfig.addDefault("mysql.port", "3306");
        sConfig.addDefault("mysql.username", "root");
        sConfig.addDefault("mysql.password", "cjdann42");
        sConfig.addDefault("mysql.database", "envycraft");

        sConfig.addDefault("messages.death", "&6You have been killed! Jail time remaining: {time_remaining}");
        sConfig.addDefault("messages.unjail", "&6You have been unjailed!");
        sConfig.options().copyDefaults(true);
        saveConfig();

        Config.setHost(sConfig.getString("mysql.host"));
        Config.setUser(sConfig.getString("mysql.username"));
        Config.setPassword(sConfig.getString("mysql.password"));
        Config.setDatabase(sConfig.getString("mysql.database"));
        Config.setPort(sConfig.getString("mysql.port"));

        if (!MySQL.isConnected())
            MySQL.connect();

        sDeathJail = new DeathJail();
        getServer().getPluginManager().registerEvents(sDeathJail, this);
    }

    @Override
    public void onDisable(){
        if (MySQL.isConnected())
            MySQL.disconnect();
    }

    static String colours(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("envy")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender instanceof Player) {
                        if (sender.hasPermission("envy.reload")) {
                            this.reloadConfig();
                            sender.sendMessage(colours(FTAG + "Config reloaded."));
                            return true;
                        }
                    } else {
                        this.reloadConfig();
                        sender.sendMessage(TAG + "Config reloaded.");
                        return true;
                    }
                    return false;
                }

                if (args[0].equalsIgnoreCase("jail"))
                    sDeathJail.onCommand(sender, cmd, commandLabel, args);
            } else {
                if (sender instanceof Player) {
                    sender.sendMessage(colours(FTAG + "&FAvailable commands:"));
                    sender.sendMessage(colours("&4-&F reload &7- Reloads the config."));
                    sender.sendMessage(colours("&4-&F jail &7- View the jail commands."));
                }
                return true;
            }
        }
        return false;
    }
}
