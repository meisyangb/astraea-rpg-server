package cn.guangdian.devour.config;

/**
 * 消息配置
 */
public class MessagesConfig {
    private String prefix;
    private String success;
    private String noSlot;
    private String notWeapon;
    private String alreadyDevoured;
    private String slotCleared;
    private String notDevourWeapon;
    private String guiOpened;
    private String reload;
    
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    
    public String getSuccess() { return success; }
    public void setSuccess(String success) { this.success = success; }
    
    public String getNoSlot() { return noSlot; }
    public void setNoSlot(String noSlot) { this.noSlot = noSlot; }
    
    public String getNotWeapon() { return notWeapon; }
    public void setNotWeapon(String notWeapon) { this.notWeapon = notWeapon; }
    
    public String getAlreadyDevoured() { return alreadyDevoured; }
    public void setAlreadyDevoured(String alreadyDevoured) { this.alreadyDevoured = alreadyDevoured; }
    
    public String getSlotCleared() { return slotCleared; }
    public void setSlotCleared(String slotCleared) { this.slotCleared = slotCleared; }
    
    public String getNotDevourWeapon() { return notDevourWeapon; }
    public void setNotDevourWeapon(String notDevourWeapon) { this.notDevourWeapon = notDevourWeapon; }
    
    public String getGuiOpened() { return guiOpened; }
    public void setGuiOpened(String guiOpened) { this.guiOpened = guiOpened; }
    
    public String getReload() { return reload; }
    public void setReload(String reload) { this.reload = reload; }
}
