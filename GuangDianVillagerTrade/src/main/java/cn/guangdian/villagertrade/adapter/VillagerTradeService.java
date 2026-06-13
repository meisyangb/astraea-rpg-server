package cn.guangdian.villagertrade.adapter;

import java.util.List;

/**
 * 村民兑换服务接口
 *
 * <p>提供给其他插件调用的API</p>
 */
public interface VillagerTradeService {

    /**
     * 为指定玩家打开兑换界面
     *
     * @param playerName 玩家名称
     * @param recipeName 配方名称
     * @return 是否成功打开
     */
    boolean openTrade(String playerName, String recipeName);

    /**
     * 检查是否存在指定配方
     *
     * @param recipeName 配方名称
     * @return 是否存在
     */
    boolean hasRecipe(String recipeName);

    /**
     * 获取所有配方名称
     *
     * @return 配方名称列表
     */
    List<String> getRecipeNames();

    /**
     * 重新加载配方
     */
    void reloadRecipes();
}
