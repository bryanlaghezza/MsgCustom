package com.msgcustom;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConversationManager implements Listener {
    private final Map<UUID, UUID> lastMessaged = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessageSent = new ConcurrentHashMap<>();
    private final Map<UUID, Long> logoutTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Long> conversationTimestamps = new ConcurrentHashMap<>();

    private final Plugin plugin;
    private static final long OFFLINE_GRACE_PERIOD_MS = 5 * 60 * 1000; // 5 minutes
    private static final long CONVERSATION_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes

    public ConversationManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void recordConversation(Player sender, Player recipient) {
        long currentTime = System.currentTimeMillis();
        UUID senderUUID = sender.getUniqueId();
        UUID recipientUUID = recipient.getUniqueId();
        
        lastMessaged.put(senderUUID, recipientUUID);
        lastMessaged.put(recipientUUID, senderUUID);
        
        conversationTimestamps.put(senderUUID, currentTime);
        conversationTimestamps.put(recipientUUID, currentTime);
    }

    public UUID getLastMessaged(UUID playerId) {
        return lastMessaged.get(playerId);
    }

    public void clearConversation(UUID playerId) {
        lastMessaged.remove(playerId);
        conversationTimestamps.remove(playerId);
    }

    public boolean isDuplicateMessage(UUID playerId, String message) {
        String lastMsg = lastMessageSent.get(playerId);
        return message.equals(lastMsg);
    }

    public void recordMessage(UUID playerId, String message) {
        lastMessageSent.put(playerId, message);
    }

    public void cleanupOfflinePlayers() {
        long currentTime = System.currentTimeMillis();
        
        // Offline players cleanup
        logoutTimestamps.entrySet().removeIf(entry -> {
            UUID playerUUID = entry.getKey();
            long logoutTime = entry.getValue();
            
            if (currentTime - logoutTime > OFFLINE_GRACE_PERIOD_MS) {
                lastMessaged.remove(playerUUID);
                lastMessageSent.remove(playerUUID);
                conversationTimestamps.remove(playerUUID);
                return true;
            }
            return false;
        });
        
        // AFK conversations cleanup
        conversationTimestamps.entrySet().removeIf(entry -> {
            UUID playerUUID = entry.getKey();
            long lastConversationTime = entry.getValue();
            
            if (currentTime - lastConversationTime > CONVERSATION_EXPIRY_MS) {
                lastMessaged.remove(playerUUID);
                lastMessageSent.remove(playerUUID);
                return true;
            }
            return false;
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        logoutTimestamps.put(playerUUID, System.currentTimeMillis());
    }
}