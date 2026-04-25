package cn.guangdian.collection.papi;

import cn.guangdian.collection.GuangDianCollection;
import cn.guangdian.collection.api.CollectionService;
import cn.guangdian.rpgcore.integration.PlaceholderService;

public class CollectionPlaceholder {
    
    private final GuangDianCollection plugin;
    private final CollectionService collectionService;
    
    public CollectionPlaceholder(GuangDianCollection plugin, CollectionService collectionService) {
        this.plugin = plugin;
        this.collectionService = collectionService;
    }
    
    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdcollection", (player, params) -> {
            if (player == null) return "";
            
            String[] args = params.split("_");
            
            if (args.length == 0) return "";
            
            switch (args[0].toLowerCase()) {
                case "totalitems":
                    return String.valueOf(collectionService.getTotalItemsCollected(player.getUniqueId()));
                    
                case "progress":
                    if (args.length < 2) return "0";
                    if (player.isOnline()) {
                        return String.valueOf(collectionService.getCategoryProgress(player.getPlayer(), args[1]));
                    }
                    return "0";
                    
                case "complete":
                    if (args.length < 2) return "false";
                    if (player.isOnline()) {
                        return String.valueOf(collectionService.isCategoryComplete(player.getPlayer(), args[1]));
                    }
                    return "false";
                    
                default:
                    return "";
            }
        });
    }
    
    public void unregister() {
    }
}
