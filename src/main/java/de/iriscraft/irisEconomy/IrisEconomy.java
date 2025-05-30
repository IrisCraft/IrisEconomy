package de.iriscraft.irisEconomy;

import de.iriscraft.irisEconomy.api.IEconomyAPI;
import de.iriscraft.irisEconomy.commands.EcoCommand;
import de.iriscraft.irisEconomy.commands.MoneyCommand;
import de.iriscraft.irisEconomy.database.DatabaseManager;
import de.iriscraft.irisEconomy.listeners.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class IrisEconomy extends JavaPlugin {

    private static IrisEconomy instance;
    private DatabaseManager databaseManager;

    private BukkitTask balanceCheckTask;

    public static String prefix;
    public static String noPermMessage;

    private IEconomyAPI economyAPI; // <-- DIESE ZEILE FEHLT!

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // Create config.yml if it doesn't exist

        // Load messages from config
        this.prefix = getConfig().getString("options.prefix");
        this.noPermMessage = getConfig().getString("options.noperm");

        // Initialize Database
        try {
            this.databaseManager = new DatabaseManager(
                    getConfig().getString("database.host"),
                    getConfig().getInt("database.port"),
                    getConfig().getString("database.database"),
                    getConfig().getString("database.username"),
                    getConfig().getString("database.password")
            );
            this.databaseManager.connect();
            this.databaseManager.createTable();
            getLogger().info("Successfully connected to MySQL database.");
        } catch (SQLException e) {
            getLogger().severe("Could not connect to MySQL database! Disabling plugin.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register Commands
        getCommand("eco").setExecutor(new EcoCommand(this));
        getCommand("money").setExecutor(new MoneyCommand(this));

        // Register Events
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Start repeating task (every 2 seconds)
        startBalanceCheckTask();

        getLogger().info("IrisEconomy has been enabled!");
    }

    @Override
    public void onDisable() {
        // Stop repeating task
        if (balanceCheckTask != null) {
            balanceCheckTask.cancel();
        }

        // Close database connection
        if (databaseManager != null) {
            try {
                databaseManager.disconnect();
                getLogger().info("Disconnected from MySQL database.");
            } catch (SQLException e) {
                getLogger().severe("Error disconnecting from MySQL database: " + e.getMessage());
            }
        }
        getLogger().info("IrisEconomy has been disabled!");
    }

    /**
     * Starts the asynchronous task to check and reset negative balances.
     * Runs every 2 seconds.
     */
    private void startBalanceCheckTask() {
        balanceCheckTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            // This needs to iterate through all players in the database, not just online players.
            // For simplicity and performance, we'll only check online players here.
            // A more robust solution might involve a separate process or only checking on transactions.
            Bukkit.getOnlinePlayers().forEach(player -> {
                UUID uuid = player.getUniqueId();
                getDatabaseManager().getBalance(uuid).thenAccept(balance -> {
                    if (balance != null && balance < 0) {
                        getDatabaseManager().setBalance(uuid, 0.0);
                        getLogger().info("Reset negative balance for player " + player.getName() + " (" + uuid + ")");
                    }
                }).exceptionally(e -> {
                    getLogger().warning("Error checking balance for player " + player.getName() + ": " + e.getMessage());
                    return null;
                });
            });
        }, 0L, 2 * 20L); // 0L delay, 2 * 20L ticks (2 seconds)
    }

    // <-- DIESE METHODE FEHLT KOMPLETT! -->
    public IEconomyAPI getEconomyAPI() {
        return economyAPI;
    }

    public static IrisEconomy getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getNoPermMessage() {
        return noPermMessage;
    }
}