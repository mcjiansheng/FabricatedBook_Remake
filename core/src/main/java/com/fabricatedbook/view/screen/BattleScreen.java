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
import com.badlogic.gdx.utils.Align;
import com.fabricatedbook.core.action.DamageAction;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.card.CardFactory;
import com.fabricatedbook.core.card.CardPool;
import com.fabricatedbook.core.engine.CombatEngine;
import com.fabricatedbook.core.engine.CombatPreviewCalculator;
import com.fabricatedbook.core.engine.CardPreview;
import com.fabricatedbook.core.engine.EnemyIntentPreview;
import com.fabricatedbook.core.engine.ViewNotifier;
import com.fabricatedbook.core.action.CombatAction;
import com.fabricatedbook.core.entity.AbstractEntity;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.potion.Potion;
import com.fabricatedbook.core.relic.Relic;
import com.fabricatedbook.core.relic.RelicFactory;
import com.fabricatedbook.core.relic.RelicManager;
import com.fabricatedbook.core.run.GameRunState;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.actor.CardActor;
import com.fabricatedbook.view.actor.EnemyActor;
import com.fabricatedbook.view.actor.PlayerActor;
import com.fabricatedbook.view.ui.HandPanel;
import com.fabricatedbook.view.ui.EnergyBar;
import com.fabricatedbook.view.ui.EscapeMenu;
import com.fabricatedbook.view.ui.ResponsiveViewport;
import com.fabricatedbook.view.ui.UiStyles;
import com.fabricatedbook.view.ui.UiModal;
import com.fabricatedbook.view.ui.GameHud;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
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

    private static final int UPGRADED_REWARD_CHANCE_PERCENT = 10;

    private final FabricBookGame game;
    private final CombatEngine combatEngine;
    private final Player player;
    private final List<Enemy> enemies;
    private final MapScreen returnMap;
    private final GameRunState runState;

    private Stage stage;
    private OrthographicCamera camera;

    // UI 组件
    private HandPanel handPanel;
    private EnergyBar energyBar;
    private PlayerActor playerActor;
    private List<EnemyActor> enemyActors;
    private Label statusLabel;
    private Label turnLabel;
    private Label combatLogLabel;
    private final Deque<String> combatLogEntries;
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
    private Group rewardModal;
    private Group escapeMenu;
    private List<RewardEntry> pendingRewards;

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
        this.runState = game.getCurrentRun();
        this.enemyActors = new ArrayList<>();
        this.battleInitialized = false;
        this.resultShown = false;
        this.random = runState != null && runState.getActiveNode() != null
                ? runState.randomFor("reward:" + runState.getActiveNode().layer
                + ":" + runState.getActiveNode().col + ":" + runState.getActiveNode().row)
                : new Random();
        this.goldAtBattleStart = player.getGold();
        this.rewardCardChosen = false;
        this.relicManager = null;
        this.rewardModal = null;
        this.pendingRewards = new ArrayList<>();
        this.combatLogEntries = new ArrayDeque<>();

        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT);
    }

    @Override
    public void show() {
        stage = new Stage(ResponsiveViewport.create());
        camera = (OrthographicCamera) stage.getCamera();
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
            actor.setIntentPreviewProvider(this::previewEnemyIntentDetail);
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
        energyBar.setPosition(FabricBookGame.SCREEN_WIDTH / 2f - 55, 214);
        stage.addActor(energyBar);

        // 状态信息
        statusLabel = new Label("", new Label.LabelStyle(
                game.getFont(), com.badlogic.gdx.graphics.Color.WHITE));
        statusLabel.setPosition(FabricBookGame.SCREEN_WIDTH / 2f - 160,
                FabricBookGame.SCREEN_HEIGHT - 142);
        stage.addActor(statusLabel);

        Table logPanel = new Table();
        logPanel.setBackground(UiStyles.panelSurface());
        logPanel.pad(10);
        logPanel.setSize(350, 92);
        logPanel.setPosition(20, 530);
        combatLogLabel = new Label("", new Label.LabelStyle(game.getFont(), Color.LIGHT_GRAY));
        combatLogLabel.setWrap(true);
        logPanel.add(combatLogLabel).width(330).left().top();
        stage.addActor(logPanel);

        // 回合信息
        turnLabel = new Label("回合 1", new Label.LabelStyle(
                game.getFontForScale(1.5f), com.badlogic.gdx.graphics.Color.WHITE));
        turnLabel.setPosition(FabricBookGame.SCREEN_WIDTH / 2f - 55,
                FabricBookGame.SCREEN_HEIGHT - 102);
        stage.addActor(turnLabel);

        new GameHud(stage, game, player,
                () -> returnMap != null ? returnMap.currentLayerStatusText() : "第" + player.getCurrentFloor() + "层",
                () -> game.setScreen(new InventoryScreen(game, player, returnMap, InventoryScreen.Tab.CARDS)),
                () -> game.setScreen(new InventoryScreen(game, player, returnMap, InventoryScreen.Tab.RELICS)),
                true, potion -> potion.use(player, enemies, relicManager),
                () -> statusLabel.setText("药水栏已更新。"));

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
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            toggleEscapeMenu();
        }
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.12f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.getViewport().apply();
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
        recordCombatLog("战斗开始");
    }

    @Override
    public void onActionExecuted(CombatAction action) {
        statusLabel.setText(action.getDescription());
        recordCombatLog(action.getDescription());
        if (action instanceof DamageAction damageAction) {
            showDamageNumbers(damageAction);
        }
    }

    @Override
    public void onBattleEnd(boolean victory, String reward) {
        if (victory) {
            statusLabel.setText("胜利！" + (reward != null ? reward : ""));
            recordCombatLog("战斗胜利");
            showRewardModal(reward);
        } else {
            statusLabel.setText("失败...");
            recordCombatLog("战斗失败");
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
        if (card.isUnplayable()) {
            statusLabel.setText("无法打出");
            clearTargetHighlight();
            activeDraggedCard = null;
            return false;
        }
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
    @Override public void pause() { game.autosaveCurrentRun(); }
    @Override public void resume() {}
    @Override public void hide() {
        game.autosaveCurrentRun();
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

    private void updateTargetHighlight(CardActor actor, float stageX, float stageY) {
        clearTargetHighlight();
        Card card = actor.getCard();
        if (targetsSelf(card)) {
            playerActor.setHighlighted(true);
            applyCardPreview(actor, null);
            statusLabel.setText("");
        } else if (targetsAllEnemies(card)) {
            for (EnemyActor enemyActor : enemyActors) {
                if (enemyActor.getEnemy().isAlive()) {
                    enemyActor.setHighlighted(true);
                }
            }
            applyCardPreview(actor, null);
            statusLabel.setText("");
        } else {
            EnemyActor targetActor = enemyActorAt(stageX, stageY);
            if (targetActor != null) {
                targetActor.setHighlighted(true);
                highlightedEnemyActor = targetActor;
                applyCardPreview(actor, targetActor.getEnemy());
                statusLabel.setText("目标：" + targetActor.getEnemy().getName());
            } else {
                actor.clearPreviewDescription();
                statusLabel.setText("拖到敌人身上后松手打出");
            }
        }
    }

    private void clearTargetHighlight() {
        playerActor.setHighlighted(false);
        for (EnemyActor enemyActor : enemyActors) {
            enemyActor.setHighlighted(false);
        }
        if (activeDraggedCard != null) {
            activeDraggedCard.clearPreviewDescription();
        }
        highlightedEnemyActor = null;
    }

    private void applyCardPreview(CardActor actor, Enemy target) {
        CardPreview preview = CombatPreviewCalculator.previewCard(actor.getCard(), player,
                enemies, target, relicManager);
        if (preview.hasPreview()) {
            actor.setPreviewDescription(preview.getDescription());
        } else {
            actor.clearPreviewDescription();
        }
    }

    private EnemyIntentPreview previewEnemyIntentDetail(Enemy enemy) {
        String actionId = combatEngine.getPreviewActionId(enemy);
        return CombatPreviewCalculator.previewEnemyIntent(enemy, player,
                enemies, relicManager, actionId);
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
                    game.getFontForScale(1.45f), Color.SCARLET));
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

    private void recordCombatLog(String message) {
        if (message == null || message.isBlank()) return;
        String line = message.length() > 46 ? message.substring(0, 43) + "..." : message;
        combatLogEntries.addFirst("· " + line);
        while (combatLogEntries.size() > 3) combatLogEntries.removeLast();
        if (combatLogLabel != null) {
            combatLogLabel.setText(String.join("\n", combatLogEntries));
        }
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

        TextButton.TextButtonStyle style = UiStyles.buttonStyle(game);
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

        Group modal = UiModal.open(stage);
        Table table = UiModal.panel(650, 330);
        table.center();
        modal.addActor(table);

        Label title = new Label("战斗失败", new Label.LabelStyle(
                game.getFontForScale(1.8f), Color.GOLD));
        table.add(title).padBottom(42);
        table.row();

        TextButton.TextButtonStyle style = UiStyles.buttonStyle(game);
        TextButton restart = new TextButton("重新开始", style);
        restart.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                game.abandonCurrentRun();
                game.setScreen(new TitleScreen(game));
            }
        });
        table.add(restart).width(220).height(54);
    }

    private void showRewardModal(String rewardText) {
        if (resultShown) return;
        resultShown = true;
        clearTargetHighlight();

        prepareRewardEntries();
        showRewardClaimModal();
    }

    private void prepareRewardEntries() {
        pendingRewards.clear();
        rewardCardChosen = false;

        int gainedGold = Math.max(0, player.getGold() - goldAtBattleStart);
        if (gainedGold > 0) {
            player.setGold(player.getGold() - gainedGold);
            pendingRewards.add(new RewardEntry("金币：" + gainedGold, Color.GOLD,
                    () -> player.gainGold(gainedGold)));
        }

        pendingRewards.add(new RewardEntry("卡牌奖励", Color.BLACK,
                this::showCardRewardSelection, true));

        int relicChance = new RelicManager(player).modifyRelicRewardChance(35);
        if (rewardRandom("relic-chance").nextInt(100) < relicChance) {
            Relic relic = RelicFactory.randomRelic(player, false,
                    rewardRandom("relic-pick"));
            if (relic != null) {
                pendingRewards.add(new RewardEntry("藏品：" + relic.getName(), Color.GOLD,
                        () -> new RelicManager(player).addRelic(relic)));
            }
        }

        if (rewardRandom("potion-chance").nextFloat() < 0.45f) {
            List<Potion> potions = new com.fabricatedbook.data.DataLoader().loadPotions();
            if (!potions.isEmpty()) {
                Potion potion = potions.get(rewardRandom("potion-pick")
                        .nextInt(potions.size())).copy();
                pendingRewards.add(new RewardEntry("药水：" + potion.getName(), Color.BLACK,
                        () -> player.addPotion(potion), false, potion));
            }
        }
    }

    /** Frontend-only preview for the reward flow when the potion bar is full. */
    public void showDebugPotionReward() {
        pendingRewards.clear();
        List<Potion> potions = new com.fabricatedbook.data.DataLoader().loadPotions();
        if (potions.isEmpty()) return;
        Potion potion = potions.get(0).copy();
        pendingRewards.add(new RewardEntry("药水：" + potion.getName(), Color.BLACK,
                () -> player.addPotion(potion), false, potion));
        showRewardClaimModal();
    }

    /** Opens the defeat modal without mutating run data; used only by frontend QA. */
    public void showDebugDefeat() {
        showDefeatModal();
    }

    private void showRewardClaimModal() {
        if (rewardModal != null) {
            rewardModal.remove();
        }

        Group modal = UiModal.open(stage);
        rewardModal = modal;
        Table table = UiModal.panel(940, 560, UiStyles.lightPanelSurface());
        table.top().padTop(45);
        modal.addActor(table);

        Label title = new Label("胜利", new Label.LabelStyle(
                game.getFontForScale(1.8f), Color.GOLD));
        title.setAlignment(Align.center);
        table.add(title).width(520).height(70).padBottom(34);
        table.row();

        for (RewardEntry entry : pendingRewards) {
            TextButton rewardButton = rewardButton(entry);
            table.add(rewardButton).width(520).height(62).padBottom(14);
            table.row();
        }

        TextButton.TextButtonStyle buttonStyle = UiStyles.buttonStyle(game);
        TextButton continueButton = new TextButton("离开奖励", buttonStyle);
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                rewardModal = null;
                modal.remove();
                returnToMapAfterReward();
            }
        });
        table.add(continueButton).width(300).height(54).padTop(30);
        modal.toFront();
    }

    private TextButton rewardButton(RewardEntry entry) {
        TextButton.TextButtonStyle style = UiStyles.buttonStyle(game);
        String text = (entry.claimed ? "已领取  " : "领取  ") + entry.label;
        TextButton button = new TextButton(text, style);
        button.getLabel().setAlignment(Align.left);
        button.getLabel().setColor(entry.claimed ? Color.LIGHT_GRAY : entry.color);
        button.getLabelCell().padLeft(24);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                if (entry.claimed) {
                    return;
                }
                if (entry.opensCardSelection) {
                    entry.claimAction.run();
                    return;
                }
                if (entry.potionReward != null && !player.canAddPotion()) {
                    showPotionDiscardSelection(entry);
                    return;
                }
                entry.claimAction.run();
                entry.claimed = true;
                commitRewardPhase();
                game.autosaveCurrentRun();
                statusLabel.setText("获得：" + entry.label);
                showRewardClaimModal();
            }
        });
        return button;
    }

    private void showCardRewardSelection() {
        if (rewardModal != null) {
            rewardModal.remove();
            rewardModal = null;
        }

        Group modal = UiModal.open(stage);
        Table table = UiModal.panel(1100, 600, UiStyles.lightPanelSurface());
        table.top().padTop(38);
        modal.addActor(table);

        Label title = new Label("请选择想要的卡牌", new Label.LabelStyle(
                game.getFontForScale(1.9f), Color.BLACK));
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
                    markCardRewardClaimed();
                    commitRewardPhase();
                    game.autosaveCurrentRun();
                    modal.remove();
                    showRewardClaimModal();
                }
            });
            table.add(cardActor).width(CardActor.CARD_WIDTH)
                    .height(CardActor.CARD_HEIGHT).pad(22);
        }
        table.row();

        TextButton.TextButtonStyle buttonStyle = UiStyles.buttonStyle(game);
        TextButton skipCard = new TextButton("跳过", buttonStyle);
        skipCard.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                rewardCardChosen = true;
                markCardRewardClaimed();
                commitRewardPhase();
                game.autosaveCurrentRun();
                modal.remove();
                showRewardClaimModal();
            }
        });
        table.add(skipCard).colspan(3).width(150).height(48).padTop(36);
        modal.toFront();
    }

    private void markCardRewardClaimed() {
        for (RewardEntry entry : pendingRewards) {
            if (entry.opensCardSelection) {
                entry.claimed = true;
                return;
            }
        }
    }

    private boolean allRewardsClaimed() {
        for (RewardEntry entry : pendingRewards) {
            if (!entry.claimed) return false;
        }
        return true;
    }

    private void showPotionDiscardSelection(RewardEntry entry) {
        // Kept in the reward flow so a full potion bar never silently loses a reward.
        if (rewardModal != null) rewardModal.remove();
        Group modal = UiModal.open(stage);
        rewardModal = modal;
        float panelHeight = 210 + player.getPotions().size() * 56f;
        Table table = UiModal.panel(480, panelHeight);
        table.top().padTop(28);
        modal.addActor(table);
        table.add(new Label("药水栏已满：选择一瓶丢弃", new Label.LabelStyle(
                game.getFontForScale(1.4f), Color.GOLD))).padBottom(20);
        table.row();
        for (int i = 0; i < player.getPotions().size(); i++) {
            final int index = i;
            Potion existing = player.getPotions().get(i);
            TextButton discard = new TextButton("丢弃 " + existing.getName(), UiStyles.buttonStyle(game));
            discard.addListener(new ClickListener() {
                @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    player.removePotion(index);
                    entry.claimAction.run();
                    entry.claimed = true;
                    commitRewardPhase();
                    game.autosaveCurrentRun();
                    statusLabel.setText("获得：" + entry.label);
                    showRewardClaimModal();
                }
            });
            table.add(discard).width(250).height(46).padBottom(10);
            table.row();
        }
        TextButton skip = new TextButton("跳过药水", UiStyles.buttonStyle(game));
        skip.addListener(new ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                entry.claimed = true;
                commitRewardPhase();
                showRewardClaimModal();
            }
        });
        table.add(skip).width(250).height(46).padTop(8);
        modal.toFront();
    }

    private void returnToMapAfterReward() {
        if (returnMap != null) {
            if (returnMap.isFinalLayer()) {
                game.setScreen(new EndingScreen(game, returnMap.endingForFinalBoss()));
                return;
            }
            returnMap.completeCurrentNodeAndReturn();
        } else {
            game.setScreen(new TitleScreen(game));
        }
    }

    private List<Card> rollCardRewards() {
        List<Card> pool = CardPool.getObtainableCardsByProfession(
                player.getProfession().name().toLowerCase()).stream()
                .filter(card -> card.getRarity() != Card.Rarity.BASIC)
                .toList();
        if (pool.isEmpty()) {
            pool = CardPool.getObtainableCardsByProfession(
                    player.getProfession().name().toLowerCase());
        }
        List<Card> selected = CardPool.randomSelect(pool, Math.min(3, pool.size()),
                rewardRandom("card-pick"));
        List<Card> rewards = new ArrayList<>();
        for (Card template : selected) {
            Card card = CardFactory.createFromTemplate(template);
            if (card.canUpgrade()
                    && rewardRandom("card-upgrade:" + card.getId()).nextInt(100)
                    < UPGRADED_REWARD_CHANCE_PERCENT) {
                card.upgrade();
            }
            rewards.add(card);
        }
        return rewards;
    }

    private Random rewardRandom(String purpose) {
        if (runState == null || runState.getActiveNode() == null) {
            return new Random(random.nextLong());
        }
        GameRunState.NodeRef node = runState.getActiveNode();
        return runState.randomFor("reward", node.layer, node.col, node.row, purpose);
    }

    private void commitRewardPhase() {
        if (runState != null && runState.isInCombat()) {
            runState.completeActiveNode();
        }
    }

    private void toggleEscapeMenu() {
        if (escapeMenu != null && escapeMenu.hasParent()) {
            escapeMenu.remove();
            escapeMenu = null;
            return;
        }
        escapeMenu = EscapeMenu.show(stage, game, () -> escapeMenu = null);
    }

    private static class RewardEntry {
        private final String label;
        private final Color color;
        private final Runnable claimAction;
        private final boolean opensCardSelection;
        private final Potion potionReward;
        private boolean claimed;

        private RewardEntry(String label, Color color, Runnable claimAction) {
            this(label, color, claimAction, false);
        }

        private RewardEntry(String label, Color color, Runnable claimAction,
                            boolean opensCardSelection) {
            this(label, color, claimAction, opensCardSelection, null);
        }

        private RewardEntry(String label, Color color, Runnable claimAction,
                            boolean opensCardSelection, Potion potionReward) {
            this.label = label;
            this.color = color;
            this.claimAction = claimAction;
            this.opensCardSelection = opensCardSelection;
            this.potionReward = potionReward;
            this.claimed = false;
        }
    }
}
