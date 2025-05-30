package de.iriscraft.irisEconomy.listeners;

import de.iriscraft.irisEconomy.IrisEconomy;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    private final IrisEconomy plugin;

    public PlayerListener(IrisEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Check if player exists in DB, if not, set balance to 0.0
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().playerExists(event.getPlayer().getUniqueId()).thenAccept(exists -> {
                if (!exists) {
                    plugin.getDatabaseManager().setBalance(event.getPlayer().getUniqueId(), 0.0);
                    plugin.getLogger().info("Initialized balance for new player: " + event.getPlayer().getName());
                }
            }).exceptionally(e -> {
                plugin.getLogger().warning("Error checking/setting balance for joining player " + event.getPlayer().getName() + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            });
        });
    }
}