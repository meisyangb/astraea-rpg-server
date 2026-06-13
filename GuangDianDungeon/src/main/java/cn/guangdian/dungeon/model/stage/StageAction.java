package cn.guangdian.dungeon.model.stage;

import java.util.ArrayList;
import java.util.List;

public class StageAction {
    private ActionType type;
    private String content;
    private String subtitle;
    private int duration;
    private String sound;
    private float volume;
    private int seconds;
    
    public StageAction() {}
    
    public ActionType getType() { return type; }
    public void setType(ActionType type) { this.type = type; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    
    public String getSound() { return sound; }
    public void setSound(String sound) { this.sound = sound; }
    
    public float getVolume() { return volume; }
    public void setVolume(float volume) { this.volume = volume; }
    
    public int getSeconds() { return seconds; }
    public void setSeconds(int seconds) { this.seconds = seconds; }
    
    public enum ActionType {
        MESSAGE,
        TITLE,
        SOUND,
        DELAY,
        NEXT_STAGE,
        NEXT_WAVE,
        DUNGEON_COMPLETE,
        DUNGEON_FAIL,
        GIVE_EXP,
        GIVE_ITEM,
        COMMAND
    }
}
