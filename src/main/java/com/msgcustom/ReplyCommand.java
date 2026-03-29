package com.msgcustom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReplyCommand implements CommandExecutor, TabCompleter {
    private final ConversationManager conversationManager;
    private static final TextColor LIGHT_PURPLE = TextColor.fromHexString("#FF55FF");

    public ReplyCommand(ConversationManager conversationManager) {
        this.conversationManager = conversationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player senderPlayer = (Player) sender;

        if (conversationManager.isOnCooldown(senderPlayer.getUniqueId())) {
            senderPlayer.sendMessage(Component.text("Please wait 0.5s between messages!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /reply <message>", NamedTextColor.RED));
            return true;
        }

        UUID lastMessagedId = conversationManager.getLastMessaged(senderPlayer.getUniqueId());

        if (lastMessagedId == null) {
            sender.sendMessage(Component.text("No one has messaged in the past 5 minutes!", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(lastMessagedId);

        if (target == null || !senderPlayer.canSee(target)) {
            sender.sendMessage(Component.text("That player is not online!", NamedTextColor.RED));
            conversationManager.clearConversation(senderPlayer.getUniqueId());
            return true;
        }

        String message = String.join(" ", args);

        if (conversationManager.containsFilteredWord(message)) {
            sender.sendMessage(Component.text("This comment \"" + message + "\" has been blocked because it encourages behavior that is prohibited within our community policies. Please consider reviewing our rules book and trying again.", NamedTextColor.RED));
            return true;
        }

        if (conversationManager.isDuplicateMessage(senderPlayer.getUniqueId(), message)) {
            sender.sendMessage(Component.text("You cannot send the same message twice!", NamedTextColor.RED));
            return true;
        }

        conversationManager.recordConversation(senderPlayer, target);
        conversationManager.recordMessage(senderPlayer.getUniqueId(), message);

        Component senderDisplayName = senderPlayer.displayName();
        Component targetDisplayName = target.displayName();

        Component toMessage = Component.text()
                .append(Component.text("To ", LIGHT_PURPLE))
                .append(targetDisplayName.colorIfAbsent(NamedTextColor.GRAY))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.GRAY))
                .build();

        Component fromMessage = Component.text()
                .append(Component.text("From ", LIGHT_PURPLE))
                .append(senderDisplayName.colorIfAbsent(NamedTextColor.GRAY))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.GRAY))
                .build();

        senderPlayer.sendMessage(toMessage);
        target.sendMessage(fromMessage);
        target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        conversationManager.notifySpies(senderPlayer, target, message);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}