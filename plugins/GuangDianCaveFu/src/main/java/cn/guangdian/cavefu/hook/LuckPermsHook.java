package cn.guangdian.cavefu.hook;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.luckperms.api.context.ImmutableContextSet;

public class LuckPermsHook {
    private static final Logger logger = Logger.getLogger("GuangDianCaveFu");
    private final String worldName;
    private LuckPerms luckPerms;
    private boolean enabled = false;

    private static final List<String> CAVE_WORLD_PERMISSIONS = List.of(
        "multiverse.core.tp",
        "multiverse.teleport.self",
        "essentials.sethome",
        "essentials.home",
        "essentials.tpa",
        "essentials.tpaccept",
        "essentials.tpdeny",
        "essentials.spawn",
        "essentials.msg",
        "essentials.r",
        "essentials.pay",
        "essentials.balance",
        "essentials.afk",
        "essentials.back",
        "guangdianmenu.use",
        "guangdianmenu.open",
        "cavefu.use",
        "cavefu.home",
        "cavefu.create"
    );

    public LuckPermsHook(String worldName) {
        this.worldName = worldName;
        setup();
    }

    private void setup() {
        try {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore == null) {
                return;
            }
            ExternalServiceIntegration externalServices = rpgCore.getExternalServices();
            if (externalServices != null) {
                luckPerms = externalServices.getLuckPerms().orElse(null);
                enabled = luckPerms != null;
            }
        } catch (Exception e) {
            logger.warning("[洞府] LuckPerms 初始化失败: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setupWorldPermissions() {
        if (!enabled || luckPerms == null) return;

        try {
            Group defaultGroup = luckPerms.getGroupManager().getGroup("default");
            if (defaultGroup != null) {
                for (String permission : CAVE_WORLD_PERMISSIONS) {
                    PermissionNode node = PermissionNode.builder(permission)
                        .value(true)
                        .withContext("world", worldName)
                        .build();
                    defaultGroup.data().add(node);
                }
                luckPerms.getGroupManager().saveGroup(defaultGroup);
                logger.info("[洞府] 已为 default 组添加洞府世界权限，共 " + CAVE_WORLD_PERMISSIONS.size() + " 个");
            }

            Collection<Group> groups = luckPerms.getGroupManager().getLoadedGroups();
            for (Group group : groups) {
                InheritanceNode inheritNode = InheritanceNode.builder(group.getName())
                    .value(true)
                    .withContext("world", worldName)
                    .build();
                group.data().add(inheritNode);
                luckPerms.getGroupManager().saveGroup(group);
            }

            logger.info("[洞府] 已为世界 " + worldName + " 配置权限继承，共处理 " + groups.size() + " 个权限组");
        } catch (Exception e) {
            logger.warning("[洞府] 配置权限继承失败: " + e.getMessage());
        }
    }

    public boolean isPermissionsConfigured() {
        if (!enabled || luckPerms == null) return true;

        try {
            Group defaultGroup = luckPerms.getGroupManager().getGroup("default");
            if (defaultGroup == null) return false;

            for (String permission : CAVE_WORLD_PERMISSIONS) {
                boolean hasPermission = false;
                for (Map.Entry<ImmutableContextSet, Collection<Node>> entry : defaultGroup.data().toMap().entrySet()) {
                    Collection<Node> nodeList = entry.getValue();
                    for (Node node : nodeList) {
                        if (node instanceof PermissionNode && node.getContexts().contains("world", worldName)) {
                            PermissionNode permNode = (PermissionNode) node;
                            if (permNode.getPermission().equals(permission) && permNode.getValue()) {
                                hasPermission = true;
                                break;
                            }
                        }
                    }
                    if (hasPermission) break;
                }
                if (!hasPermission) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            logger.warning("[洞府] 检查权限配置失败: " + e.getMessage());
            return false;
        }
    }
}
