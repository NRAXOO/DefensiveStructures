package com.example.defensivewalls.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** Comprobacion y consumo atomico de materiales del inventario del jugador. */
public final class CostService {

    private final ItemFactory items;

    public CostService(ItemFactory items) {
        this.items = items;
    }

    private boolean usable(ItemStack stack, String token) {
        return stack != null && !stack.getType().isAir()
                && ItemMatcher.matches(token, stack.getType())
                && !items.isStructureItem(stack);
    }

    public int count(Player player, String token) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (usable(stack, token)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    public boolean has(Player player, Cost cost) {
        return cost.amount() <= 0 || count(player, cost.token()) >= cost.amount();
    }

    /** Consume exactamente el coste. Si no hay suficiente no modifica nada y devuelve false. */
    public boolean take(Player player, Cost cost) {
        if (cost.amount() <= 0) {
            return true;
        }
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getStorageContents(); // copias
        int remaining = cost.amount();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (!usable(stack, cost.token())) {
                continue;
            }
            int take = Math.min(remaining, stack.getAmount());
            if (take == stack.getAmount()) {
                contents[i] = null;
            } else {
                stack.setAmount(stack.getAmount() - take);
            }
            remaining -= take;
        }
        if (remaining > 0) {
            return false;
        }
        inv.setStorageContents(contents);
        return true;
    }

    /** Devuelve el coste al jugador (solo para deshacer una operacion fallida). */
    public void refund(Player player, Cost cost) {
        int left = cost.amount();
        var material = ItemMatcher.representative(cost.token());
        while (left > 0) {
            int n = Math.min(left, material.getMaxStackSize());
            var leftovers = player.getInventory().addItem(new ItemStack(material, n));
            leftovers.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            left -= n;
        }
    }
}
