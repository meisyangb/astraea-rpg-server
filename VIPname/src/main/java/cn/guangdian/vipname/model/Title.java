package cn.guangdian.vipname.model;

import java.util.List;

/**
 * 称号模型
 */
public class Title {
    
    private final String id;           // 称号ID
    private final String name;         // 称号名称
    private final String display;      // 显示文本
    private final String prefix;       // 前缀
    private final String suffix;       // 后缀
    private final int priority;        // 优先级
    private final String permission;   // 权限节点
    private final List<String> variables; // 关联的变量
    
    public Title(String id, String name, String display, String prefix, String suffix,
                int priority, String permission, List<String> variables) {
        this.id = id;
        this.name = name;
        this.display = display;
        this.prefix = prefix;
        this.suffix = suffix;
        this.priority = priority;
        this.permission = permission;
        this.variables = variables;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDisplay() { return display; }
    public String getPrefix() { return prefix; }
    public String getSuffix() { return suffix; }
    public int getPriority() { return priority; }
    public String getPermission() { return permission; }
    public List<String> getVariables() { return variables; }
    
    /**
     * 获取完整显示格式
     */
    public String getFullDisplay() {
        return prefix + display + suffix;
    }
}