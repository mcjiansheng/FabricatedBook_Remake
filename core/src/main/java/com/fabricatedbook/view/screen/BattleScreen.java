package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fabricatedbook.core.action.DamageAction;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.engine.CombatEngine;
import com.fabricatedbook.core.engine.ViewNotifier;
import com.fabricatedbook.core.action.CombatAction;
import com.fabricatedbook.core.entity.AbstractEntity;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.core.relic.RelicManager;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.actor.CardActor;
import com.fabricatedbook.view.actor.EnemyActor;
import com.fabricatedbook.view.actor.PlayerActor;
import com.fabricatedbook.view.ui.HandPanel;
import com.fabricatedbook.view.ui.EnergyBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * BattleScreen — 战斗画面
 * <p>
 * 显示战斗场景，包括玩家手牌、敌人、格挡/Buff/能量等状态。
 * 通过 ViewNotifier 接口接收战斗引擎的事件通知。
 * 实现用户交互：选择目标、使用卡牌、结束回合等。
 * <p>
 * 引用方：MapScreen（进入战斗节点时切换）、CombatEngine（战斗结算）
 */
public class BattleScreen implements Screen, ViewNotifier, CardActor.CardInteractionHandler {

    private final FabricBookGame game;
    private final CombatEngine combatEngine;
    private final Player player;
    private final List<Enemy> enemies;
    private final MapScreen returnMap;

    private Stage stage;
    private OrthographicCamera camera;

    // UI 组件
    private HandPanel handPanel;
    private EnergyBar energyBar;
    private PlayerActor playerActor;
    private List<EnemyActor> enemyActors;
    private Label statusLabel;
    private Label turnLabel;
    private Table potionTable;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    private boolean battleInitialized;
    private boolean resultShown;
    private final Random random;
    private int goldAtBattleStart;
    private CardActor activeDraggedCard;
    private EnemyActor highlightedEnemyActor;
    private boolean rewardCardChosen;
    private RelicManager relicManager;
    private String pendingExtraRewardsText;

    /**
     * 构造战斗画面。
     *
     * @param game         游戏主类
     * @param combatEngine 战斗引擎
     * @param player       玩家实体
     * @param enemies      敌人列表
     */
    public BattleScreen(FabricBookGame game, CombatEngine combatEngine,
                        Player player, List<Enemy> enemies) {
        this(game, combatEngine, player, enemies, null);
    }

    public BattleScreen(FabricBookGame game, CombatEngine combatEngine,
                        Player player, List<Enemy> enemies, MapScreen returnMap) {
        this.game = game;
        this.combatEngine = combatEngine != null ? combatEngine : new CombatEngine();
        this.player = player;
        this.enemies = enemies;
        this.returnMap = returnMap;
        this.enemyActors = new ArrayList<>();
        this.battleInitialized = false;
        this.resultShown = false;
        this.random = new Random();
        this.goldAtBattleStart = player.getGold();
        this.rewardCardChosen = false;
        this.relicManager = null;
        this.pendingExtraRewardsText = "";

        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT);
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        font = game.getFont();
        shapeRenderer = new ShapeRenderer();

        // 玩家角色
        playerActor = new PlayerActor(player, font, shapeRenderer);
        playerActor.setPosition(150, 230);
        stage.addActor(playerActor);

        // 敌人角色
        float enemyY = 245;
        float enemyGap = 190f;
        float totalEnemyWidth = enemies.size() * EnemyActor.ENEMY_WIDTH
                + Math.max(0, enemies.size() - 1) * (enemyGap - EnemyActor.ENEMY_WIDTH);
        float enemyX = Math.max(620, FabricBookGame.SCREEN_WIDTH - totalEnemyWidth - 90);
        for (int i = 0; i < enemies.size(); i++) {
            EnemyActor actor = new EnemyActor(enemies.get(i), font, shapeRenderer);
            actor.setPosition(enemyX + i * enemyGap, enemyY);
            stage.addActor(actor);
            enemyActors.add(actor);
        }

        // 手牌面板（底部）
        handPanel = new HandPanel(game, combatEngine, this);
        handPanel.setPosition(0, 0);
        stage.addActor(handPanel);

        // 能量条
        energyBar = new EnergyBar(player, game.getFont());
        energyBar.setPosition(FabricBookGame.SCREEN_WIDTH - 150,
                FabricBookGame.SCREEN_HEIGHT - 62);
        stage.addActor(energyBar);

        // 状态信息
        statusLabel = new Label("", new Label.LabelStyle(
                game.getFont(), com.badlogic.gdx.graphics.Color.WHITE));
        statusLabel.setPosition(FabricBookGame.SCREEN_WIDTH / 2f - 160,
                FabricBookGame.SCREEN_HEIGHT - 118);
        stage.addActor(statusLabel);

        // 回合信息
        turnLabel = new Label("回合 1", new Label.LabelStyle(
                game.getFont(), com.badlogic.gdx.graphics.Color.WHITE));
        turnLabel.setFontScale(1.5f);
        turnLabel.setPosition(FabricBookGame.SCREEN_WIDTH / 2f - 55,
                FabricBookGame.SCREEN_HEIGHT - 62);
        stage.addActor(turnLabel);

        potionTable = new Table();
        potionTable.setPosition(20, FabricBookGame.SCREEN_HEIGHT - 150);
        stage.addActor(potionTable);
        renderPotionButtons();

        if (!battleInitialized) {
            relicManager = new RelicManager(player);
            combatEngine.setRelicManager(relicManager);
            combatEngine.setViewNotifier(this);
            combatEngine.initBattle(player, enemies);
            battleInitialized = true;
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.12f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawBackground();
        updateEnemyVisibility();

        // UI 更新
        energyBar.update();
        handPanel.update();

        stage.act(delta);
        stage.draw();
    }

    // ==================== ViewNotifier 实现 ====================

    @Override
    public void onBattleStart(AbstractEntity playerEntity,
                              List<AbstractEntity> enemyEntities) {
        statusLabel.setText("战斗开始！");
    }

    @Override
    public void onActionExecuted(CombatAction action) {
        statusLabel.setText(action.getDescription());
        if (action instanceof DamageAction damageAction) {
            showDamageNumbers(damageAction);
        }
    }

    @Override
    public void onBattleEnd(boolean victory, String reward) {
        if (victory) {
            statusLabel.setText("胜利！" + (reward != null ? reward : ""));
            showRewardModal(reward);
        } else {
            statusLabel.setText("失败...");
            showDefeatModal();
        }
    }

    @Override
    public void onTurnStart(int turnNumber) {
        turnLabel.setText("回合 " + turnNumber);
        energyBar.update();
    }

    @Override
    public void onCardPlayed(CombatAction action) {
        statusLabel.setText("使用卡牌: " + action.getDescription());
    }

    // ==================== 卡牌拖拽交互 ====================

    @Override
    public void onCardDragStart(CardActor actor) {
        if (!combatEngine.isInBattle()) {
            actor.returnHome();
            return;
        }
        activeDraggedCard = actor;
        updateTargetHighlight(actor, actor.getX() + CardActor.CARD_WIDTH / 2f,
                actor.getY() + CardActor.CARD_HEIGHT / 2f);
    }

    @Override
    public void onCardDragged(CardActor actor, float stageX, float stageY) {
        if (actor != activeDraggedCard) return;
        updateTargetHighlight(actor, stageX, stageY);
    }

    @Override
    public boolean onCardDragEnd(CardActor actor, float stageX, float stageY) {
        if (actor != activeDraggedCard || !combatEngine.isInBattle()) {
            clearTargetHighlight();
            return false;
        }

        Card card = actor.getCard();
        Enemy target = null;
        if (requiresSingleEnemy(card)) {
            EnemyActor targetActor = enemyActorAt(stageX, stageY);
            if (targetActor == null) {
                statusLabel.setText("请选择一个敌人作为目标");
                clearTargetHighlight();
                return false;
            }
            target = targetActor.getEnemy();
        }

        boolean success = combatEngine.playCard(card, target);
        clearTargetHighlight();
        activeDraggedCard = null;
        if (success) {
            actor.finishDragAsPlayed();
            handPanel.forceRefresh();
            return true;
        }
        statusLabel.setText(player.getEnergy() < card.getCost()
                ? "能量不足" : "无法使用这张牌");
        return false;
    }

    @Override
    public void onCardDragCancelled(CardActor actor) {
        if (actor == activeDraggedCard) {
            activeDraggedCard = null;
        }
        clearTargetHighlight();
    }

    // ====== 生命周期 ======

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {
        Gdx.input.setInputProcessor(null);
    }
    @Override
    public void dispose() {
        stage.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }

    // ====== Getter ======

    public CombatEngine getCombatEngine() { return combatEngine; }
    public Player getPlayer() { return player; }
    public List<Enemy> getEnemies() { return enemies; }

    private void renderPotionButtons() {
        potionTable.clear();
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = game.getFont();
        for (int i = 0; i < player.getPotions().size(); i++) {
            final int index = i;
            Potion potion = player.getPotions().get(i);
            TextButton button = new TextButton(potion.getName(), style);
            button.addListener(new ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                    float x, float y) {
                    if (!combatEngine.isInBattle()) return;
                    Potion used = player.removePotion(index);
                    if (used != null && used.use(player, enemies, relicManager)) {
                        statusLabel.setText("使用药水：" + used.getName());
                        renderPotionButtons();
                    }
                }
            });
            potionTable.add(button).width(130).height(34).padRight(6);
        }
    }

    private void updateTargetHighlight(CardActor actor, float stageX, float stageY) {
        clearTargetHighlight();
        Card card = actor.getCard();
        if (targetsSelf(card)) {
            playerActor.setHighlighted(true);
            statusLabel.setText("");
        } else if (targetsAllEnemies(card)) {
            for (EnemyActor enemyActor : enemyActors) {
                if (enemyActor.getEnemy().isAlive()) {
                    enemyActor.setHighlighted(true);
                }
            }
            statusLabel.setText("");
        } else {
            EnemyActor targetActor = enemyActorAt(stageX, stageY);
            if (targetActor != null) {
                targetActor.setHighlighted(true);
                highlightedEnemyActor = targetActor;
                statusLabel.setText("目标：" + targetActor.getEnemy().getName());
            } else {
                statusLabel.setText("拖到敌人身上后松手打出");
            }
        }
    }

    private void clearTargetHighlight() {
        playerActor.setHighlighted(false);
        for (EnemyActor enemyActor : enemyActors) {
            enemyActor.setHighlighted(false);
        }
        highlightedEnemyActor = null;
    }

    private boolean targetsSelf(Card card) {
        return card.getTargetType() == Card.TargetType.SELF
                || card.getTargetType() == Card.TargetType.ALL_ALLIES;
    }

    private boolean targetsAllEnemies(Card card) {
        return card.getTargetType() == Card.TargetType.ALL_ENEMIES;
    }

    private boolean requiresSingleEnemy(Card card) {
        return card.getTargetType() == Card.TargetType.SINGLE_ENEMY
                || card.getTargetType() == Card.TargetType.ENEMY;
    }

    private EnemyActor enemyActorAt(float stageX, float stageY) {
        for (EnemyActor enemyActor : enemyActors) {
            if (!enemyActor.getEnemy().isAlive()) continue;
            if (!enemyActor.isVisible()) continue;
            if (stageX >= enemyActor.getX()
                    && stageX <= enemyActor.getX() + EnemyActor.ENEMY_WIDTH
                    && stageY >= enemyActor.getY()
                    && stageY <= enemyActor.getY() + EnemyActor.ENEMY_HEIGHT) {
                return enemyActor;
            }
        }
        return null;
    }

    private void showDamageNumbers(DamageAction damageAction) {
        for (Map.Entry<AbstractEntity, Integer> entry
                : damageAction.getFinalDamageByTarget().entrySet()) {
            AbstractEntity target = entry.getKey();
            int damage = entry.getValue();
            if (damage <= 0) continue;

            Actor anchor = actorForEntity(target);
            if (anchor == null) continue;

            Label number = new Label("-" + damage, new Label.LabelStyle(
                    game.getFont(), Color.SCARLET));
            number.setFontScale(1.45f);
            number.setPosition(anchor.getX() + anchor.getWidth() / 2f - 18,
                    anchor.getY() + anchor.getHeight() * 0.72f);
            number.addAction(Actions.sequence(
                    Actions.parallel(
                            Actions.moveBy(0, 42, 0.75f),
                            Actions.fadeOut(0.75f)
                    ),
                    Actions.removeActor()
            ));
            stage.addActor(number);
            number.toFront();
        }
    }

    private Actor actorForEntity(AbstractEntity entity) {
        if (entity == player) {
            return playerActor;
        }
        for (EnemyActor enemyActor : enemyActors) {
            if (enemyActor.getEnemy() == entity) {
                return enemyActor;
            }
        }
        return null;
    }

    private void drawBackground() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.13f, 0.13f, 0.17f, 1f);
        shapeRenderer.rect(0, 0, FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);
        shapeRenderer.setColor(0.08f, 0.08f, 0.11f, 1f);
        shapeRenderer.rect(0, 0, FabricBookGame.SCREEN_WIDTH, 205);
        shapeRenderer.setColor(0.07f, 0.07f, 0.10f, 1f);
        shapeRenderer.rect(0, FabricBookGame.SCREEN_HEIGHT - 90,
                FabricBookGame.SCREEN_WIDTH, 90);
        shapeRenderer.setColor(0.18f, 0.18f, 0.22f, 1f);
        shapeRenderer.rect(80, 205, FabricBookGame.SCREEN_WIDTH - 160, 4);
        shapeRenderer.end();
    }

    private void updateEnemyVisibility() {
        for (EnemyActor enemyActor : enemyActors) {
            boolean alive = enemyActor.getEnemy().isAlive();
            enemyActor.setVisible(alive);
            if (!alive) {
                enemyActor.setHighlighted(false);
            }
        }
    }

    private void showResultButton(boolean victory) {
        if (resultShown) return;
        resultShown = true;

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = game.getFont();
        TextButton button = new TextButton(victory ? "返回地图" : "重新开始", style);
        button.setPosition(FabricBookGame.SCREEN_WIDTH / 2f - 80, 210);
        button.setSize(160, 52);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                if (victory && returnMap != null) {
                    returnMap.completeCurrentNodeAndReturn();
                } else {
                    game.setScreen(new TitleScreen(game));
                }
            }
        });
        stage.addActor(button);
    }

    private void showDefeatModal() {
        if (resultShown) return;
        resultShown = true;
        clearTargetHighlight();

        Group modal = new Group();
        modal.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);
        stage.addActor(modal);

        Actor backdrop = new Actor() {
            @Override
            public void draw(com.badlogic.gdx.graphics.g2d.Batch batch,
                             float parentAlpha) {
                batch.end();
                shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(0f, 0f, 0f, 0.66f);
                shapeRenderer.rect(0, 0, FabricBookGame.SCREEN_WIDTH,
                        FabricBookGame.SCREEN_HEIGHT);
                shapeRenderer.setColor(0.12f, 0.11f, 0.09f, 0.96f);
                shapeRenderer.rect(315, 185, 650, 330);
                shapeRenderer.end();
                batch.begin();
            }
        };
        backdrop.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);
        modal.addActor(backdrop);

        Table table = new Table();
        table.setFillParent(true);
        table.center();
        modal.addActor(table);

        Label title = new Label("战斗失败", new Label.LabelStyle(game.getFont(), Color.GOLD));
        title.setFontScale(1.8f);
        table.add(title).padBottom(42);
        table.row();

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = game.getFont();
        style.fontColor = Color.WHITE;
        TextButton restart = new TextButton("重新开始", style);
        restart.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                game.setScreen(new TitleScreen(game));
            }
        });
        table.add(restart).width(220).height(54);
        modal.toFront();
    }

    private void showRewardModal(String rewardText) {
        if (resultShown) return;
        resultShown = true;
        clearTargetHighlight();

        Group modal = new Group();
        modal.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);
        stage.addActor(modal);

        Actor backdrop = new Actor() {
            @Override
            public void draw(com.badlogic.gdx.graphics.g2d.Batch batch,
                             float parentAlpha) {
                batch.end();
                shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(0f, 0f, 0f, 0.62f);
                shapeRenderer.rect(0, 0, FabricBookGame.SCREEN_WIDTH,
                        FabricBookGame.SCREEN_HEIGHT);
                shapeRenderer.setColor(0.78f, 0.78f, 0.78f, 1f);
                shapeRenderer.rect(170, 80, 940, 560);
                shapeRenderer.setColor(Color.WHITE);
                shapeRenderer.rect(510, 330, 260, 58);
                shapeRenderer.rect(510, 258, 260, 58);
                shapeRenderer.end();
                batch.begin();
            }
        };
        backdrop.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);
        modal.addActor(backdrop);

        Table table = new Table();
        table.setFillParent(true);
        table.top().padTop(150);
        modal.addActor(table);

        Label title = new Label("胜利", new Label.LabelStyle(
                game.getFont(), Color.GOLD));
        title.setFontScale(1.8f);
        table.add(title).padBottom(70);
        table.row();

        int gainedGold = Math.max(0, player.getGold() - goldAtBattleStart);
        Label goldLabel = new Label("金币：" + gainedGold,
                new Label.LabelStyle(game.getFont(), Color.GOLD));
        table.add(goldLabel).width(260).height(58).padBottom(14);
        table.row();

        Label cardLabel = new Label("一张卡牌",
                new Label.LabelStyle(game.getFont(), Color.BLACK));
        table.add(cardLabel).width(260).height(58).padBottom(14);
        table.row();

        pendingExtraRewardsText = applyExtraRewards();
        for (String extra : pendingExtraRewardsText.split("\\n")) {
            if (extra.isBlank()) {
                continue;
            }
            Label extraLabel = new Label(extra, new Label.LabelStyle(
                    game.getFont(), extra.startsWith("药水") ? Color.BLACK : Color.GOLD));
            table.add(extraLabel).width(300).height(34).padBottom(4);
            table.row();
        }

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = game.getFont();
        buttonStyle.fontColor = Color.WHITE;
        TextButton continueButton = new TextButton("继续", buttonStyle);
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                modal.remove();
                showCardRewardSelection();
            }
        });
        table.add(continueButton).width(220).height(52).padTop(42);
        modal.toFront();
    }

    private void showCardRewardSelection() {
        Group modal = new Group();
        modal.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);
        stage.addActor(modal);

        Actor backdrop = new Actor() {
            @Override
            public void draw(com.badlogic.gdx.graphics.g2d.Batch batch,
                             float parentAlpha) {
                batch.end();
                shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(0.78f, 0.78f, 0.78f, 1f);
                shapeRenderer.rect(0, 0, FabricBookGame.SCREEN_WIDTH,
                        FabricBookGame.SCREEN_HEIGHT);
                shapeRenderer.end();
                batch.begin();
            }
        };
        backdrop.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);
        modal.addActor(backdrop);

        Table table = new Table();
        table.setFillParent(true);
        table.top().padTop(85);
        modal.addActor(table);

        Label title = new Label("请选择想要的卡牌", new Label.LabelStyle(
                game.getFont(), Color.BLACK));
        title.setFontScale(1.9f);
        table.add(title).colspan(3).padBottom(45);
        table.row();

        List<Card> rewards = rollCardRewards();
        for (Card card : rewards) {
            CardActor cardActor = new CardActor(card, game.getFont(), shapeRenderer);
            cardActor.setDraggingEnabled(false);
            cardActor.addListener(new ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                    float x, float y) {
                    if (rewardCardChosen) return;
                    rewardCardChosen = true;
                    player.getDrawPile().add(CardFactory.createFromTemplate(card));
                    statusLabel.setText("获得卡牌：" + card.getName());
                    returnToMapAfterReward();
                }
            });
            table.add(cardActor).width(CardActor.CARD_WIDTH)
                    .height(CardActor.CARD_HEIGHT).pad(22);
        }
        table.row();

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = game.getFont();
        buttonStyle.fontColor = Color.WHITE;
        TextButton skipCard = new TextButton("跳过", buttonStyle);
        skipCard.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                rewardCardChosen = true;
                returnToMapAfterReward();
            }
        });
        table.add(skipCard).colspan(3).width(150).height(48).padTop(36);
        modal.toFront();
    }

    private void returnToMapAfterReward() {
        if (returnMap != null) {
            returnMap.completeCurrentNodeAndReturn();
        } else {
            game.setScreen(new TitleScreen(game));
        }
    }

    private List<Card> rollCardRewards() {
        List<Card> pool = CardPool.getCardsByProfession(
                player.getProfession().name().toLowerCase()).stream()
                .filter(card -> card.getRarity() != Card.Rarity.BASIC)
                .toList();
        if (pool.isEmpty()) {
            pool = CardPool.getCardsByProfession(player.getProfession().name().toLowerCase());
        }
        return CardPool.randomSelect(pool, Math.min(3, pool.size()));
    }

    private String applyExtraRewards() {
        StringBuilder sb = new StringBuilder();
        if (random.nextFloat() < 0.35f) {
            Relic relic = RelicFactory.randomRelic(player, false);
            if (relic != null) {
                new RelicManager(player).addRelic(relic);
                sb.append("藏品：").append(relic.getName());
            }
        }
        if (random.nextFloat() < 0.45f) {
            List<Potion> potions = new com.fabricatedbook.data.DataLoader().loadPotions();
            if (!potions.isEmpty() && player.canAddPotion()) {
                Potion potion = potions.get(random.nextInt(potions.size())).copy();
                player.addPotion(potion);
                if (!sb.isEmpty()) {
                    sb.append("\n");
                }
                sb.append("药水：").append(potion.getName());
            }
        }
        return sb.toString();
    }
}
