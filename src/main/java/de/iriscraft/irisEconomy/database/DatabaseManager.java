package de.iriscraft.irisEconomy.database;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private Connection connection;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    public DatabaseManager(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    /**
     * Establishes a connection to the MySQL database.
     *
     * @throws SQLException If a database access error occurs.
     */
    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false";
        this.connection = DriverManager.getConnection(url, username, password);
    }

    /**
     * Closes the database connection.
     *
     * @throws SQLException If a database access error occurs.
     */
    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Creates the 'iriseco_balances' table if it does not exist.
     *
     * @throws SQLException If a database access error occurs.
     */
    public void createTable() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS iriseco_balances (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "balance DOUBLE DEFAULT 0.0" +
                    ");";
            statement.executeUpdate(sql);
        }
    }

    /**
     * Checks if a player's balance exists in the database.
     *
     * @param uuid The UUID of the player.
     * @return A CompletableFuture resolving to true if the player exists, false otherwise.
     */
    public CompletableFuture<Boolean> playerExists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM iriseco_balances WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                de.iriscraft.irisEconomy.IrisEconomy.getInstance().getLogger().severe("Error checking player existence: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Gets a player's balance from the database.
     *
     * @param uuid The UUID of the player.
     * @return A CompletableFuture resolving to the player's balance, or 0.0 if not found/error.
     */
    public CompletableFuture<Double> getBalance(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT balance FROM iriseco_balances WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            } catch (SQLException e) {
                de.iriscraft.irisEconomy.IrisEconomy.getInstance().getLogger().severe("Error getting balance for " + uuid + ": " + e.getMessage());
                e.printStackTrace();
            }
            return 0.0; // Default if not found or error
        });
    }

    /**
     * Sets a player's balance in the database. Inserts if not exists, updates otherwise.
     *
     * @param uuid    The UUID of the player.
     * @param balance The new balance.
     * @return A CompletableFuture resolving to true on success, false on failure.
     */
    public CompletableFuture<Boolean> setBalance(UUID uuid, double balance) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO iriseco_balances (uuid, balance) VALUES (?, ?) " +
                            "ON DUPLICATE KEY UPDATE balance = ?")) {
                ps.setString(1, uuid.toString());
                ps.setDouble(2, balance);
                ps.setDouble(3, balance);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                de.iriscraft.irisEconomy.IrisEconomy.getInstance().getLogger().severe("Error setting balance for " + uuid + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Adds an amount to a player's balance.
     *
     * @param uuid   The UUID of the player.
     * @param amount The amount to add (can be negative for subtraction).
     * @return A CompletableFuture resolving to the new balance, or the old balance on failure.
     */
    public CompletableFuture<Double> addBalance(UUID uuid, double amount) {
        return getBalance(uuid).thenCompose(currentBalance -> {
            double newBalance = currentBalance + amount;
            return setBalance(uuid, newBalance).thenApply(success -> success ? newBalance : currentBalance);
        });
    }
}