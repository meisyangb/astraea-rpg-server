package cn.guangdian.classsystem.enums;

/**
 * 途径枚举 - 克苏鲁风格四条途径
 * 参考《诡秘之主》风格设计
 */
public enum Pathway {
    
    ABYSS("深渊途径", "abyss"),
    VOID("虚空途径", "void"),
    SHADOW("暗影途径", "shadow"),
    CORRUPTION("腐化途径", "corruption");
    
    private final String name;
    private final String id;
    
    Pathway(String name, String id) {
        this.name = name;
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getId() {
        return id;
    }
    
    /**
     * 根据ID获取途径
     */
    public static Pathway fromId(String id) {
        if (id == null) return null;
        switch (id) {
            case "abyss": return ABYSS;
            case "void": return VOID;
            case "shadow": return SHADOW;
            case "corruption": return CORRUPTION;
            default: return null;
        }
    }
}