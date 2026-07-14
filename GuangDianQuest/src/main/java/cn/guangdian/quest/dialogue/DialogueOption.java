package cn.guangdian.quest.dialogue;

/**
 * 对话选项
 * 
 * 支持配置多个选项，如 [接受]、[拒绝]、[等等] 等
 */
public class DialogueOption {
    
    private final String text;       // 选项文本
    private final String action;     // 动作类型：accept, complete_talk, reject, command
    private final String command;    // 自定义命令（可选）
    
    public DialogueOption(String text, String action, String command) {
        this.text = text;
        this.action = action;
        this.command = command;
    }
    
    public String getText() { return text; }
    public String getAction() { return action; }
    public String getCommand() { return command; }
}