package cn.guangdian.rpgcore.annotation;

import java.lang.annotation.*;

/**
 * RPG组件注解 - 标记可被自动发现的组件
 *
 * <p>标注在需要被 RPGCore 自动管理的组件类上，如 Listener、CommandExecutor 等。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @RPGComponent(type = ComponentType.LISTENER)
 * public class PlayerJoinListener implements Listener {
 *     @EventHandler
 *     public void onPlayerJoin(PlayerJoinEvent event) {
 *         // 处理逻辑...
 *     }
 * }
 * }</pre>
 *
 * @author GuangDian
 * @since 2.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface RPGComponent {

    /**
     * 组件类型
     */
    enum Type {
        /**
         * 事件监听器
         */
        LISTENER,
        /**
         * 命令执行器
         */
        COMMAND,
        /**
         * 定时任务
         */
        TASK,
        /**
         * 占位符扩展
         */
        PLACEHOLDER,
        /**
         * 通用组件
         */
        COMPONENT
    }

    /**
     * 组件类型
     *
     * @return 组件类型
     */
    Type type() default Type.COMPONENT;

    /**
     * 组件名称
     *
     * @return 组件名称，默认为空（使用类名）
     */
    String name() default "";

    /**
     * 组件优先级（用于排序）
     *
     * @return 优先级，数值越大优先级越高
     */
    int priority() default 0;

    /**
     * 是否启用
     *
     * @return 是否启用，默认为 true
     */
    boolean enabled() default true;
}