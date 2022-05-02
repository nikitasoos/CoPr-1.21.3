package net.coreprotect.database.statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import net.coreprotect.config.Config;
import net.coreprotect.config.ConfigHandler;
import net.coreprotect.database.Database;

public class UserStatement {

    private UserStatement() {
        throw new IllegalStateException("Database class");
    }

    public static int insert(Connection connection, String user) {
        int id = -1;

        try {
            int unixtimestamp = (int) (System.currentTimeMillis() / 1000L);
            String query = "INSERT INTO " + ConfigHandler.prefix + "user (time, `user`) VALUES (?, ?)";
            query = Database.setCorrectQueryFormat(query);
            PreparedStatement preparedStmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStmt.setInt(1, unixtimestamp);
            preparedStmt.setString(2, user);
            preparedStmt.executeUpdate();
            ResultSet keys = preparedStmt.getGeneratedKeys();
            keys.next();
            id = keys.getInt(1);
            keys.close();
            preparedStmt.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    public static int getId(PreparedStatement preparedStatement, String user, boolean load) throws SQLException {
        if (load && !ConfigHandler.playerIdCache.containsKey(user.toLowerCase(Locale.ROOT))) {
            UserStatement.loadId(preparedStatement.getConnection(), user, null);
        }

        return ConfigHandler.playerIdCache.get(user.toLowerCase(Locale.ROOT));
    }

    public static int loadId(Connection connection, String user, String uuid) {
        // generate if doesn't exist
        int id = -1;

        try {
            String collate = "";
            if (Config.getGlobal().TYPE_DATABASE.toLowerCase(Locale.ROOT).equals("sqlite")) {
                collate = " COLLATE NOCASE";
            }

            String where = "user = ?" + collate;
            if (uuid != null) {
                where = where + " OR uuid = ?";
            }

            String query = "SELECT rowid as id, uuid FROM " + ConfigHandler.prefix + "user WHERE " + where + " ORDER BY rowid ASC" + Database.getOffsetLimit(0, 1);
            PreparedStatement preparedStmt = connection.prepareStatement(query);
            preparedStmt.setString(1, user);

            if (uuid != null) {
                preparedStmt.setString(2, uuid);
            }

            ResultSet resultSet = preparedStmt.executeQuery();
            while (resultSet.next()) {
                id = resultSet.getInt("id");
                uuid = resultSet.getString("uuid");
            }
            resultSet.close();
            preparedStmt.close();

            if (id == -1) {
                id = insert(connection, user);
            }

            ConfigHandler.playerIdCache.put(user.toLowerCase(Locale.ROOT), id);
            ConfigHandler.playerIdCacheReversed.put(id, user);
            if (uuid != null) {
                ConfigHandler.uuidCache.put(user.toLowerCase(Locale.ROOT), uuid);
                ConfigHandler.uuidCacheReversed.put(uuid, user);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return id;
    }

    public static String loadName(Connection connection, int id) {
        // generate if doesn't exist
        String user = "";
        String uuid = null;

        try {
            Statement statement = connection.createStatement();
            String query = "SELECT `user`, uuid FROM " + ConfigHandler.prefix + "user WHERE rowid='" + id + "'" + Database.getOffsetLimit(0, 1);

            ResultSet resultSet = Database.sendQueryWithoutIndex(statement, query, "", true);
            while (resultSet.next()) {
                user = resultSet.getString("user");
                uuid = resultSet.getString("uuid");
            }

            if (user.length() == 0) {
                return user;
            }

            ConfigHandler.playerIdCache.put(user.toLowerCase(Locale.ROOT), id);
            ConfigHandler.playerIdCacheReversed.put(id, user);
            if (uuid != null) {
                ConfigHandler.uuidCache.put(user.toLowerCase(Locale.ROOT), uuid);
                ConfigHandler.uuidCacheReversed.put(uuid, user);
            }

            resultSet.close();
            statement.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return user;
    }
}
