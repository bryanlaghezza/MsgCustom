package com.msgcustom;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ConversationManager implements Listener {

    public static class ChatProfile {
        public volatile UUID lastMessaged = null;
        public volatile String lastMessageSent = null;
        public final AtomicLong lastMessageTimestamp = new AtomicLong(0);
        public final AtomicLong lastConversationTime = new AtomicLong(0);
        public final AtomicLong logoutTime = new AtomicLong(0);
    }

    private final Map<UUID, ChatProfile> playerProfiles = new ConcurrentHashMap<>();
    private final WordFilter wordFilter;

    private static final long MESSAGE_COOLDOWN_MS = 500;
    private static final long OFFLINE_GRACE_PERIOD_MS = 5 * 60 * 1000;
    private static final long CONVERSATION_EXPIRY_MS = 5 * 60 * 1000;

    public ConversationManager(WordFilter wordFilter) {
        this.wordFilter = wordFilter;
    }

    private ChatProfile getProfile(UUID uuid) {
        return playerProfiles.computeIfAbsent(uuid, k -> new ChatProfile());
    }

    public void recordConversation(Player sender, Player recipient) {
        long currentTime = System.currentTimeMillis();
        
        ChatProfile sProf = getProfile(sender.getUniqueId());
        ChatProfile rProf = getProfile(recipient.getUniqueId());

        sProf.lastMessaged = recipient.getUniqueId();
        sProf.lastConversationTime.set(currentTime);

        rProf.lastMessaged = sender.getUniqueId();
        rProf.lastConversationTime.set(currentTime);
        
    }

    public UUID getLastMessaged(UUID playerId) {
        ChatProfile profile = playerProfiles.get(playerId);
        return profile != null ? profile.lastMessaged : null;
    }

    public void clearConversation(UUID playerId) {
        ChatProfile profile = playerProfiles.get(playerId);
        if (profile != null) {
            profile.lastMessaged = null;
            profile.lastConversationTime.set(0);
        }
    }

    public boolean isDuplicateMessage(UUID playerId, String message) {
        ChatProfile profile = playerProfiles.get(playerId);
        return profile != null && java.util.Objects.equals(message, profile.lastMessageSent);
    }

    public void recordMessage(UUID playerId, String message) {
        getProfile(playerId).lastMessageSent = message;
    }

    public boolean isOnCooldown(UUID playerId) {
        ChatProfile profile = getProfile(playerId);
        
        while (true) {
            long lastTime = profile.lastMessageTimestamp.get();
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastTime < MESSAGE_COOLDOWN_MS) {
                return true; 
            }

            if (profile.lastMessageTimestamp.compareAndSet(lastTime, currentTime)) {
                return false; 
            }
        }
    }
    
    public boolean containsFilteredWord(String message) {
        return wordFilter.containsFilteredWord(message);
    }

    public void cleanupOfflinePlayers() {
        long now = System.currentTimeMillis();

        playerProfiles.entrySet().removeIf(entry -> {
            ChatProfile profile = entry.getValue();
            
            long logout = profile.logoutTime.get();
            long lastConv = profile.lastConversationTime.get();

            boolean offlineTooLong = logout > 0 && (now - logout > OFFLINE_GRACE_PERIOD_MS);
            boolean conversationExpired = lastConv > 0 && (now - lastConv > CONVERSATION_EXPIRY_MS);

            return offlineTooLong || conversationExpired;
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ChatProfile profile = playerProfiles.get(event.getPlayer().getUniqueId());
        if (profile != null) {
            profile.logoutTime.set(System.currentTimeMillis());
        }
    }
}