package cn.guangdian.rpgcore.event.events;

import cn.guangdian.rpgcore.event.CoreEvent;

import java.util.UUID;

/**
 * 模块启用事件
 * 
 * <p>当一个模块成功启用时触发。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class ModuleEnableEvent extends CoreEvent {

    private final String moduleId;
    private final String version;

    /**
     * 创建模块启用事件
     * 
     * @param moduleId 模块ID
     * @param version 模块版本
     */
    public ModuleEnableEvent(String moduleId, String version) {
        super(false);
        this.moduleId = moduleId;
        this.version = version;
    }

    /**
     * 获取模块ID
     */
    public String getModuleId() {
        return moduleId;
    }

    /**
     * 获取模块版本
     */
    public String getVersion() {
        return version;
    }
}