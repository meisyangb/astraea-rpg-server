package cn.guangdian.devour.config;

/**
 * 吞噬设置
 */
public class DevourSettings {
    private boolean consumeItem;
    private boolean allowClear;
    private boolean returnOnClear;
    
    private boolean soundEnable;
    private String soundOpen;
    private String soundDevour;
    private String soundClear;
    private String soundClose;
    private String soundError;
    private float soundVolume;
    private float soundPitch;
    
    private boolean particleEnable;
    private String particleType;
    private int particleCount;
    
    public boolean isConsumeItem() { return consumeItem; }
    public void setConsumeItem(boolean consumeItem) { this.consumeItem = consumeItem; }
    
    public boolean isAllowClear() { return allowClear; }
    public void setAllowClear(boolean allowClear) { this.allowClear = allowClear; }
    
    public boolean isReturnOnClear() { return returnOnClear; }
    public void setReturnOnClear(boolean returnOnClear) { this.returnOnClear = returnOnClear; }
    
    public boolean isSoundEnable() { return soundEnable; }
    public void setSoundEnable(boolean soundEnable) { this.soundEnable = soundEnable; }
    
    public String getSoundOpen() { return soundOpen; }
    public void setSoundOpen(String soundOpen) { this.soundOpen = soundOpen; }
    
    public String getSoundDevour() { return soundDevour; }
    public void setSoundDevour(String soundDevour) { this.soundDevour = soundDevour; }
    
    public String getSoundClear() { return soundClear; }
    public void setSoundClear(String soundClear) { this.soundClear = soundClear; }
    
    public String getSoundClose() { return soundClose; }
    public void setSoundClose(String soundClose) { this.soundClose = soundClose; }
    
    public String getSoundError() { return soundError; }
    public void setSoundError(String soundError) { this.soundError = soundError; }
    
    public float getSoundVolume() { return soundVolume; }
    public void setSoundVolume(float soundVolume) { this.soundVolume = soundVolume; }
    
    public float getSoundPitch() { return soundPitch; }
    public void setSoundPitch(float soundPitch) { this.soundPitch = soundPitch; }
    
    public boolean isParticleEnable() { return particleEnable; }
    public void setParticleEnable(boolean particleEnable) { this.particleEnable = particleEnable; }
    
    public String getParticleType() { return particleType; }
    public void setParticleType(String particleType) { this.particleType = particleType; }
    
    public int getParticleCount() { return particleCount; }
    public void setParticleCount(int particleCount) { this.particleCount = particleCount; }
}
