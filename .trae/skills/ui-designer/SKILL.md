---
name: ui-designer
description: "UI设计专家 - 设计游戏界面、菜单系统、全息图显示、用户体验优化。在涉及GUI、菜单、显示系统时调用。"
---

# UI设计专家 (UI Designer)

> 专业的游戏界面设计技能，打造优秀的用户体验

---

## 设计原则

### 1. 一致性原则
- 统一的色彩风格
- 一致的布局结构
- 统一的交互方式
- 一致的字体和图标

### 2. 可用性原则
- 清晰的信息层级
- 直观的操作方式
- 及时的反馈机制
- 容错性设计

### 3. 美观性原则
- 合理的色彩搭配
- 适当的留白
- 统一的视觉风格
- 流畅的动画效果

---

## 技术实现

### 1. 库存界面 (Inventory GUI)
```java
// 创建菜单
public class MainMenu implements InventoryHolder {
    private final Inventory inventory;
    
    public MainMenu() {
        this.inventory = Bukkit.createInventory(this, 54, 
            Component.text("主菜单").color(NamedTextColor.GOLD));
        initializeItems();
    }
    
    private void initializeItems() {
        // 装饰边框
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i : new int[]{0,1,2,3,4,5,6,7,8,45,46,47,48,49,50,51,52,53}) {
            inventory.setItem(i, border);
        }
        
        // 功能按钮
        inventory.setItem(20, createItem(Material.DIAMOND_SWORD, 
            Component.text("装备系统").color(NamedTextColor.AQUA)));
        inventory.setItem(22, createItem(Material.BOOK, 
            Component.text("技能系统").color(NamedTextColor.GREEN)));
        inventory.setItem(24, createItem(Material.CHEST, 
            Component.text("背包").color(NamedTextColor.YELLOW)));
    }
    
    private ItemStack createItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
```

### 2. 全息图显示 (TextDisplay)
```java
// 创建玩家头顶显示
public void createPlayerDisplay(Player player, String text) {
    Location loc = player.getLocation().clone().add(0, 2.8, 0);
    
    TextDisplay display = player.getWorld().spawn(loc, TextDisplay.class);
    display.setText(text);
    display.setBillboard(Display.Billboard.CENTER);
    display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
    display.setShadowed(true);
    display.setSeeThrough(false);
    
    // 设置变换
    display.setTransformation(new Transformation(
        new Vector3f(0, 0, 0),
        new Quaternionf(),
        new Vector3f(1, 1, 1),
        new Quaternionf()
    ));
    
    // 骑乘到玩家身上
    player.addPassenger(display);
}
```

### 3. Boss血条 (BossBar)
```java
// 创建Boss血条
public void createBossBar(Player player, String title, double health, double maxHealth) {
    BossBar bossBar = BossBar.bossBar(
        Component.text(title).color(NamedTextColor.RED),
        (float)(health / maxHealth),
        BossBar.Color.RED,
        BossBar.Overlay.PROGRESS
    );
    
    player.showBossBar(bossBar);
    
    // 更新血条
    bossBar.progress((float)(health / maxHealth));
    bossBar.name(Component.text(title + " " + (int)health + "/" + (int)maxHealth)
        .color(NamedTextColor.RED));
}
```

### 4. 动作栏消息 (Action Bar)
```java
// 发送动作栏消息
public void sendActionBar(Player player, String message) {
    player.sendActionBar(Component.text(message).color(NamedTextColor.YELLOW));
}

// 带进度条的动作栏
public void sendProgressActionBar(Player player, String label, double current, double max) {
    int progress = (int)((current / max) * 20);
    StringBuilder bar = new StringBuilder(label + " ");
    
    bar.append("§a");
    for (int i = 0; i < progress; i++) bar.append("█");
    bar.append("§7");
    for (int i = progress; i < 20; i++) bar.append("░");
    
    bar.append(" §f").append(String.format("%.1f%%", (current/max)*100));
    
    player.sendActionBar(Component.text(bar.toString()));
}
```

### 5. 标题和副标题 (Title)
```java
// 发送标题
public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
    player.showTitle(Title.title(
        Component.text(title).color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true),
        Component.text(subtitle).color(NamedTextColor.WHITE),
        Title.Times.times(
            Duration.ofMillis(fadeIn * 50),
            Duration.ofMillis(stay * 50),
            Duration.ofMillis(fadeOut * 50)
        )
    ));
}
```

---

## 设计模式

### 1. 菜单页模式
```java
public abstract class MenuPage implements InventoryHolder {
    protected final Player player;
    protected final int page;
    protected final Inventory inventory;
    
    public MenuPage(Player player, int page, int size, Component title) {
        this.player = player;
        this.page = page;
        this.inventory = Bukkit.createInventory(this, size, title);
    }
    
    public abstract void initializeItems();
    public abstract void onClick(int slot);
    public abstract MenuPage getNextPage();
    public abstract MenuPage getPrevPage();
}
```

### 2. 按钮构建器模式
```java
public class ButtonBuilder {
    private final ItemStack item;
    private final ItemMeta meta;
    
    public ButtonBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }
    
    public ButtonBuilder name(Component name) {
        meta.displayName(name);
        return this;
    }
    
    public ButtonBuilder lore(Component... lore) {
        meta.lore(Arrays.asList(lore));
        return this;
    }
    
    public ButtonBuilder glow(boolean glow) {
        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }
    
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}

// 使用示例
ItemStack button = new ButtonBuilder(Material.DIAMOND)
    .name(Component.text("稀有装备").color(NamedTextColor.AQUA))
    .lore(
        Component.text("攻击力: +50").color(NamedTextColor.GRAY),
        Component.text(""),
        Component.text("点击装备").color(NamedTextColor.YELLOW)
    )
    .glow(true)
    .build();
```

---

## 色彩规范

### 主色调
| 用途 | 颜色 | NamedTextColor |
|------|------|----------------|
| 主要按钮 | 金色 | GOLD |
| 成功/确认 | 绿色 | GREEN |
| 警告/取消 | 红色 | RED |
| 信息提示 | 青色 | AQUA |
| 次要文字 | 灰色 | GRAY |
| 稀有物品 | 浅紫色 | LIGHT_PURPLE |

### 渐变效果
```java
// 创建渐变色文字
public Component createGradientText(String text, NamedTextColor start, NamedTextColor end) {
    Component result = Component.empty();
    // 实现渐变逻辑
    return result;
}
```

---

## 输出格式

```
╔══════════════════════════════════════════════════════════╗
║  🎨 UI设计方案                                           ║
╠══════════════════════════════════════════════════════════╣
║  设计目标: [菜单/显示/界面]                              ║
║  设计风格: [现代/复古/简约/华丽]                         ║
╠══════════════════════════════════════════════════════════╣
║  设计要点:                                               ║
║    1. [要点描述]                                         ║
║    2. [要点描述]                                         ║
║    3. [要点描述]                                         ║
╠══════════════════════════════════════════════════════════╣
║  技术实现:                                               ║
║    [代码示例]                                            ║
╠══════════════════════════════════════════════════════════╣
║  交互设计:                                               ║
║    - 点击: [效果]                                        ║
║    - 悬停: [效果]                                        ║
║    - 动画: [效果]                                        ║
╚══════════════════════════════════════════════════════════╝
```

---

*技能版本: 1.0*
*最后更新: 2026-04-10*
