package me.xylum.envy;

import me.vagdedes.mysql.database.MySQL;
import me.vagdedes.mysql.database.SQL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

public class BlockedCommand {
    public static final String TABLE = "envy_jail_blocked_commands";

    public static void create(String command) {
        SQL.insertData("command","'" + command + "'", TABLE);
    }

    public static void delete(String command) {
        SQL.deleteData("command", "=",  command, TABLE);
    }

    public static void load() {
        // Load the jails.
        try {
            Main.sDeathJail.blockedCommands = new ArrayList<>();
            ResultSet result = MySQL.query("SELECT * FROM `" + TABLE + "`");
            while (result.next()) {
                Main.sDeathJail.blockedCommands.add(result.getString("command"));
            }
        } catch (SQLException sqlExc) {
            sqlExc.printStackTrace();
        }
        // Log that the jails have been loaded.
        getLogger().log(Level.INFO, Main.TAG + Main.sDeathJail.blockedCommands.size() + " blocked commands loaded");
    }

}
