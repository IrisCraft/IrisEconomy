package de.iriscraft.irisEconomy.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IEconomyAPI {
    CompletableFuture<Double> getBalance(UUID playerUuid);
    CompletableFuture<Boolean> setBalance(UUID playerUuid, double amount);
    CompletableFuture<Boolean> addBalance(UUID playerUuid, double amount);
    CompletableFuture<Boolean> removeBalance(UUID playerUuid, double amount);
}

