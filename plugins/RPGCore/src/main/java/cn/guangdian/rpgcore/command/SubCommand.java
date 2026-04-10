package cn.guangdian.rpgcore.command;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 子命令抽象类
 * 
 * <p>所有GuangDian插件的子命令都应继承此类。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public abstract class SubCommand {

    private final String name;
    private final String permission;
    private final String description;
    private final List<String> aliases;
    private final boolean playerOnly;

    public SubCommand(String name, String permission, String description) {
        this(name, permission, description, new ArrayList<>(), false);
    }

    public SubCommand(String name, String permission, String description, List<String> aliases, boolean playerOnly) {
        this.name = name;
        this.permission = permission;
        this.description = description;
        this.aliases = aliases;
        this.playerOnly = playerOnly;
    }

    /**
     * 执行命令
     * 
     * @param sender 命令发送者
     * @param args 命令参数（已移除子命令名称）
     */
    public abstract void execute(CommandSender sender, String[] args);

    /**
     * Tab补全
     * 
     * @param sender 命令发送者
     * @param args 当前参数
     * @return 补全列表
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    // ========== Getters ==========

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public boolean isPlayerOnly() {
        return playerOnly;
    }

    // ========== Builder方法 ==========

    /**
     * 创建Builder
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Builder类
     */
    public static class Builder {
        private final String name;
        private String permission;
        private String description = "";
        private List<String> aliases = new ArrayList<>();
        private boolean playerOnly = false;
        private java.util.function.BiConsumer<CommandSender, String[]> executor;

        public Builder(String name) {
            this.name = name;
        }

        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder aliases(String... aliases) {
            this.aliases = Arrays.asList(aliases);
            return this;
        }

        public Builder playerOnly() {
            this.playerOnly = true;
            return this;
        }

        public Builder executor(java.util.function.BiConsumer<CommandSender, String[]> executor) {
            this.executor = executor;
            return this;
        }

        public SubCommand build() {
            if (executor == null) {
                throw new IllegalStateException("Executor must be set!");
            }
            return new SubCommand(name, permission, description, aliases, playerOnly) {
                @Override
                public void execute(CommandSender sender, String[] args) {
                    executor.accept(sender, args);
                }
            };
        }
    }
}