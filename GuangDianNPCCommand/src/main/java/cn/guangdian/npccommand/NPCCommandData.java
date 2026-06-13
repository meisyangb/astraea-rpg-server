package cn.guangdian.npccommand;

import java.util.ArrayList;
import java.util.List;

public class NPCCommandData {

    private final int npcId;
    private long cooldown;
    private final List<CommandEntry> commands;

    public NPCCommandData(int npcId) {
        this.npcId = npcId;
        this.cooldown = 0;
        this.commands = new ArrayList<>();
    }

    public int getNpcId() {
        return npcId;
    }

    public long getCooldown() {
        return cooldown;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }

    public List<CommandEntry> getCommands() {
        return commands;
    }

    public void addCommand(CommandType type, String command) {
        commands.add(new CommandEntry(type, command));
    }

    public void removeCommand(int index) {
        if (index >= 0 && index < commands.size()) {
            commands.remove(index);
        }
    }

    public void clearCommands() {
        commands.clear();
    }

    public enum CommandType {
        CONSOLE,
        PLAYER,
        OP,
        COMMAND,
        NO_PERMS
    }

    public static class CommandEntry {
        private final CommandType type;
        private final String command;

        public CommandEntry(CommandType type, String command) {
            this.type = type;
            this.command = command;
        }

        public CommandType getType() {
            return type;
        }

        public String getCommand() {
            return command;
        }
    }
}
