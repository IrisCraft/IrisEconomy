package de.iriscraft.irisEconomy.commands;

import de.iriscraft.irisEconomy.IrisEconomy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MoneyCommand implements CommandExecutor {

    private final IrisEconomy plugin;

    public MoneyCommand(IrisEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        OfflinePlayer targetPlayer;
        if (args.length == 0) {
            targetPlayer = player; // Check own money
        } else {
            targetPlayer = Bukkit.getOfflinePlayer(args[0]);
            if (targetPlayer == null) {
                player.sendMessage(plugin.getPrefix() + ("§cSpieler nicht gefunden!"));
                return true;
            }
        }

        // Database operation should be asynchronous
        OfflinePlayer finalTargetPlayer = targetPlayer; // For use in lambda
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().getBalance(finalTargetPlayer.getUniqueId()).thenAccept(balance -> {
                // Send message back on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (finalTargetPlayer.equals(player)) {
                        player.sendMessage(plugin.getPrefix() + (
                                "§7Dein §9Geld §7beträgt§8: §9" + String.format("%.2f", balance) + "€"
                        ));
                    } else {
                        player.sendMessage(plugin.getPrefix() + (
                                "§7Das §9Geld §7von §9" + finalTargetPlayer.getName() + " §7beträgt§8: §9" + String.format("%.2f", balance) + "€"
                        ));
                    }
                });
            }).exceptionally(e -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(plugin.getPrefix() + ("&cEin Fehler ist aufgetreten: " + e.getMessage()));
                });
                plugin.getLogger().severe("Error in MoneyCommand for " + finalTargetPlayer.getName() + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            });
        });

        return true;
    }
}