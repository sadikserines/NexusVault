package me.sase.nexusVault;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listens to physical interactions with the Nexus Box block.
 * Handles block placement, breaking, and right-click interactions while ensuring
 * compatibility with GriefPrevention claim protections.
 */
public class BlockListener implements Listener {

    private final NexusVault plugin;

    public BlockListener(NexusVault plugin) {
        this.plugin = plugin;
    }

    /**
     * Triggered when a player places a block.
     * Checks if the placed block is a Nexus Box and registers it to the database if the player has building permissions.
     *
     * @param event The BlockPlaceEvent triggered by Bukkit.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        ItemStack itemInHand = event.getItemInHand();
        Player player = event.getPlayer();

        if (itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasDisplayName()) {
            String expectedName = ChatColor.GOLD + "" + ChatColor.BOLD + "Nexus Box";

            if (itemInHand.getItemMeta().getDisplayName().equals(expectedName)) {
                Location loc = event.getBlockPlaced().getLocation();

                // GriefPrevention claim check
                if (plugin.isGriefPreventionEnabled()) {
                    String claimError = GriefPrevention.instance.allowBuild(player, loc);
                    if (claimError != null) {
                        player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Nexus" +
                                ChatColor.DARK_RED + ChatColor.BOLD + "Vault" +
                                ChatColor.DARK_GRAY + ChatColor.BOLD + " > " +
                                ChatColor.RED + "You cannot place a Nexus Box in this claim!");
                        event.setCancelled(true);
                        return;
                    }
                }

                String uuid = player.getUniqueId().toString();
                plugin.getDataManager().addBlock(loc, uuid);

                player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Nexus" +
                        ChatColor.DARK_RED + ChatColor.BOLD + "Vault" +
                        ChatColor.DARK_GRAY + ChatColor.BOLD + " > " +
                        ChatColor.GREEN + "Nexus Box successfully placed.");
            }
        }
    }

    /**
     * Triggered when a player breaks a block.
     * Ensures only the owner or an administrator can break the Nexus Box and removes it from the database.
     *
     * @param event The BlockBreakEvent triggered by Bukkit.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Location loc = event.getBlock().getLocation();
        Player player = event.getPlayer();

        String ownerUUID = plugin.getDataManager().getOwner(loc);

        if (ownerUUID != null) {
            // GriefPrevention claim check for breaking
            if (plugin.isGriefPreventionEnabled()) {
                String claimError = GriefPrevention.instance.allowBreak(player, loc.getBlock(), loc);
                if (claimError != null) {
                    player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Nexus" +
                            ChatColor.DARK_RED + ChatColor.BOLD + "Vault" +
                            ChatColor.DARK_GRAY + ChatColor.BOLD + " > " +
                            ChatColor.RED + "This is a protected area, you cannot break this Nexus Box!");
                    event.setCancelled(true);
                    return;
                }
            }

            // Check if the player is the owner or has OP privileges
            if (player.getUniqueId().toString().equals(ownerUUID) || player.isOp()) {
                plugin.getDataManager().removeBlock(loc);

                event.setDropItems(false);
                loc.getWorld().dropItemNaturally(loc, plugin.getItemManager().getNexusBoxItem());

                player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Nexus" +
                        ChatColor.DARK_RED + ChatColor.BOLD + "Vault" +
                        ChatColor.DARK_GRAY + ChatColor.BOLD + " > " +
                        ChatColor.YELLOW + "Nexus Box removed.");
            } else {
                event.setCancelled(true);
                player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Nexus" +
                        ChatColor.DARK_RED + ChatColor.BOLD + "Vault" +
                        ChatColor.DARK_GRAY + ChatColor.BOLD + " > " +
                        ChatColor.RED + "This Nexus Box does not belong to you!");
            }
        }
    }

    /**
     * Triggered when a player interacts (clicks) on a block.
     * Opens the Nexus Box settings menu if the owner right-clicks it without sneaking.
     *
     * @param event The PlayerInteractEvent triggered by Bukkit.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Ignore if the player is sneaking (e.g., trying to place a hopper against it)
        if (event.getPlayer().isSneaking()) return;

        Location loc = event.getClickedBlock().getLocation();
        Player player = event.getPlayer();
        String ownerUUID = plugin.getDataManager().getOwner(loc);

        if (ownerUUID != null) {
            event.setCancelled(true);

            if (player.getUniqueId().toString().equals(ownerUUID) || player.isOp()) {
                plugin.getGuiManager().openBlockSettingsMenu(player, loc);
            } else {
                player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Nexus" +
                        ChatColor.DARK_RED + ChatColor.BOLD + "Vault" +
                        ChatColor.DARK_GRAY + ChatColor.BOLD + " > " +
                        ChatColor.RED + "Only the owner can access this Nexus Box's settings!");
            }
        }
    }
}