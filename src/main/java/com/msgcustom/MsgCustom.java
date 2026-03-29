package com.msgcustom;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MsgCustom extends JavaPlugin {
    private ConversationManager conversationManager;
    private WordFilter wordFilter;

    @Override
    public void onEnable() {
        wordFilter = new WordFilter();
        
        // Loading filter.txt
        File filterFile = new File(getDataFolder(), "filter.txt");
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Creating filter.txt 
        if (!filterFile.exists()) {
            try {
                filterFile.createNewFile();
                getLogger().info("Whoops couldn't find 'filter.txt' creating it shortly!");
            } catch (IOException e) {
                getLogger().severe("Couldn't create filter.txt: " + e.getMessage());
            }
        }
        
        wordFilter.loadFilterFile(filterFile);
        getLogger().info("Loaded " + wordFilter.getFilteredWordsCount() + " words and " + 
                        wordFilter.getFilteredPhrasesCount() + " phrases");
        
        conversationManager = new ConversationManager(wordFilter);

        getServer().getPluginManager().registerEvents(conversationManager, this);
        MessageCommand messageCommand = new MessageCommand(conversationManager);
        ReplyCommand replyCommand = new ReplyCommand(conversationManager);
        SocialSpyCommand socialSpyCommand = new SocialSpyCommand(conversationManager);

        // Register plugin.yml
        if (getCommand("msg") != null) {
            getCommand("msg").setExecutor(messageCommand);
            getCommand("msg").setTabCompleter(messageCommand); 
        }
        if (getCommand("reply") != null) {
            getCommand("reply").setExecutor(replyCommand);
            getCommand("reply").setTabCompleter(replyCommand); 
        }
        if (getCommand("socialspy") != null) {
            getCommand("socialspy").setExecutor(socialSpyCommand);
            getCommand("socialspy").setTabCompleter(socialSpyCommand);
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            cleanCommandRegistry();

            if (getCommand("msg") != null) getCommand("msg").setExecutor(messageCommand);
            if (getCommand("reply") != null) getCommand("reply").setExecutor(replyCommand);
            if (getCommand("socialspy") != null) getCommand("socialspy").setExecutor(socialSpyCommand);

            getLogger().info("Registry commands complete!");
        }, 1L);

        // 5-minute cleanup
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (conversationManager != null) {
                conversationManager.cleanupOfflinePlayers();
            }
        }, 6000L, 6000L);

        getLogger().info("Msgcustom has been enabled!");
    }

    private void cleanCommandRegistry() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get(Bukkit.getServer());

            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

            List<String> toRemove = Arrays.asList(
                    "minecraft:msg", "minecraft:message", "minecraft:w", 
                    "minecraft:whisper", "minecraft:tell", "minecraft:teammsg", "minecraft:tm",
                    "msgcustom:msg", "msgcustom:message", "msgcustom:w", 
                    "msgcustom:whisper", "msgcustom:tell", "msgcustom:reply", "msgcustom:r",
                    "msgcustom:socialspy"
            );

            for (String cmdLabel : toRemove) {
                if (knownCommands.containsKey(cmdLabel)) {
                    knownCommands.remove(cmdLabel);
                }
            }

            getLogger().info("Successfully removed " + toRemove.size() + " namespaced command entries.");
        } catch (Exception e) {
            getLogger().warning("Could not clean registry: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Msgcustom has been disabled!");
    }
}