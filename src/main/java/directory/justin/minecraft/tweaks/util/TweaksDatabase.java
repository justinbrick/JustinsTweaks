package directory.justin.minecraft.tweaks.util;

import directory.justin.minecraft.tweaks.TweaksMod;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConnection;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TweaksDatabase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TweaksMod.class);

    private String path;
    // Create for a certain file.
    public TweaksDatabase(String fileName) {
        String relativePath = System.getProperty("user.dir");
        String configDirectory = Paths.get(relativePath, "config").toAbsolutePath().toString();
        File configDir = new File(configDirectory);
        if (!configDir.exists()) configDir.mkdir();
        path = Paths.get(relativePath, "config", fileName).toAbsolutePath().toString();
        try (SQLiteConnection conn = (SQLiteConnection) DriverManager.getConnection("jdbc:sqlite:" + path)) {
            if (conn == null) LOGGER.warn("Was unable to create a database at path: " + path);
            LOGGER.info("Created a new database at " + path);
        } catch (SQLException e) {
            LOGGER.error("There was an error trying to connect to database! Posting information...");
            LOGGER.error("Database path: " + path);
            LOGGER.error(e.toString());
        }
    }

    public void createPropertyInt(String name, int defaultValue) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path)) {
            try (var statement = conn.prepareStatement(String.format(
                    "CREATE TABLE IF NOT EXISTS %s (" +
                    "   key TEXT PRIMARY KEY," +
                    "   data INTEGER DEFAULT %d" +
                    ");", name, defaultValue))) {
                statement.execute();
            }
        } catch (SQLException e) {
            LOGGER.error("There was an error trying to create table! Posting information...");
            LOGGER.error("Database path: " + path);
            LOGGER.error(e.toString());
        }
    }

    public int getDataInt(ServerPlayerEntity p, String property) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path)) {
            String id = p.getUuidAsString();
            try (var statement = conn.prepareStatement(String.format(
                    "SELECT 1 FROM %s WHERE key = '%s'", property, id
            ))) {
                var set = statement.executeQuery();
                if (set.next())
                    return set.getInt("data");
                return Integer.MIN_VALUE;
            }
        } catch (SQLException e) {
            LOGGER.error("There was an error trying to fetch data from table! Posting information...");
            LOGGER.error("Database path: " + path);
            LOGGER.error(e.toString());
        }
        return Integer.MIN_VALUE;
    }

    public void setDataInt(ServerPlayerEntity p, String property, int value) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path)) {
            try (var statement = conn.prepareStatement(String.format(
                    "INSERT INTO %S (table, data) VALUES (%s, %d)", property, p.getUuidAsString(), value
            ))) {
                statement.execute();
            }
        } catch (SQLException e) {
            LOGGER.error("There was an error trying to set data into table! Posting information...");
            LOGGER.error("Database path: " + path);
            LOGGER.error(e.toString());
        }
    }
}

