package com.msgcustom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SocialSpyCommand implements CommandExecutor, TabCompleter {
    private final ConversationManager conversationManager;

    public SocialSpyCommand(ConversationManager conversationManager) {
        this.conversationManager = conversationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player admin = (Player) sender;

        if (!admin.hasPermission("msgcustom.socialspy")) {
            admin.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            Set<UUID> targets = conversationManager.getSpyTargets(admin.getUniqueId());

            if (targets.isEmpty()) {
                admin.sendMessage(Component.text("You are not spying on anyone.", NamedTextColor.YELLOW));
                return true;
            }

            admin.sendMessage(Component.text("You are currently spying on:", NamedTextColor.GREEN));

            for (UUID targetUUID : targets) {
                Player target = Bukkit.getPlayer(targetUUID);
                
                Component playerName = (target != null) 
                        ? target.displayName().colorIfAbsent(NamedTextColor.GRAY) 
                        : Component.text("Unknown (" + targetUUID + ")", NamedTextColor.GRAY);

                Component clickableName = Component.text("  • ", NamedTextColor.GRAY)
                        .append(playerName)
                        .hoverEvent(Component.text("Click to unspy", NamedTextColor.LIGHT_PURPLE))
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/socialspy " + (target != null ? target.getName() : targetUUID.toString())));

                admin.sendMessage(clickableName);
            }
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            admin.sendMessage(Component.text("Player '" + targetName + "' is not online!", NamedTextColor.RED));
            return true;
        }

        if (target.getUniqueId().equals(admin.getUniqueId())) {
            admin.sendMessage(Component.text("You cannot spy on yourself! Why?", NamedTextColor.RED));
            return true;
        }

        boolean nowSpying = conversationManager.toggleSpy(target.getUniqueId(), admin.getUniqueId());

        if (nowSpying) {
            admin.sendMessage(Component.text()
                    .append(Component.text("You are now spying on ", NamedTextColor.GREEN))
                    .append(target.displayName().colorIfAbsent(NamedTextColor.GRAY))
                    .build());
        } else {
            admin.sendMessage(Component.text()
                    .append(Component.text("You are no longer spying on ", NamedTextColor.YELLOW))
                    .append(target.displayName().colorIfAbsent(NamedTextColor.GRAY))
                    .build());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player admin = (Player) sender;
        if (!admin.hasPermission("msgcustom.socialspy")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            List<String> suggestions = new ArrayList<>();
            suggestions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList()));

            return suggestions;
        }

        return new ArrayList<>();
    }
}