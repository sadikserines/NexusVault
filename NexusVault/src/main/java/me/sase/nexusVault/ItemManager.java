package me.sase.nexusVault;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

/**
 * Handles the initialization and metadata injection for custom plugin items.
 * Generates the crafting recipes and textures for the Nexus Box.
 */
public class ItemManager {

    private final NexusVault plugin;
    private ItemStack nexusBoxItem;

    public ItemManager(NexusVault plugin) {
        this.plugin = plugin;
        createNexusBoxItem();
        registerRecipe();
    }

    /**
     * Creates the custom player head item representing the Nexus Box.
     * Enforces a static UUID to ensure the items stack properly in player inventories.
     */
    private void createNexusBoxItem() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Nexus Box");

        PlayerProfile profile = Bukkit.createProfile(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), "NexusBox");
        PlayerTextures textures = profile.getTextures();

        try {
            textures.setSkin(new URL("http://textures.minecraft.net/texture/8d52dde2785e571d17a1d6d1d70fdb75d3f3fd023597d566147a85b1036eb983"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        profile.setTextures(textures);
        meta.setPlayerProfile((com.destroystokyo.paper.profile.PlayerProfile) profile);

        head.setItemMeta(meta);
        this.nexusBoxItem = head;
    }

    /**
     * Registers the shaped crafting recipe for the Nexus Box into the server.
     */
    private void registerRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "nexus_box");
        ShapedRecipe recipe = new ShapedRecipe(key, nexusBoxItem);

        recipe.shape("GNG", "NCN", "GNG");

        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('N', Material.NETHERITE_BLOCK);
        recipe.setIngredient('C', Material.CHEST);

        Bukkit.addRecipe(recipe);
    }

    /**
     * Exposes the customized Nexus Box ItemStack for other classes to spawn or drop.
     *
     * @return The configured ItemStack.
     */
    public ItemStack getNexusBoxItem() {
        return nexusBoxItem;
    }
}