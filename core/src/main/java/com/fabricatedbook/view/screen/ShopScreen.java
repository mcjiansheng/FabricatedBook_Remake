package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.card.Card;
import com.fabricatedbook.core.shop.ShopManager;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.ui.ResponsiveViewport;
import com.fabricatedbook.view.ui.EscapeMenu;
import com.fabricatedbook.view.ui.UiStyles;
import com.fabricatedbook.view.ui.UiTheme;

import java.util.List;

/**
 * ShopScreen — 商店画面
 * <p>
 * 显示"诡异行商"节点的商品列表，玩家可购买卡牌、藏品和药水。
 * 支持弃牌操作。
 * <p>
 * 引用方：MapScreen（进入商店节点时切换）
 */
public class ShopScreen implements Screen {

    private final FabricBookGame game;
    private final Player player;
    private final ShopManager shopManager;
    private final MapScreen returnMap;
    private Stage stage;
    private OrthographicCamera camera;
    private Table itemTable;
    private Label goldLabel;
    private Label feedbackLabel;
    private com.badlogic.gdx.scenes.scene2d.Group escapeMenu;
    private Group removeModal;
    private int selectedRemoveIndex = -1;

    /**
     * 构造商店画面。
     *
     * @param game         游戏主类
     * @param player       玩家实体
     * @param shopManager  商店管理器
     */
    public ShopScreen(FabricBookGame game, Player player,
                      ShopManager shopManager) {
        this(game, player, shopManager, null);
    }

    public ShopScreen(FabricBookGame game, Player player,
                      ShopManager shopManager, MapScreen returnMap) {
        this.game = game;
        this.player = player;
        this.shopManager = shopManager;
        this.returnMap = returnMap;
        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT);
    }

    @Override
    public void show() {
        stage = new Stage(ResponsiveViewport.create());
        Gdx.input.setInputProcessor(stage);

        // 标题
        Label titleLabel = new Label("诡异行商", new Label.LabelStyle(
                game.getFontForScale(2.0f), UiTheme.ACCENT_GOLD));
        titleLabel.setPosition(FabricBookGame.SCREEN_WIDTH / 2f - 80,
                FabricBookGame.SCREEN_HEIGHT - 60);
        stage.addActor(titleLabel);

        // 金币显示
        goldLabel = new Label("金币: " + player.getGold(),
                new Label.LabelStyle(game.getFont(),
                        com.badlogic.gdx.graphics.Color.WHITE));
        goldLabel.setPosition(20, FabricBookGame.SCREEN_HEIGHT - 60);
        stage.addActor(goldLabel);

        feedbackLabel = new Label("选择商品购买。", new Label.LabelStyle(
                game.getFont(), com.badlogic.gdx.graphics.Color.LIGHT_GRAY));
        feedbackLabel.setPosition(20, FabricBookGame.SCREEN_HEIGHT - 88);
        stage.addActor(feedbackLabel);

        // 商品列表
        itemTable = new Table();
        itemTable.setFillParent(true);
        itemTable.top().padTop(100);
        renderItems();
        stage.addActor(itemTable);

        // 返回按钮
        TextButton.TextButtonStyle buttonStyle = UiStyles.buttonStyle(game);
        TextButton backBtn = new TextButton("离开商店", buttonStyle);
        backBtn.setPosition(FabricBookGame.SCREEN_WIDTH - 200, 30);
        backBtn.setSize(180, 50);
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                if (returnMap != null) {
                    returnMap.completeCurrentNodeAndReturn();
                } else {
                    game.setScreen(new MapScreen(game, player));
                }
            }
        });
        stage.addActor(backBtn);
    }

    /**
     * 渲染商品列表。
     */
    private void renderItems() {
        itemTable.clear();
        List<ShopManager.ShopItem> items = shopManager.getItems();
        Table content = new Table();
        content.top().left();
        addSection(content, "卡牌", items, ShopManager.ShopItem.ItemType.CARD);
        addSection(content, "藏品", items, ShopManager.ShopItem.ItemType.RELIC);
        addSection(content, "药水", items, ShopManager.ShopItem.ItemType.POTION);
        addRemoveService(content);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        itemTable.add(scroll).width(920).height(540).top();
    }

    private void addSection(Table content, String title, List<ShopManager.ShopItem> items,
                            ShopManager.ShopItem.ItemType type) {
        Label header = new Label(title, new Label.LabelStyle(
                game.getFontForScale(1.25f), UiTheme.ACCENT_GOLD));
        content.add(header).colspan(3).left().padTop(14).padBottom(8);
        content.row();
        for (int index = 0; index < items.size(); index++) {
            ShopManager.ShopItem item = items.get(index);
            if (item.getType() != type) continue;
            addItemRow(content, item, index);
        }
    }

    private void addItemRow(Table content, ShopManager.ShopItem item, int index) {
        boolean sold = item.isPurchased();
        Label name = new Label(item.getName() + "  ·  " + item.getPrice() + " 金币"
                + (sold ? "  [已购]" : ""), new Label.LabelStyle(game.getFont(),
                sold ? com.badlogic.gdx.graphics.Color.GRAY : com.badlogic.gdx.graphics.Color.WHITE));
        Label description = new Label(item.getDescription(), new Label.LabelStyle(game.getFont(),
                sold ? com.badlogic.gdx.graphics.Color.GRAY : com.badlogic.gdx.graphics.Color.LIGHT_GRAY));
        description.setWrap(true);
        content.add(name).width(240).left().pad(6);
        content.add(description).width(470).left().pad(6);
        if (sold) {
            content.add(new Label("已售罄", new Label.LabelStyle(game.getFont(),
                    com.badlogic.gdx.graphics.Color.GRAY))).width(120).pad(6);
        } else {
            TextButton buy = new TextButton("购买", UiStyles.buttonStyle(game));
            buy.addListener(new ClickListener() {
                @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    purchaseItem(index, item);
                }
            });
            content.add(buy).width(120).height(38).pad(6);
        }
        content.row();
    }

    private void addRemoveService(Table content) {
        Label header = new Label("服务", new Label.LabelStyle(game.getFontForScale(1.25f), UiTheme.ACCENT_GOLD));
        content.add(header).colspan(3).left().padTop(18).padBottom(8);
        content.row();
        String status = shopManager.isRemovePurchased() ? "已使用" : shopManager.getRemoveCost() + " 金币";
        content.add(new Label("移除一张卡牌  ·  " + status,
                new Label.LabelStyle(game.getFont(), com.badlogic.gdx.graphics.Color.WHITE))).width(240).left().pad(6);
        content.add(new Label("从牌组中选择一张卡牌移除。",
                new Label.LabelStyle(game.getFont(), com.badlogic.gdx.graphics.Color.LIGHT_GRAY))).width(470).left().pad(6);
        TextButton button = new TextButton(shopManager.isRemovePurchased() ? "已使用" : "选择卡牌",
                UiStyles.buttonStyle(game));
        button.setDisabled(shopManager.isRemovePurchased());
        button.addListener(new ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (player.getGold() < shopManager.getRemoveCost()) {
                    feedbackLabel.setText("金币不足：还需要 "
                            + (shopManager.getRemoveCost() - player.getGold()) + " 金币。");
                } else if (player.getDrawPile().isEmpty()) {
                    feedbackLabel.setText("牌组为空，无法移除卡牌。");
                } else {
                    showRemoveModal();
                }
            }
        });
        content.add(button).width(120).height(38).pad(6);
        content.row();
    }

    private void purchaseItem(int index, ShopManager.ShopItem item) {
        if (player.getGold() < item.getPrice()) {
            feedbackLabel.setText("金币不足：还需要 " + (item.getPrice() - player.getGold()) + " 金币。");
            return;
        }
        if (item.getType() == ShopManager.ShopItem.ItemType.POTION
                && player.getPotions().size() >= player.getMaxPotionSlots()) {
            feedbackLabel.setText("药水栏已满，无法购买药水。");
            return;
        }
        if (shopManager.purchase(index)) {
            goldLabel.setText("金币: " + player.getGold());
            feedbackLabel.setText("已购买：" + item.getName());
            game.autosaveCurrentRun();
            renderItems();
        } else {
            feedbackLabel.setText("购买失败，请检查商品状态。");
        }
    }

    private void showRemoveModal() {
        if (removeModal != null) removeModal.remove();
        removeModal = new Group();
        removeModal.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);
        stage.addActor(removeModal);

        Table backdrop = new Table();
        backdrop.setSize(FabricBookGame.SCREEN_WIDTH, FabricBookGame.SCREEN_HEIGHT);
        backdrop.setBackground(UiStyles.modalBackdrop());
        removeModal.addActor(backdrop);

        Table panel = new Table();
        panel.setBackground(UiStyles.panelSurface());
        panel.setSize(840, 470);
        panel.setPosition((FabricBookGame.SCREEN_WIDTH - panel.getWidth()) / 2f,
                (FabricBookGame.SCREEN_HEIGHT - panel.getHeight()) / 2f);
        panel.pad(24);
        removeModal.addActor(panel);

        Label title = new Label("移除一张卡牌", new Label.LabelStyle(
                game.getFontForScale(1.6f), UiTheme.ACCENT_GOLD));
        panel.add(title).padBottom(12);
        panel.row();
        panel.add(new Label("费用：" + shopManager.getRemoveCost() + " 金币。选择后不可撤销。",
                new Label.LabelStyle(game.getFont(), com.badlogic.gdx.graphics.Color.WHITE))).padBottom(12);
        panel.row();

        Table cards = new Table();
        cards.top().left();
        for (int i = 0; i < player.getDrawPile().size(); i++) {
            Card card = player.getDrawPile().get(i);
            final int cardIndex = i;
            String marker = cardIndex == selectedRemoveIndex ? "✓ " : "";
            TextButton choose = new TextButton(marker + card.getName() + "  用 " + card.getCost()
                    + "\n" + card.getDescription(), UiStyles.buttonStyle(game));
            choose.getLabel().setAlignment(com.badlogic.gdx.utils.Align.left);
            choose.addListener(new ClickListener() {
                @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    selectedRemoveIndex = cardIndex;
                    showRemoveModal();
                }
            });
            cards.add(choose).width(360).height(64).pad(5);
            if ((i + 1) % 2 == 0) cards.row();
        }
        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        panel.add(scroll).width(760).height(300).padBottom(14);
        panel.row();

        Table actions = new Table();
        TextButton cancel = new TextButton("取消", UiStyles.buttonStyle(game));
        cancel.addListener(new ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                selectedRemoveIndex = -1;
                removeModal.remove();
                removeModal = null;
            }
        });
        TextButton confirm = new TextButton("确认移除", UiStyles.buttonStyle(game));
        confirm.setDisabled(selectedRemoveIndex < 0);
        confirm.addListener(new ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (selectedRemoveIndex < 0) return;
                Card selected = player.getDrawPile().get(selectedRemoveIndex);
                if (shopManager.purchaseRemove(selectedRemoveIndex)) {
                    goldLabel.setText("金币: " + player.getGold());
                    feedbackLabel.setText("已移除：" + selected.getName());
                    game.autosaveCurrentRun();
                    renderItems();
                } else {
                    feedbackLabel.setText("移除失败，请检查金币与牌组状态。");
                }
                selectedRemoveIndex = -1;
                removeModal.remove();
                removeModal = null;
            }
        });
        actions.add(cancel).width(160).height(46).padRight(14);
        actions.add(confirm).width(160).height(46);
        panel.add(actions);
        removeModal.toFront();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            toggleEscapeMenu();
        }
        Gdx.gl.glClearColor(UiTheme.BATTLE_HAND.r, UiTheme.BATTLE_HAND.g, UiTheme.BATTLE_HAND.b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    @Override public void pause() { game.autosaveCurrentRun(); }
    @Override public void resume() {}
    @Override public void hide() {
        game.autosaveCurrentRun();
        Gdx.input.setInputProcessor(null);
    }
    @Override public void dispose() {
        stage.dispose();
    }

    private void toggleEscapeMenu() {
        if (escapeMenu != null && escapeMenu.hasParent()) {
            escapeMenu.remove();
            escapeMenu = null;
            return;
        }
        escapeMenu = EscapeMenu.show(stage, game, () -> escapeMenu = null);
    }
}
