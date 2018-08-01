package me.xylum.envy;

import me.vagdedes.mysql.database.MySQL;
import me.vagdedes.mysql.database.SQL;

public class Jail {
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
                    "envy_jails"
            );
        }
    }

    public void delete() {
        SQL.deleteData("name", "=",  name, "envy_jails");
        Main.sDeathJail.jails.remove(name);
    }
}