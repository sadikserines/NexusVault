package me.sase.nexusVault;

import org.bukkit.Material;

/**
 * A lightweight data object representing a player's active transaction phase.
 * Holds context variables temporarily while a player is utilizing the Action Menu.
 */
public class VaultSession {

    /** The target item material for the transaction. */
    public Material material;

    /** Defines the mode. True indicates a Market Sale, False indicates a Vault Withdrawal. */
    public boolean isSelling;

    /** Defines the multiplier format. True calculates logic as x64 stacks. */
    public boolean isStackMode;

    /** The selected quantity specified by the user. */
    public int amount;

    /**
     * Initializes a new secure session instance.
     *
     * @param material Target item material.
     * @param isSelling Intent mode.
     */
    public VaultSession(Material material, boolean isSelling) {
        this.material = material;
        this.isSelling = isSelling;
        this.isStackMode = false;
        this.amount = 1;
    }
}