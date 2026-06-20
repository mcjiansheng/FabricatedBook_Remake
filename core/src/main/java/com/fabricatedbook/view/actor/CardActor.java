package com.fabricatedbook.view.actor;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.math.Vector2;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.ui.UiStyles;

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
    private boolean draggingEnabled;
    private String previewDescription;
    private String unavailableReason;
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
        draggingEnabled = true;
        previewDescription = null;
        unavailableReason = null;

        // 根据卡牌类型加载图标
        String iconFile = "img/ukn.png";
        if (card.getType() == Card.CardType.ATTACK) iconFile = "img/atk.png";
        else if (card.getType() == Card.CardType.DEFENSE) iconFile = "img/def.png";
        else if (card.getType() == Card.CardType.SKILL) iconFile = "img/inc.png";
        else if (card.getType() == Card.CardType.ABILITY) iconFile = "img/inc.png";
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
                if (!draggingEnabled) {
                    return false;
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

    public void setDraggingEnabled(boolean draggingEnabled) {
        this.draggingEnabled = draggingEnabled;
    }

    /** Updates the card's visual and input state without embedding combat rules in the actor. */
    public void setUnavailableReason(String unavailableReason) {
        this.unavailableReason = unavailableReason;
        setDraggingEnabled(unavailableReason == null || unavailableReason.isBlank());
    }

    /** 获取卡牌数据 */
    public Card getCard() { return card; }

    public boolean isDragging() { return dragging; }

    public void setPreviewDescription(String previewDescription) {
        this.previewDescription = previewDescription;
    }

    public void clearPreviewDescription() {
        this.previewDescription = null;
    }

    public void returnHome() {
        dragging = false;
        clearPreviewDescription();
        setScale(hovered ? HOVER_SCALE : 1f);
        setPosition(homeX, homeY);
        if (interactionHandler != null) {
            interactionHandler.onCardDragCancelled(this);
        }
    }

    public void finishDragAsPlayed() {
        dragging = false;
        clearPreviewDescription();
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

        // Keep card chrome inside the Stage batch so dragging does not split rendering.
        drawRect(batch, BG_COLOR, drawX, drawY, visualWidth, visualHeight);
        if (unavailableReason != null && !unavailableReason.isBlank()) {
            drawRect(batch, new Color(0.08f, 0.08f, 0.08f, 0.52f), drawX, drawY, visualWidth, visualHeight);
        }
        if (dragging) {
            drawFrame(batch, SELECTED_COLOR, drawX - 3, drawY - 3, visualWidth + 6, visualHeight + 6, 3);
        }
        drawFrame(batch, borderColor(), drawX, drawY, visualWidth, visualHeight, 1);
        drawFrame(batch, borderColor(), drawX + 3, drawY + 3, visualWidth - 6, visualHeight - 6, 1);
        Color oldColor = font.getColor().cpy();
        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;
        font.setColor(0.12f, 0.10f, 0.08f, 1f);
        font.getData().setScale(FabricBookGame.uiFontScale(0.78f * scale));

        // 绘制类型图标
        if (typeIcon != null) {
            batch.draw(typeIcon, drawX + 5 * scale, drawY + visualHeight - 25 * scale,
                    20 * scale, 20 * scale);
        }

        // 绘制卡牌名称
        font.draw(batch, card.getName(), drawX + 32 * scale, drawY + visualHeight - 10 * scale);

        font.getData().setScale(FabricBookGame.uiFontScale(0.60f * scale));
        font.draw(batch, typeLabel(card.getType()) + " · " + rarityLabel(card.getRarity()),
                drawX + 32 * scale, drawY + visualHeight - 27 * scale);
        if (card.isUpgraded()) {
            font.setColor(Color.GOLD);
            font.draw(batch, "已升级", drawX + visualWidth - 48 * scale,
                    drawY + visualHeight - 10 * scale);
            font.setColor(0.12f, 0.10f, 0.08f, 1f);
        }
        font.getData().setScale(FabricBookGame.uiFontScale(0.78f * scale));

        // 绘制消耗
        font.draw(batch, "用 " + (card.getCost() < 0 ? "X" : card.getCost()), drawX + 8 * scale,
                drawY + visualHeight - 42 * scale);

        // 绘制描述
        String desc = previewDescription != null ? previewDescription : card.getDescription();
        if (desc != null) {
            float lineY = drawY + visualHeight - 72 * scale;
            for (String line : wrapLines(desc, (CARD_WIDTH - 16) * scale, 4)) {
                font.draw(batch, line, drawX + 8 * scale, lineY);
                lineY -= 18 * scale;
            }
        }

        if (unavailableReason != null && !unavailableReason.isBlank()) {
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, unavailableReason, drawX + 8 * scale, drawY + 14 * scale);
        } else if (card.getValue() > 0) {
            font.draw(batch, "值 " + card.getValue(), drawX + 8 * scale, drawY + 14 * scale);
        }
        font.setColor(oldColor);
        font.getData().setScale(oldScaleX, oldScaleY);
    }

    private void drawRect(Batch batch, Color color, float x, float y, float width, float height) {
        Color old = batch.getColor().cpy();
        batch.setColor(color);
        batch.draw(UiStyles.pixelTexture(), x, y, width, height);
        batch.setColor(old);
    }

    private void drawFrame(Batch batch, Color color, float x, float y, float width, float height, float line) {
        drawRect(batch, color, x, y, width, line);
        drawRect(batch, color, x, y + height - line, width, line);
        drawRect(batch, color, x, y, line, height);
        drawRect(batch, color, x + width - line, y, line, height);
    }

    private String[] wrapLines(String text, float maxWidth, int maxLines) {
        String normalized = text.trim();
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        int index = 0;
        GlyphLayout layout = new GlyphLayout();
        while (index < normalized.length() && lines.size() < maxLines) {
            char next = normalized.charAt(index++);
            if (next == '\n') {
                lines.add(line.toString());
                line.setLength(0);
                continue;
            }
            line.append(next);
            layout.setText(font, line);
            if (layout.width > maxWidth && line.length() > 1) {
                line.setLength(line.length() - 1);
                lines.add(line.toString());
                line.setLength(0);
                line.append(next);
            }
        }
        if (line.length() > 0 && lines.size() < maxLines) {
            lines.add(line.toString());
        }
        if (index < normalized.length() && !lines.isEmpty()) {
            int last = lines.size() - 1;
            String current = lines.get(last);
            while (!current.isEmpty()) {
                layout.setText(font, current + "...");
                if (layout.width <= maxWidth) break;
                current = current.substring(0, current.length() - 1);
            }
            lines.set(last, current + "...");
        }
        return lines.toArray(new String[0]);
    }

    private Color borderColor() {
        return switch (card.getType()) {
            case ATTACK -> Color.RED;
            case DEFENSE -> new Color(0.42f, 0.42f, 1f, 1f);
            case SKILL -> new Color(0.35f, 0.95f, 0.40f, 1f);
            case ABILITY -> Color.GOLD;
            case EQUIP -> Color.GOLD;
            case STATUS -> Color.GRAY;
            case CURSE -> Color.PURPLE;
            case TASK -> Color.CYAN;
        };
    }

    private String typeLabel(Card.CardType type) {
        return switch (type) {
            case ATTACK -> "攻击";
            case DEFENSE -> "防御";
            case SKILL -> "技能";
            case ABILITY -> "能力";
            case EQUIP -> "装备";
            case STATUS -> "状态";
            case CURSE -> "诅咒";
            case TASK -> "任务";
        };
    }

    private String rarityLabel(Card.Rarity rarity) {
        return switch (rarity) {
            case BASIC -> "基础";
            case COMMON -> "普通";
            case UNCOMMON -> "罕见";
            case RARE -> "稀有";
            case EPIC -> "史诗";
            case LEGENDARY -> "传说";
        };
    }

    /**
     * 释放纹理资源。
     * 在 Screen.hide() 或 dispose() 中调用。
     */
    public void dispose() {
        if (typeIcon != null) typeIcon.dispose();
    }
}
