package com.fabricatedbook.view.actor;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.fabricatedbook.view.ui.UiStyles;

/**
 * ButtonActor — 自定义按钮 Actor
 * <p>
 * 通用按钮组件，支持文本标签、悬停高亮和点击回调。
 * 使用 ShapeRenderer 绘制圆角矩形按钮，无贴图依赖。
 * <p>
 * 引用方：各 Screen 中的按钮创建（TitleScreen、EventScreen 等）
 */
public class ButtonActor extends Actor {

    private String label;
    private Runnable onClick;
    private boolean hovered;
    private boolean pressed;
    private final BitmapFont font;
    private final ShapeRenderer shapeRenderer;

    public static final float BUTTON_WIDTH = 200;
    public static final float BUTTON_HEIGHT = 50;

    // 按钮颜色
    private static final com.badlogic.gdx.graphics.Color NORMAL_COLOR =
            new com.badlogic.gdx.graphics.Color(0.3f, 0.35f, 0.4f, 1f);
    private static final com.badlogic.gdx.graphics.Color HOVER_COLOR =
            new com.badlogic.gdx.graphics.Color(0.4f, 0.5f, 0.6f, 1f);
    private static final com.badlogic.gdx.graphics.Color PRESS_COLOR =
            new com.badlogic.gdx.graphics.Color(0.2f, 0.25f, 0.3f, 1f);
    private static final com.badlogic.gdx.graphics.Color TEXT_COLOR =
            new com.badlogic.gdx.graphics.Color(1f, 1f, 1f, 1f);

    /**
     * 构造按钮。
     *
     * @param label     按钮文本
     * @param font      字体
     * @param renderer  ShapeRenderer
     * @param onClick   点击回调
     */
    public ButtonActor(String label, BitmapFont font, ShapeRenderer renderer, Runnable onClick) {
        this.label = label;
        this.font = font;
        this.shapeRenderer = renderer;
        this.onClick = onClick;
        setSize(BUTTON_WIDTH, BUTTON_HEIGHT);

        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                pressed = true;
                if (ButtonActor.this.onClick != null) {
                    ButtonActor.this.onClick.run();
                }
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                hovered = true;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                hovered = false;
                pressed = false;
            }
        });
    }

    /** 设置按钮文本 */
    public void setLabel(String label) { this.label = label; }

    /** 设置点击回调 */
    public void setOnClick(Runnable onClick) { this.onClick = onClick; }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        com.badlogic.gdx.graphics.Color color;
        if (pressed) {
            color = PRESS_COLOR;
        } else if (hovered) {
            color = HOVER_COLOR;
        } else {
            color = NORMAL_COLOR;
        }
        com.badlogic.gdx.graphics.Color old = batch.getColor().cpy();
        batch.setColor(color);
        batch.draw(UiStyles.pixelTexture(), getX(), getY(), getWidth(), getHeight());
        batch.setColor(old);

        // 绘制按钮文本（居中）
        float textWidth = font.draw(batch, label, getX(), getY()).width;
        font.draw(batch, label,
                getX() + (getWidth() - textWidth) / 2,
                getY() + getHeight() / 2 + 5);
    }
}
