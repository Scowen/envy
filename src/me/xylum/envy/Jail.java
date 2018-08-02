package me.xylum.envy;

import me.vagdedes.mysql.database.MySQL;
import me.vagdedes.mysql.database.SQL;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getWorld;

public class Jail {
    public static final String TABLE = "envy_jails";

    String name;
    String world;
    double x;
    double y;
    double z;

    Jail(String name, String world, double x, double y, double z, boolean insert) {
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;

        if (insert) {
            SQL.insertData("name, world, x, y, z",
                    "'" + name + "'," +
                            "'" + world + "'," +
                            x + "," +
                            y + "," +
                            z,
                    TABLE
            );
        }
    }

    public static void load() {
        // Load the jails.
        try {
            Main.sDeathJail.jails = new HashMap<>();
            ResultSet result = MySQL.query("SELECT * FROM `" + TABLE + "`");
            while (result.next()) {
                Jail jail = new Jail(
                        result.getString("name"),
                        result.getString("world"),
                        result.getDouble("x"),
                        result.getDouble("y"),
                        result.getDouble("z"),
                        false
                );
                Main.sDeathJail.jails.put(jail.name, jail);
            }
        } catch (SQLException sqlExc) {
            sqlExc.printStackTrace();
        }
        // Log that the jails have been loaded.
        getLogger().log(Level.INFO, Main.TAG + Main.sDeathJail.jails.size() + " death jails loaded");
    }

    public void delete() {
        SQL.deleteData("name", "=",  name, TABLE);
        Main.sDeathJail.jails.remove(name);
    }

    public Location getLocation() {
        World world = getWorld(this.world);

        if (world == null)
            world = getWorld("world");

        if (world == null)
            return null;

        return new Location(world, x, y, z);
    }
}