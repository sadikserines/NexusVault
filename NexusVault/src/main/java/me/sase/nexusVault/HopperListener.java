package me.sase.nexusVault;

import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Intercepts items flowing into the Nexus Box via hoppers.
 * Prevents physical insertion and securely redirects items to the virtual vault or auto-sells them based on block settings.
 */
public class HopperListener implements Listener {

    private final NexusVault plugin;

    public HopperListener(NexusVault plugin) {
        this.plugin = plugin;
    }

    /**
     * Triggered when a hopper picks up an item from the ground (e.g., from water streams).
     *
     * @param event The InventoryPickupItemEvent triggered by Bukkit.
     */
    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (event.getInventory().getHolder() instanceof Hopper) {
            Hopper hopper = (Hopper) event.getInventory().getHolder();
            processHopper(hopper.getBlock());
        }
    }

    /**
     * Triggered when an item is moved into the hopper from another inventory.
     *
     * @param event The InventoryMoveItemEvent triggered by Bukkit.
     */
    @EventHandler
    public void onHopperReceive(InventoryMoveItemEvent event) {
        if (event.getDestination().getHolder() instanceof Hopper) {
            Hopper hopper = (Hopper) event.getDestination().getHolder();
            processHopper(hopper.getBlock());
        }
    }

    /**
     * Evaluates the hopper's contents and extracts items if pointing to a Nexus Box.
     * Bypasses the disk via caching unless the item is immediately auto-sold.
     * Delayed by 1 tick to sync with core Minecraft engine physics.
     *
     * @param hopperBlock The Block object of the active Hopper.
     */
    private void processHopper(Block hopperBlock) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (!(hopperBlock.getBlockData() instanceof org.bukkit.block.data.type.Hopper)) return;

            org.bukkit.block.data.type.Hopper hopperData = (org.bukkit.block.data.type.Hopper) hopperBlock.getBlockData();
            Block targetBlock = hopperBlock.getRelative(hopperData.getFacing());

            String ownerUUID = plugin.getDataManager().getOwner(targetBlock.getLocation());

            if (ownerUUID != null) {
                Hopper hopperState = (Hopper) hopperBlock.getState();
                Inventory hopperInv = hopperState.getInventory();

                boolean isAutoSell = plugin.getDataManager().isAutoSellEnabled(targetBlock.getLocation());

                for (int i = 0; i < hopperInv.getSize(); i++) {
                    ItemStack item = hopperInv.getItem(i);

                    if (item != null && item.getType() != Material.AIR) {

                        if (isAutoSell) {
                            Double sellPrice = EconomyShopGUIHook.getItemSellPrice(new ItemStack(item.getType(), 1));

                            if (sellPrice != null && sellPrice > 0) {
                                double total = sellPrice * item.getAmount();
                                plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)), total);
                            }
                        } else {
                            // Caches the item addition in RAM, bypassing the disk to prevent latency.
                            plugin.getDataManager().addItemToVault(ownerUUID, item.getType(), item.getAmount(), false);
                        }

                        hopperInv.setItem(i, null);
                    }
                }
            }
        }, 1L);
    }
}