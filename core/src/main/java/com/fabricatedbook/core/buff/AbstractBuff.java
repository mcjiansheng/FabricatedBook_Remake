package com.fabricatedbook.core.buff;

import com.fabricatedbook.core.entity.AbstractEntity;

/**
 * AbstractBuff — Buff 骨架实现类
 * <p>
 * 提供 BuffHook 接口的默认骨架实现。
 * 子类只需重写需要参与结算的钩子方法。
 * 持有 Buff 名称、层数和图标路径。
 * <p>
 * 引用方：各具体 Buff 实现（继承此类）
 */
public abstract class AbstractBuff implements BuffHook {

    /** Buff 名称 */
    protected final String name;

    /** Buff 当前层数/堆叠数 */
    protected int stack;

    /** Buff 图标资源路径 */
    protected String iconPath;

    /**
     * 构造 Buff 骨架。
     *
     * @param name  Buff 名称
     * @param stack Buff 层数
     */
    protected AbstractBuff(String name, int stack) {
        this.name = name;
        this.stack = Math.max(0, stack);
        this.iconPath = "icons/buff/" + name + ".png";
    }

    @Override
    public String getBuffName() {
        return name;
    }

    @Override
    public int getStack() {
        return stack;
    }

    /**
     * 设置 Buff 层数。
     *
     * @param stack 新层数
     */
    public void setStack(int stack) {
        this.stack = Math.max(0, stack);
    }

    /**
     * 增加 Buff 层数。
     *
     * @param amount 增加量
     */
    public void addStack(int amount) {
        this.stack += Math.max(0, amount);
    }

    /**
     * 获取 Buff 图标路径。
     *
     * @return 图标路径字符串
     */
    public String getIconPath() {
        return iconPath;
    }

    /**
     * 设置 Buff 图标路径。
     *
     * @param iconPath 图标路径
     */
    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    @Override
    public String toString() {
        return name + "[" + stack + "]";
    }
}
