package com.fabricatedbook.view.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.engine.CombatEngine;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.actor.CardActor;
import com.fabricatedbook.view.screen.BattleScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * HandPanel — 手牌面板
 * <p>
 * 在战斗界面底部显示玩家的手牌列表。
 * 支持选择卡牌并使用。
 * <p>
 * 引用方：BattleScreen（展示手牌交互）
 */
public class HandPanel extends Group {

    private final FabricBookGame game;
    private final CombatEngine combatEngine;
    private final BattleScreen battleScreen;
    private final List<CardActor> cardActors;
    private CardActor selectedCard;
    private TextButton endTurnBtn;

    private BitmapFont font;
    private ShapeRenderer shapeRenderer;

    private static final float PANEL_Y = 10;
    private static final float CARD_GAP = 15;

    /**
     * 构造手牌面板。
     *
     * @param game          游戏主类
     * @param combatEngine  战斗引擎
     * @param battleScreen  战斗画面
     */
    public HandPanel(FabricBookGame game, CombatEngine combatEngine,
                     BattleScreen battleScreen) {
        this.game = game;
        this.combatEngine = combatEngine;
        this.battleScreen = battleScreen;
        this.font = game.getFont();
        this.shapeRenderer = new ShapeRenderer();
        this.cardActors = new ArrayList<>();
        this.selectedCard = null;

        // 结束回合按钮
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        endTurnBtn = new TextButton("结束回合", buttonStyle);
        endTurnBtn.setPosition(FabricBookGame.SCREEN_WIDTH - 150, 30);
        endTurnBtn.setSize(130, 50);
        endTurnBtn.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                combatEngine.endRound();
                update();
            }
        });
        addActor(endTurnBtn);
    }

    /**
     * 更新手牌显示。
     */
    public void update() {
        // 清除旧的卡牌 Actor
        for (CardActor ca : cardActors) {
            removeActor(ca);
        }
        cardActors.clear();

        // 获取当前手牌
        Player player = combatEngine.getPlayer();
        List<Card> hand = player.getHand();
        List<Enemy> enemies = combatEngine.getAliveEnemyList();

        // 重新创建卡牌 Actor
        float totalWidth = hand.size() * CardActor.CARD_WIDTH
                + (hand.size() - 1) * CARD_GAP;
        float startX = (FabricBookGame.SCREEN_WIDTH - totalWidth) / 2f;

        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            CardActor actor = new CardActor(card, font, shapeRenderer);

            final int index = i;
            actor.setOnClick(() -> {
                // 使用卡牌
                Enemy target = enemies.isEmpty() ? null : enemies.get(0);
                boolean success = combatEngine.playCard(card, target);
                if (success) {
                    update();
                }
            });

            actor.setPosition(startX + i * (CardActor.CARD_WIDTH + CARD_GAP),
                    PANEL_Y);
            addActor(actor);
            cardActors.add(actor);
        }
    }
}
