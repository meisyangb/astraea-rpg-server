package cn.guangdian.dungeon.model.stage;

public enum WaveTriggerType {
    ON_KILL_COMPLETE,     // 击杀完成所有怪物后触发
    ON_TIME,              // 定时触发（秒）
    ON_LOCATION,          // 玩家到达指定位置触发
    ON_COMMAND,           // 手动输入命令触发
    ON_INTERACT,          // 交互触发（踩压力板、开箱子等）
    ON_ALL_PLAYERS_READY   // 所有玩家准备就绪
}
