package de.iriscraft.irisEconomy.api;

import de.iriscraft.irisEconomy.database.DatabaseManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EconomyAPI implements IEconomyAPI {

    private DatabaseManager databaseManager;

    public EconomyAPI(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Double> getBalance(UUID playerUuid) {
        return databaseManager.getBalance(playerUuid);
    }

    public CompletableFuture<Boolean> setBalance(UUID playerUuid, double amount) {
        return databaseManager.setBalance(playerUuid, amount);
    }

    public CompletableFuture<Boolean> addBalance(UUID playerUuid, double amount) {
        // addBalance im DatabaseManager gibt den neuen Balance-Wert zurück,
        // hier wollen wir nur den Erfolg der Operation als Boolean.
        return databaseManager.addBalance(playerUuid, amount).thenApply(newBalance -> true);
    }

    public CompletableFuture<Boolean> removeBalance(UUID playerUuid, double amount) {
        if (amount < 0) {
            // Eine negative Menge entfernen ist gleichbedeutend mit Hinzufügen einer positiven Menge
            return addBalance(playerUuid, Math.abs(amount));
        }
        // removeBalance ist im Wesentlichen ein addBalance mit negativem Betrag
        return databaseManager.addBalance(playerUuid, -amount).thenApply(newBalance -> true);
    }

}
