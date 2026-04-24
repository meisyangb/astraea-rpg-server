package cn.guangdian.collection.command;

import cn.guangdian.collection.GuangDianCollection;
import cn.guangdian.collection.api.CollectionService;
import cn.guangdian.collection.gui.CollectionGUIListener;
import cn.guangdian.collection.model.CollectionSet;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 图鉴命令 - 使用 RPGCore CommandFramework
 *
 * <p>基于注解驱动的命令系统，替代传统的 onCommand 方式。</p>
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "collection", description = "图鉴收集系统", permission = "collection.use")
public class CollectionCommand extends BaseCommand {

    private final GuangDianCollection plugin;
    private final CollectionService collectionService;
    private final CollectionGUIListener guiListener;

    public CollectionCommand(GuangDianCollection plugin, CollectionService collectionService, CollectionGUIListener guiListener) {
        this.plugin = plugin;
        this.collectionService = collectionService;
        this.guiListener = guiListener;
    }

    /**
     * 打开图鉴主界面
     */
    @SubCommand(name = "", playerOnly = true)
    @Description("打开图鉴主界面")
    public void openDefault(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        guiListener.openMainGUI(player);
    }

    /**
     * 打开图鉴主界面
     */
    @SubCommand(name = "open", playerOnly = true)
    @Description("打开图鉴主界面")
    public void open(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        guiListener.openMainGUI(player);
    }

    /**
     * 重新加载配置
     */
    @SubCommand(name = "reload", permission = "collection.admin")
    @Description("重新加载配置")
    public void reload(CommandContext ctx) {
        CommandSender sender = ctx.getSender();
        plugin.getConfigManager().reload();
        collectionService.reloadData();
        ctx.sendMessage(plugin.getConfigManager().getPrefix() + "<green>配置已重新加载");
    }

    /**
     * 查看收集统计
     */
    @SubCommand(name = "stats", playerOnly = true)
    @Description("查看收集统计")
    public void stats(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        int totalItems = collectionService.getTotalItemsCollected(player.getUniqueId());

        ctx.sendMessage(plugin.getConfigManager().getPrefix() + "<gold>===== 图鉴统计 =====");
        ctx.sendMessage("<yellow>收集物品总数: <white>" + totalItems);

        for (CollectionSet set : collectionService.getSets().values()) {
            int setProgress = 0;
            int setTotal = 0;

            for (String categoryId : set.getCategoryIds()) {
                setProgress += collectionService.getCategoryProgress(player, categoryId);
                java.util.Optional<cn.guangdian.collection.model.CollectionCategory> catOpt =
                    collectionService.getCategory(categoryId);
                if (catOpt.isPresent()) {
                    setTotal += catOpt.get().getTotalEntries();
                }
            }

            String status = setProgress >= setTotal ?
                "<green>已完成" :
                "<yellow>" + setProgress + "/" + setTotal;
            ctx.sendMessage("<gray>- " + set.getName() + ": " + status);
        }
    }

    /**
     * 显示帮助信息
     */
    @SubCommand(name = "help")
    @Description("显示帮助信息")
    public void help(CommandContext ctx) {
        CommandSender sender = ctx.getSender();

        ctx.sendMessage(plugin.getConfigManager().getPrefix() + "<gold>===== 图鉴帮助 =====");
        ctx.sendMessage("<yellow>/collection <gray>- 打开图鉴主界面");
        ctx.sendMessage("<yellow>/collection open <gray>- 打开图鉴主界面");
        ctx.sendMessage("<yellow>/collection stats <gray>- 查看收集统计");
        ctx.sendMessage("<yellow>/collection help <gray>- 显示帮助信息");

        if (sender.hasPermission("collection.admin")) {
            ctx.sendMessage("<red>/collection reload <gray>- 重新加载配置");
        }
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        List<String> completions = new ArrayList<>();

        if (context.getArgCount() == 0) {
            completions.addAll(List.of("open", "stats", "help"));
            if (context.hasPermission("collection.admin")) {
                completions.add("reload");
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(context.getStringArgOrDefault(0, "").toLowerCase()))
            .collect(Collectors.toList());
    }
}
