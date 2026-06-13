package cn.guangdian.devour.config;

/**
 * 打开方式配置
 */
public class OpenMethodConfig {
    private boolean command;
    private boolean rightClick;
    private boolean shiftRightClick;
    
    public boolean isCommand() { return command; }
    public void setCommand(boolean command) { this.command = command; }
    
    public boolean isRightClick() { return rightClick; }
    public void setRightClick(boolean rightClick) { this.rightClick = rightClick; }
    
    public boolean isShiftRightClick() { return shiftRightClick; }
    public void setShiftRightClick(boolean shiftRightClick) { this.shiftRightClick = shiftRightClick; }
}
