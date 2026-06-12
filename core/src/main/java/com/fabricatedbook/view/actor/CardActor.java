package com.fabricatedbook.view.actor;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.math.Vector2;
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
    private boolean hovered;
    private boolean dragging;
    private float homeX;
    private float homeY;
    private CardInteractionHandler interactionHandler;

    public static final float CARD_WIDTH = 120;
    public static final float CARD_HEIGHT = 170;
    private static final float HOVER_SCALE = 1.08f;
    private static final float DRAG_SCALE = 1.06f;

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

        addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer,
                              Actor fromActor) {
                if (!dragging) {
                    hovered = true;
                    setScale(HOVER_SCALE);
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer,
                             Actor toActor) {
                if (!dragging) {
                    hovered = false;
                    setScale(1f);
                }
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y,
                                     int pointer, int button) {
                if (button == com.badlogic.gdx.Input.Buttons.RIGHT && dragging) {
                    cancelDrag();
                    return true;
                }
                if (button != com.badlogic.gdx.Input.Buttons.LEFT) {
                    return false;
                }

                dragging = true;
                hovered = false;
                homeX = getX();
                homeY = getY();
                setScale(DRAG_SCALE);
                toFront();
                if (interactionHandler != null) {
                    interactionHandler.onCardDragStart(CardActor.this);
                }
                updateDragPosition(x, y);
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y,
                                     int pointer) {
                if (!dragging) return;
                updateDragPosition(x, y);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y,
                                int pointer, int button) {
                if (!dragging || button != com.badlogic.gdx.Input.Buttons.LEFT) {
                    return;
                }
                Vector2 stagePoint = localToStageCoordinates(new Vector2(x, y));
                boolean played = interactionHandler != null
                        && interactionHandler.onCardDragEnd(CardActor.this,
                        stagePoint.x, stagePoint.y);
                if (!played) {
                    returnHome();
                }
            }
        });
    }

    public interface CardInteractionHandler {
        void onCardDragStart(CardActor actor);
        void onCardDragged(CardActor actor, float stageX, float stageY);
        boolean onCardDragEnd(CardActor actor, float stageX, float stageY);
        void onCardDragCancelled(CardActor actor);
    }

    /** 设置拖拽交互回调 */
    public void setInteractionHandler(CardInteractionHandler handler) {
        this.interactionHandler = handler;
    }

    /** 获取卡牌数据 */
    public Card getCard() { return card; }

    public boolean isDragging() { return dragging; }

    public void returnHome() {
        dragging = false;
        setScale(hovered ? HOVER_SCALE : 1f);
        setPosition(homeX, homeY);
        if (interactionHandler != null) {
            interactionHandler.onCardDragCancelled(this);
        }
    }

    public void finishDragAsPlayed() {
        dragging = false;
        setScale(1f);
    }

    private void cancelDrag() {
        returnHome();
    }

    private void updateDragPosition(float localX, float localY) {
        Vector2 stagePoint = localToStageCoordinates(new Vector2(localX, localY));
        setPosition(stagePoint.x - CARD_WIDTH / 2f, stagePoint.y - CARD_HEIGHT / 2f);
        if (interactionHandler != null) {
            interactionHandler.onCardDragged(this, stagePoint.x, stagePoint.y);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float scale = getScaleX();
        float visualWidth = CARD_WIDTH * scale;
        float visualHeight = CARD_HEIGHT * scale;
        float drawX = getX() - (visualWidth - CARD_WIDTH) / 2f;
        float drawY = getY() - (visualHeight - CARD_HEIGHT) / 2f;

        batch.end();

        // 绘制背景圆角矩形
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BG_COLOR);
        shapeRenderer.rect(drawX, drawY, visualWidth, visualHeight);
        if (dragging) {
            shapeRenderer.setColor(SELECTED_COLOR);
            shapeRenderer.rect(drawX - 3, drawY - 3, visualWidth + 6, visualHeight + 6);
        }
        shapeRenderer.end();

        // 绘制边框
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(BORDER_COLOR);
        shapeRenderer.rect(drawX, drawY, visualWidth, visualHeight);
        shapeRenderer.end();

        batch.begin();
        Color oldColor = font.getColor().cpy();
        font.setColor(0.12f, 0.10f, 0.08f, 1f);

        // 绘制类型图标
        if (typeIcon != null) {
            batch.draw(typeIcon, drawX + 5 * scale, drawY + visualHeight - 25 * scale,
                    20 * scale, 20 * scale);
        }

        // 绘制卡牌名称
        font.draw(batch, card.getName(), drawX + 32 * scale, drawY + visualHeight - 10 * scale);

        // 绘制消耗
        font.draw(batch, "费用 " + card.getCost(), drawX + 8 * scale,
                drawY + visualHeight - 42 * scale);

        // 绘制描述
        String desc = card.getDescription();
        if (desc != null) {
            font.draw(batch, desc, drawX + 8 * scale, drawY + visualHeight - 72 * scale,
                    visualWidth - 16 * scale, com.badlogic.gdx.utils.Align.left, true);
        }

        // 绘制价值/稀有度
        if (card.getValue() > 0) {
            font.draw(batch, "价值 " + card.getValue(), drawX + 8 * scale,
                    drawY + 14 * scale);
        }
        font.setColor(oldColor);
    }

    /**
     * 释放纹理资源。
     * 在 Screen.hide() 或 dispose() 中调用。
     */
    public void dispose() {
        if (typeIcon != null) typeIcon.dispose();
    }
}
