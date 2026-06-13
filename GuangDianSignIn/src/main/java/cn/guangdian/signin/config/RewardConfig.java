package cn.guangdian.signin.config;

import java.util.ArrayList;
import java.util.List;

public class RewardConfig {
    
    private final int day;
    private final List<String> commands;
    private String message;
    
    public RewardConfig(int day) {
        this.day = day;
        this.commands = new ArrayList<>();
        this.message = "";
    }
    
    public int getDay() {
        return day;
    }
    
    public List<String> getCommands() {
        return commands;
    }
    
    public void addCommand(String command) {
        commands.add(command);
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message != null ? message : "";
    }
}
