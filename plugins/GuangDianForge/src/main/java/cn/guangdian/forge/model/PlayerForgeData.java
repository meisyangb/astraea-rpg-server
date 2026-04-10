package cn.guangdian.forge.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家锻造数据
 */
public class PlayerForgeData {
    private final UUID playerId;
    private int forgeLevel;
    private long forgeExp;
    private Set<String> learnedRecipes;
    private int totalForges;
    private int successForges;

    public PlayerForgeData(UUID id) {
        this.playerId = id;
        this.forgeLevel = 1;
        this.forgeExp = 0;
        this.learnedRecipes = new HashSet<>();
        this.totalForges = 0;
        this.successForges = 0;
    }

    public boolean hasLearned(String recipeId) {
        return learnedRecipes.contains(recipeId);
    }

    public void learnRecipe(String recipeId) {
        learnedRecipes.add(recipeId);
    }

    public UUID getPlayerId() { return playerId; }
    public int getForgeLevel() { return forgeLevel; }
    public void setForgeLevel(int forgeLevel) { this.forgeLevel = forgeLevel; }
    public long getForgeExp() { return forgeExp; }
    public void setForgeExp(long forgeExp) { this.forgeExp = forgeExp; }
    public Set<String> getLearnedRecipes() { return learnedRecipes; }
    public void setLearnedRecipes(Set<String> learnedRecipes) { this.learnedRecipes = learnedRecipes; }
    public int getTotalForges() { return totalForges; }
    public void setTotalForges(int totalForges) { this.totalForges = totalForges; }
    public int getSuccessForges() { return successForges; }
    public void setSuccessForges(int successForges) { this.successForges = successForges; }
}