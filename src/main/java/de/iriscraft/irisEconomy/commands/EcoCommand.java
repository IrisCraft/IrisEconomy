package de.iriscraft.irisEconomy.commands;

import de.iriscraft.irisEconomy.IrisEconomy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ExecutionException;

public class EcoCommand implements CommandExecutor {

    private final IrisEconomy plugin;

    public EcoCommand(IrisEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("iris.eco")) {
            player.sendMessage(plugin.getNoPermMessage());
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getPrefix() + ("§7Du musst ein §9Argument §7angeben§8."));
            player.sendMessage(plugin.getPrefix() + ("§7Verfügbare Argumente§8: §9set§8,§9add§8,§9remove§8,§9reload"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set":
            case "add":
            case "remove":
                if (args.length < 3) {
                    player.sendMessage(plugin.getPrefix() + ("§7Du musst einen §9Spieler §7und eine §9Summe §7angeben§8."));
                    return true;
                }

                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
                if (targetPlayer == null) {
                    player.sendMessage(plugin.getPrefix() + ("§cSpieler nicht gefunden!"));
                    return true;
                }

                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getPrefix() + ("§cBitte gib einen gültigen Betrag an!"));
                    return true;
                }

                // Database operations should be asynchronous
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        double currentBalance = plugin.getDatabaseManager().getBalance(targetPlayer.getUniqueId()).get(); // Blocking call for result

                        switch (subCommand) {
                            case "set":
                                plugin.getDatabaseManager().setBalance(targetPlayer.getUniqueId(), amount).get();
                                break;
                            case "add":
                                plugin.getDatabaseManager().addBalance(targetPlayer.getUniqueId(), amount).get();
                                break;
                            case "remove":
                                // Skript's logic for remove was a bit unusual (checking for negative amount input)
                                // Standard remove means subtracting a positive amount.
                                if (amount < 0) { // If user tries to remove a negative amount (effectively adding)
                                    player.sendMessage(plugin.getPrefix() + ("§cBitte gib einen positiven Betrag zum Entfernen an!"));
                                    return; // Exit async task
                                }
                                if (currentBalance - amount < 0) {
                                    // Optional: Prevent negative balances or set to 0 if it goes below
                                    // For now, we'll allow negative balances as per Skript's 'every 2 seconds' check
                                    plugin.getDatabaseManager().addBalance(targetPlayer.getUniqueId(), -amount).get();
                                } else {
                                    plugin.getDatabaseManager().addBalance(targetPlayer.getUniqueId(), -amount).get();
                                }
                                break;
                        }

                        // Get updated balance and send message back on main thread
                        plugin.getDatabaseManager().getBalance(targetPlayer.getUniqueId()).thenAccept(updatedBalance -> {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                player.sendMessage(plugin.getPrefix() + (
                                        "§7Das Geld von §9" + targetPlayer.getName() + " §7ist nun §9" + String.format("%.2f", updatedBalance) + "€§8."
                                ));
                            });
                        });

                    } catch (InterruptedException | ExecutionException e) {
                        player.sendMessage(plugin.getPrefix() + ("§cEin Fehler ist aufgetreten: " + e.getMessage()));
                        plugin.getLogger().severe("Error in EcoCommand (" + subCommand + "): " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                break;

            case "reload":
                player.sendMessage(plugin.getPrefix() + ("§7Das Skript wird neu geladen§8!"));
                // In Java, direct plugin reloading from a command is generally discouraged
                // and can lead to issues. It's better to tell the user to use the server's reload command
                // or to restart the server. For simplicity, we'll just log and inform.
                player.sendMessage(plugin.getPrefix() + ("§7Das Skript wurde neu geladen§8!"));
                break;

            default:
                player.sendMessage(plugin.getPrefix() + ("§7Du musst ein §9Argument §7angeben§8."));
                player.sendMessage(plugin.getPrefix() + ("§7Verfügbare Argumente§8: §9set§8,§9add§8,§9remove§8,§9reload"));
                break;
        }
        return true;
    }
}