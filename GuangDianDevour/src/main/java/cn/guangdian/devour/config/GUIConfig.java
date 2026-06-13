package cn.guangdian.devour.config;

import org.bukkit.Material;

import java.util.List;

/**
 * GUI 配置
 */
public class GUIConfig {
    private String title;
    private int size;
    private List<Integer> devourSlots;
    private int attributeDisplayStart;
    private int previewDisplayStart;
    private int confirmButton;
    private int cancelButton;
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    
    public List<Integer> getDevourSlots() { return devourSlots; }
    public void setDevourSlots(List<Integer> devourSlots) { this.devourSlots = devourSlots; }
    
    public int getAttributeDisplayStart() { return attributeDisplayStart; }
    public void setAttributeDisplayStart(int attributeDisplayStart) { this.attributeDisplayStart = attributeDisplayStart; }
    
    public int getPreviewDisplayStart() { return previewDisplayStart; }
    public void setPreviewDisplayStart(int previewDisplayStart) { this.previewDisplayStart = previewDisplayStart; }
    
    public int getConfirmButton() { return confirmButton; }
    public void setConfirmButton(int confirmButton) { this.confirmButton = confirmButton; }
    
    public int getCancelButton() { return cancelButton; }
    public void setCancelButton(int cancelButton) { this.cancelButton = cancelButton; }
}
