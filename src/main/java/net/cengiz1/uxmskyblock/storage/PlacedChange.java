package net.cengiz1.uxmskyblock.storage;

import java.util.UUID;

public record PlacedChange(UUID island, long key, String material, boolean add) {
}
