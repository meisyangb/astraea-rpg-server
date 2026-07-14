package cn.guangdian.vipname.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家称号数据
 */
public class PlayerTitle {
    
    private final UUID playerId;
    private final Set<String> ownedTitles = new HashSet<>();
    private String currentTitle;
    
    public PlayerTitle(UUID playerId) {
        this.playerId = playerId;
    }
    
    public UUID getPlayerId() { return playerId; }
    public Set<String> getOwnedTitles() { return ownedTitles; }
    public String getCurrentTitle() { return currentTitle; }
    public void setCurrentTitle(String title) { this.currentTitle = title; }
    
    public void addTitle(String titleId) {
        ownedTitles.add(titleId.toLowerCase());
    }
    
    public void removeTitle(String titleId) {
        ownedTitles.remove(titleId.toLowerCase());
    }
    
    public boolean hasTitle(String titleId) {
        return ownedTitles.contains(titleId.toLowerCase());
    }
}