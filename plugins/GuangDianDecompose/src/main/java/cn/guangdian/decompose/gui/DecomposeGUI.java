package cn.guangdian.decompose.gui;

import cn.guangdian.decompose.GuangDianDecompose;
import cn.guangdian.decompose.manager.DecomposeManager;
import cn.guangdian.decompose.model.DecomposeRule;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DecomposeGUI implements InventoryHolder {

    private final GuangDianDecompose plugin;
    private final Map<UUID, ItemStack> pendingItems;
    private final Map<UUID, List<ItemStack>> previewRewards;

    private static final int GUI_SIZE = 36;
    private static final int INPUT_SLOT = 13;
    private static final int PREVIEW_START_SLOT = 19;
    private static final int PREVIEW_END_SLOT = 25;
    private static final int DECOMPOSE_BUTTON_SLOT = 31;
    private static final int CLOSE_BUTTON_SLOT = 35;

    public DecomposeGUI(GuangDianDecompose plugin) {
        this.plugin = plugin;
        this.pendingItems = new HashMap<>();
        this.previewRewards = new HashMap<>();
    }

    public void open(Player player) {
        String title = plugin.getConfig().getString("gui-title", "<dark_gray>装备分解");
        Inventory gui = Bukkit.createInventory(this, GUI_SIZE, Component.text(title.replace("&", "§")));

        fillBackground(gui);
        setupDecomposeButton(gui);
        setupCloseButton(gui);

        player.openInventory(gui);
    }

    private void fillBackground(Inventory gui) {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < GUI_SIZE; i++) {
            if (i != INPUT_SLOT && !isPreviewSlot(i) && i != DECOMPOSE_BUTTON_SLOT && i != CLOSE_BUTTON_SLOT) {
                gui.setItem(i, glass);
            }
        }

        ItemStack inputIndicator = createItem(Material.LIME_STAINED_GLASS_PANE, "<green>↓ 放入装备 ↓");
        gui.setItem(4, inputIndicator);
        gui.setItem(22, createItem(Material.LIME_STAINED_GLASS_PANE, "<green>↑ 预览材料 ↑"));
    }

    private boolean isPreviewSlot(int slot) {
        return slot >= PREVIEW_START_SLOT && slot <= PREVIEW_END_SLOT;
    }

    private void setupDecomposeButton(Inventory gui) {
        ItemStack button = createItem(Material.ANVIL, "<red><bold>点击分解", 
                "<gray>将装备放入中间槽位",
                "<gray>点击此按钮进行分解");
        gui.setItem(DECOMPOSE_BUTTON_SLOT, button);
    }

    private void setupCloseButton(Inventory gui) {
        ItemStack button = createItem(Material.BARRIER, "<red>关闭界面");
        gui.setItem(CLOSE_BUTTON_SLOT, button);
    }

    public void handleInput(Player player, Inventory gui, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            clearPreview(player, gui);
            return;
        }

        String mythicId = plugin.getMythicMobsHook().getMythicItemId(item);
        if (mythicId == null) {
            setPreviewError(gui, "<red>此物品无法分解!");
            return;
        }

        DecomposeRule rule = plugin.getRuleManager().getRule(mythicId);
        if (rule == null) {
            setPreviewError(gui, "<red>该装备没有分解规则!");
            return;
        }

        pendingItems.put(player.getUniqueId(), item.clone());
        updatePreview(gui, rule);
    }

    private void updatePreview(Inventory gui, DecomposeRule rule) {
        List<ItemStack> rewards = new ArrayList<>();

        int slot = PREVIEW_START_SLOT;
        for (DecomposeRule.MaterialReward material : rule.getMaterials()) {
            if (slot > PREVIEW_END_SLOT) break;

            ItemStack previewItem = createRewardPreview(material);
            if (previewItem != null) {
                gui.setItem(slot, previewItem);
                rewards.add(previewItem);
                slot++;
            }
        }

        ItemStack button = createItem(Material.ANVIL, "<green><bold>点击分解",
                "<gray>成功率: <yellow>" + (int)(rule.getChance() * 100) + "%",
                "",
                rule.hasCustomMessage() ? rule.getMessage() : "<gray>点击进行分解");
        gui.setItem(DECOMPOSE_BUTTON_SLOT, button);
    }

    private ItemStack createRewardPreview(DecomposeRule.MaterialReward material) {
        if (material.isMythicItem()) {
            return plugin.getMythicMobsHook().getMythicItem(material.getItemId(), material.getAmount());
        } else if (material.isVanillaItem()) {
            try {
                Material mat = Material.valueOf(material.getItemId().toUpperCase());
                return new ItemStack(mat, material.getAmount());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private void setPreviewError(Inventory gui, String error) {
        ItemStack errorItem = createItem(Material.RED_STAINED_GLASS_PANE, error);
        gui.setItem(PREVIEW_START_SLOT + 3, errorItem);

        ItemStack button = createItem(Material.BARRIER, "<red>无法分解", error);
        gui.setItem(DECOMPOSE_BUTTON_SLOT, button);
    }

    private void clearPreview(Player player, Inventory gui) {
        pendingItems.remove(player.getUniqueId());
        for (int i = PREVIEW_START_SLOT; i <= PREVIEW_END_SLOT; i++) {
            gui.setItem(i, null);
        }
        setupDecomposeButton(gui);
    }

    public DecomposeManager.DecomposeResult handleDecompose(Player player, Inventory gui) {
        ItemStack inputItem = gui.getItem(INPUT_SLOT);
        if (inputItem == null || inputItem.getType() == Material.AIR) {
            return DecomposeManager.DecomposeResult.failure("请先放入要分解的装备!");
        }

        DecomposeManager.DecomposeResult result = plugin.getDecomposeManager().decompose(player, inputItem);

        if (result.isSuccess()) {
            clearPreview(player, gui);
            player.sendMessage(result.getMessage().replace("&", "§"));
        } else {
            player.sendMessage("§c" + result.getMessage());
        }

        return result;
    }

    public boolean isDecomposeButton(int slot) {
        return slot == DECOMPOSE_BUTTON_SLOT;
    }

    public boolean isCloseButton(int slot) {
        return slot == CLOSE_BUTTON_SLOT;
    }

    public boolean isInputSlot(int slot) {
        return slot == INPUT_SLOT;
    }

    public int getInputSlot() {
        return INPUT_SLOT;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name.replace("&", "§")));
            if (lore.length > 0) {
                List<Component> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(Component.text(line.replace("&", "§")));
                }
                meta.lore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
