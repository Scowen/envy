package me.xylum.envy;

import me.vagdedes.mysql.basic.Config;
import me.vagdedes.mysql.database.MySQL;
import me.vagdedes.mysql.database.SQL;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements CommandExecutor {
    public static final String TAG = "[Envy] ";
    public static final String FTAG = "&2[&4Envy&2]&F ";

    FileConfiguration config = getConfig();
    private DeathJail deathJail;

    @Override
    public void onEnable() {
        config.addDefault("mysql.host", "localhost");
        config.addDefault("mysql.username", "root");
        config.addDefault("mysql.password", "cjdann42");
        config.addDefault("mysql.database", "envycraft");

        config.addDefault("messages.death", "&6You have been killed! Jail time remaining: {time_remaining}");
        config.addDefault("messages.unjail", "&6You have been unjailed!");
        config.options().copyDefaults(true);
        saveConfig();

        Config.setHost(config.getString("mysql.host"));
        Config.setUser(config.getString("mysql.username"));
        Config.setPassword(config.getString("mysql.password"));
        Config.setDatabase(config.getString("mysql.database"));
        Config.setPort("3306");

        if (!MySQL.isConnected())
            MySQL.connect();

        deathJail = new DeathJail(config);
        getServer().getPluginManager().registerEvents(deathJail, this);
    }

    @Override
    public void onDisable(){
        if (MySQL.isConnected())
            MySQL.disconnect();
    }

    public static String colours(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("envy")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender instanceof Player && sender.hasPermission("envy.reload")) {
                        this.reloadConfig();
                        sender.sendMessage(colours(FTAG + "Config reloaded."));
                        return true;
                    } else {
                        this.reloadConfig();
                        sender.sendMessage(TAG + "Config reloaded.");
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }
}
