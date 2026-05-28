package com.fabricatedbook.view.actor;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.fabricatedbook.core.card.Card;

/**
 * CardActor — 卡牌 Actor
 * <p>
 * 在战斗界面中展示一张卡牌，支持点击选择。
 * 显示卡牌名称、消耗、效果描述和类型图标。
 * <p>
 * 引用方：HandPanel（持有 CardActor 列表）、BattleScreen（卡牌交互）
 * 资源文件：img/{atk,def,ukn}.png（卡牌类型图标）
 */
public class CardActor extends Actor {

    private final Card card;
    private final BitmapFont font;
    private final ShapeRenderer shapeRenderer;
    private Texture cardBg;
    private Texture typeIcon;
    private boolean selected;
    private Runnable onClick;

    public static final float CARD_WIDTH = 120;
    public static final float CARD_HEIGHT = 170;

    // 颜色常量
    private static final com.badlogic.gdx.graphics.Color BG_COLOR =
            new com.badlogic.gdx.graphics.Color(0.95f, 0.93f, 0.85f, 1f);
    private static final com.badlogic.gdx.graphics.Color BORDER_COLOR =
            new com.badlogic.gdx.graphics.Color(0.4f, 0.3f, 0.2f, 1f);
    private static final com.badlogic.gdx.graphics.Color SELECTED_COLOR =
            new com.badlogic.gdx.graphics.Color(1f, 0.8f, 0.2f, 0.5f);

    /**
     * 构造卡牌 Actor。
     *
     * @param card     卡牌数据
     * @param font     字体
     * @param renderer ShapeRenderer
     */
    public CardActor(Card card, BitmapFont font, ShapeRenderer renderer) {
        this.card = card;
        this.font = font;
        this.shapeRenderer = renderer;
        setSize(CARD_WIDTH, CARD_HEIGHT);

        // 根据卡牌类型加载图标
        String iconFile = "img/ukn.png";
        if (card.getType() == Card.CardType.ATTACK) iconFile = "img/atk.png";
        else if (card.getType() == Card.CardType.DEFENSE) iconFile = "img/def.png";
        try {
            typeIcon = new Texture(iconFile);
        } catch (Exception e) {
            typeIcon = null;
        }

        // 点击事件
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selected = !selected;
                if (onClick != null) onClick.run();
            }
        });
    }

    /** 设置点击回调 */
    public void setOnClick(Runnable onClick) { this.onClick = onClick; }

    /** 是否被选中 */
    public boolean isSelected() { return selected; }
    public void setSelected(boolean s) { selected = s; }

    /** 获取卡牌数据 */
    public Card getCard() { return card; }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.end();

        // 绘制背景圆角矩形
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BG_COLOR);
        shapeRenderer.rect(getX(), getY(), CARD_WIDTH, CARD_HEIGHT);
        if (selected) {
            shapeRenderer.setColor(SELECTED_COLOR);
            shapeRenderer.rect(getX() - 2, getY() - 2, CARD_WIDTH + 4, CARD_HEIGHT + 4);
        }
        shapeRenderer.end();

        // 绘制边框
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(BORDER_COLOR);
        shapeRenderer.rect(getX(), getY(), CARD_WIDTH, CARD_HEIGHT);
        shapeRenderer.end();

        batch.begin();

        // 绘制类型图标
        if (typeIcon != null) {
            batch.draw(typeIcon, getX() + 5, getY() + CARD_HEIGHT - 25, 20, 20);
        }

        // 绘制卡牌名称
        font.draw(batch, card.getName(), getX() + 5, getY() + CARD_HEIGHT - 8);

        // 绘制消耗
        font.draw(batch, "费:" + card.getCost(), getX() + 5, getY() + CARD_HEIGHT - 35);

        // 绘制描述（自动换行，最多显示4行）
        String desc = card.getDescription();
        if (desc != null) {
            String[] lines = desc.split("(?<=\\G.{12})");
            for (int i = 0; i < Math.min(lines.length, 4); i++) {
                font.draw(batch, lines[i], getX() + 5, getY() + CARD_HEIGHT - 55 - i * 18);
            }
        }

        // 绘制价值/稀有度
        if (card.getValue() > 0) {
            font.draw(batch, "★".repeat(card.getValue()), getX() + 5, getY() + 10);
        }
    }

    /**
     * 释放纹理资源。
     * 在 Screen.hide() 或 dispose() 中调用。
     */
    public void dispose() {
        if (typeIcon != null) typeIcon.dispose();
    }
}
