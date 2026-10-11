package net.cengiz1.uxmskyblock.proxy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.UUID;

public class ProxyMessage {

    public static final int PROTOCOL_VERSION = 1;

    private final MessageType type;
    private final String origin;
    private final String island;

    private ProxyMessage(MessageType type, String origin, String island) {
        this.type = type;
        this.origin = origin;
        this.island = island;
    }

    public static ProxyMessage island(MessageType type, String origin, UUID islandId) {
        return new ProxyMessage(type, origin, islandId == null ? null : islandId.toString());
    }

    public MessageType getType() {
        return type;
    }

    public String getOrigin() {
        return origin;
    }

    public UUID getIslandId() {
        if (island == null)
            return null;
        try {
            return UUID.fromString(island);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    public String serialize() {
        JsonObject object = new JsonObject();
        object.addProperty("v", PROTOCOL_VERSION);
        object.addProperty("type", type.name());
        object.addProperty("origin", origin);
        if (island != null)
            object.addProperty("island", island);
        return object.toString();
    }

    public static ProxyMessage parse(String raw) {
        try {
            JsonObject object = JsonParser.parseString(raw).getAsJsonObject();
            if (object.has("v") && object.get("v").getAsInt() > PROTOCOL_VERSION)
                return null;
            MessageType type = MessageType.fromString(object.has("type") ? object.get("type").getAsString() : null);
            if (type == null)
                return null;
            String origin = object.has("origin") ? object.get("origin").getAsString() : "";
            String island = object.has("island") ? object.get("island").getAsString() : null;
            return new ProxyMessage(type, origin, island);
        } catch (Throwable error) {
            return null;
        }
    }
}
