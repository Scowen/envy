package me.xylum.envy;

import me.vagdedes.mysql.database.MySQL;
import me.vagdedes.mysql.database.SQL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

public class JailTime {
    public static final String TABLE = "envy_jail_groups";

    String group;
    Jail jail;
    int seconds;


    public JailTime(String group, String jailName, int seconds, boolean insert) {
        if (!Main.sDeathJail.jails.containsKey(jailName))
            throw new IllegalArgumentException("Jail '" + jailName + "' does not exist.");

        Jail jail = Main.sDeathJail.jails.get(jailName);

        this.group = group.toLowerCase();
        this.jail = jail;
        this.seconds = seconds;

        create(insert);
    }

    public JailTime(String group, Jail jail, int seconds, boolean insert) {
        this.group = group.toLowerCase();
        this.jail = jail;
        this.seconds = seconds;

        create(insert);
    }

    public static void load() {
        // Load the jails.
        try {
            Main.sDeathJail.jailTimes = new HashMap<>();
            ResultSet result = MySQL.query("SELECT * FROM `" + TABLE + "`");
            while (result.next()) {
                JailTime jailTime = new JailTime(
                        result.getString("group"),
                        result.getString("jail"),
                        result.getInt("seconds"),
                        false
                );
                Main.sDeathJail.jailTimes.put(jailTime.group, jailTime);
            }
        } catch (SQLException sqlExc) {
            sqlExc.printStackTrace();
        }
        // Log that the jails have been loaded.
        getLogger().log(Level.INFO, Main.TAG + Main.sDeathJail.jailTimes.size() + " death jail groups loaded");
    }

    public void create(boolean insert) {
        if (insert) {
            SQL.insertData("`group`, `jail`, `seconds`",
                    "'" + group + "', " +
                            "'" + jail.name + "', " +
                            "'" + seconds + "'",
                    TABLE
            );
        }
    }

    public void delete() {
        SQL.deleteData("group", "=",  group, TABLE);
        Main.sDeathJail.jailTimes.remove(group);
    }
}
