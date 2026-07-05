package cn.guangdian.classsystem.enums;

/**
 * 序列枚举 - 克苏鲁风格序列系统
 * 序列9（最低）→ 序列0（最高，真神）
 * 序列越低越强
 */
public enum Sequence {
    
    SEQ_0(0, "真神", "序列0"),
    SEQ_1(1, "神子", "序列1"),
    SEQ_2(2, "主宰", "序列2"),
    SEQ_3(3, "之王", "序列3"),
    SEQ_4(4, "神选", "序列4"),
    SEQ_5(5, "暴君", "序列5"),
    SEQ_6(6, "领主", "序列6"),
    SEQ_7(7, "骑士", "序列7"),
    SEQ_8(8, "守卫", "序列8"),
    SEQ_9(9, "行者", "序列9");
    
    private final int level;
    private final String title;
    private final String name;
    
    Sequence(int level, String title, String name) {
        this.level = level;
        this.title = title;
        this.name = name;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * 是否是真神
     */
    public boolean isTrueGod() {
        return level == 0;
    }
    
    /**
     * 是否是半神
     */
    public boolean isDemigod() {
        return level >= 1 && level <= 4;
    }
    
    /**
     * 是否可以晋升（序列 > 0）
     */
    public boolean canAdvance() {
        return level > 0;
    }
    
    /**
     * 获取下一序列（更强的序列）
     */
    public Sequence nextSequence() {
        if (level <= 0) return null;
        switch (level) {
            case 9: return SEQ_8;
            case 8: return SEQ_7;
            case 7: return SEQ_6;
            case 6: return SEQ_5;
            case 5: return SEQ_4;
            case 4: return SEQ_3;
            case 3: return SEQ_2;
            case 2: return SEQ_1;
            case 1: return SEQ_0;
            default: return null;
        }
    }
    
    /**
     * 根据等级获取序列
     */
    public static Sequence fromLevel(int level) {
        switch (level) {
            case 0: return SEQ_0;
            case 1: return SEQ_1;
            case 2: return SEQ_2;
            case 3: return SEQ_3;
            case 4: return SEQ_4;
            case 5: return SEQ_5;
            case 6: return SEQ_6;
            case 7: return SEQ_7;
            case 8: return SEQ_8;
            case 9: return SEQ_9;
            default: return SEQ_9;
        }
    }
}