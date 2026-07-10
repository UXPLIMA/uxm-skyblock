package net.cengiz1.uxmskyblock.storage;

import java.util.UUID;

public final class BankLogEntry {

    private final long timestamp;
    private final String type;
    private final UUID actor;
    private final String actorName;
    private final double amount;
    private final double balance;

    public BankLogEntry(long timestamp, String type, UUID actor, String actorName, double amount, double balance) {
        this.timestamp = timestamp;
        this.type = type;
        this.actor = actor;
        this.actorName = actorName;
        this.amount = amount;
        this.balance = balance;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public UUID getActor() {
        return actor;
    }

    public String getActorName() {
        return actorName;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalance() {
        return balance;
    }
}
