package me.sase.nexusVault;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the command execution logic for the plugin.
 * Processes the main '/nv' command.
 */
public class VaultCommand implements CommandExecutor {

    private final NexusVault plugin;

    public VaultCommand(NexusVault plugin) {
        this.plugin = plugin;
    }

    /**
     * Intercepts command instructions issued by entities (Player or Console).
     * Secures the command so only active players can access GUI menus.
     *
     * @return true representing a successfully handled command syntax.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            plugin.getGuiManager().openVault(player);
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
    }
}