package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
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
import com.fabricatedbook.view.ui.GameHud;
import com.fabricatedbook.view.ui.UiModal;
import com.fabricatedbook.view.ui.UiFeedback;
import com.fabricatedbook.view.ui.UiLayout;
import com.fabricatedbook.view.ui.UiScrollPane;
import com.fabricatedbook.view.ui.UiTooltip;
import com.fabricatedbook.view.ui.UiGlossary;
import com.fabricatedbook.view.actor.CardActor;

import java.util.List;
import java.util.ArrayList;

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
    private final String entryMessage;
    private Stage stage;
    private OrthographicCamera camera;
    private Table itemTable;
    private ScrollPane itemScroll;
    private float itemScrollPosition;
    private final List<CardActor> itemCardPreviews = new ArrayList<>();
    private Label itemDetailsLabel;
    private UiFeedback feedbackLabel;
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
        this(game, player, shopManager, returnMap, null);
    }

    public ShopScreen(FabricBookGame game, Player player,
                      ShopManager shopManager, MapScreen returnMap,
                      String entryMessage) {
        this.game = game;
        this.player = player;
        this.shopManager = shopManager;
        this.returnMap = returnMap;
        this.entryMessage = entryMessage;
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
                FabricBookGame.SCREEN_HEIGHT - 112);
        stage.addActor(titleLabel);

        feedbackLabel = new UiFeedback(game);
        feedbackLabel.setPosition(20, FabricBookGame.SCREEN_HEIGHT - 116);
        stage.addActor(feedbackLabel);
        feedbackLabel.show(entryMessage == null || entryMessage.isBlank()
                ? "选择商品购买。"
                : entryMessage, UiFeedback.Tone.INFO);

        // 商品列表
        itemTable = new Table();
        itemTable.setFillParent(true);
        itemTable.top().padTop(142);
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
        new GameHud(stage, game, player,
                () -> returnMap != null ? returnMap.currentLayerStatusText() : "第" + player.getCurrentFloor() + "层",
                () -> game.setScreen(new InventoryScreen(game, player, returnMap, InventoryScreen.Tab.CARDS)),
                () -> game.setScreen(new InventoryScreen(game, player, returnMap, InventoryScreen.Tab.RELICS)),
                false, null, () -> {
                    feedbackLabel.show("药水栏已更新。", UiFeedback.Tone.INFO);
                    rememberItemScroll();
                    renderItems();
                });
    }

    /**
     * 渲染商品列表。
     */
    private void renderItems() {
        for (CardActor preview : itemCardPreviews) preview.dispose();
        itemCardPreviews.clear();
        itemTable.clear();
        List<ShopManager.ShopItem> items = shopManager.getItems();
        Table content = new Table();
        content.top().left();
        itemDetailsLabel = new Label("悬停或点击商品查看完整说明。",
                new Label.LabelStyle(game.getFont(), com.badlogic.gdx.graphics.Color.LIGHT_GRAY));
        itemDetailsLabel.setWrap(true);

        Table products = new Table();
        products.top().left();
        addCardSection(products, items);
        addRemoveService(products);

        Table detailPanel = new Table();
        detailPanel.top().left().pad(14);
        detailPanel.setBackground(UiStyles.panelSurface());
        detailPanel.add(new Label("商品说明", new Label.LabelStyle(
                game.getFontForScale(1.08f), UiTheme.ACCENT_GOLD))).left().padBottom(10);
        detailPanel.row();
        detailPanel.add(itemDetailsLabel).width(220).top().left();

        Table body = new Table();
        body.top().left();
        body.add(products).width(900).top().left().padRight(14);
        body.add(detailPanel).width(248).height(500).top();
        content.add(body).width(UiLayout.SHOP_CONTENT_WIDTH).top().left();

        itemScroll = UiScrollPane.vertical(content);
        itemScroll.setFadeScrollBars(false);
        itemScroll.setScrollingDisabled(true, false);
        itemTable.add(itemScroll).width(UiLayout.SHOP_CONTENT_WIDTH).height(540).top();
        itemScroll.layout();
        itemScroll.setScrollY(itemScrollPosition);
    }

    /** Card goods deliberately use their real card faces; price and action stay beneath each face. */
    private void addCardSection(Table content, List<ShopManager.ShopItem> items) {
        addSectionHeader(content, "卡牌");
        Table firstRow = new Table();
        firstRow.top().left();
        Table secondRowCards = new Table();
        secondRowCards.top().left();
        Table itemShelf = new Table();
        itemShelf.top().left();
        int cardCount = 0;
        for (int index = 0; index < items.size(); index++) {
            ShopManager.ShopItem item = items.get(index);
            if (item.getType() != ShopManager.ShopItem.ItemType.CARD) continue;
            Table target = cardCount++ < 5 ? firstRow : secondRowCards;
            target.add(createCardDisplay(item, index)).width(120).height(218).pad(3);
        }
        content.add(firstRow).width(900).left();
        content.row();
        addCompactRow(itemShelf, "藏品", items, ShopManager.ShopItem.ItemType.RELIC);
        addCompactRow(itemShelf, "药水", items, ShopManager.ShopItem.ItemType.POTION);
        Table secondRow = new Table();
        secondRow.top().left();
        secondRow.add(secondRowCards).width(252).top().left().padRight(14);
        secondRow.add(itemShelf).width(530).top().left();
        content.add(secondRow).width(900).left().padBottom(8);
        content.row();
    }

    /** The small items deliberately occupy the space beside the second card row, like the reference shop. */
    private void addCompactRow(Table shelf, String title, List<ShopManager.ShopItem> items,
                               ShopManager.ShopItem.ItemType type) {
        shelf.add(new Label(title, new Label.LabelStyle(game.getFontForScale(0.84f), UiTheme.ACCENT_GOLD)))
                .width(48).left();
        for (int index = 0; index < items.size(); index++) {
            ShopManager.ShopItem item = items.get(index);
            if (item.getType() != type) continue;
            shelf.add(createCompactItemTile(item, index)).width(146).height(90).pad(2);
        }
        shelf.row();
    }

    private void addSectionHeader(Table content, String title) {
        content.add(new Label(title, new Label.LabelStyle(
                game.getFontForScale(1.25f), UiTheme.ACCENT_GOLD))).width(900).left().padTop(6).padBottom(3);
        content.row();
    }

    private Table createCardDisplay(ShopManager.ShopItem item, int index) {
        Table display = new Table();
        display.top();
        CardActor preview = new CardActor((Card) item.getData(), game.getFont(), null);
        preview.setDraggingEnabled(false);
        String status = itemStatus(item);
        if (!"可购买".equals(status)) preview.setUnavailableReason(status);
        itemCardPreviews.add(preview);
        display.add(preview).width(CardActor.CARD_WIDTH).height(CardActor.CARD_HEIGHT);
        display.row();
        display.add(priceLabel(item)).height(21);
        display.row();
        display.add(statusLabel(item)).height(18);
        bindPurchase(preview, item, index);
        bindItemDetails(display, item);
        UiTooltip.bind(preview, stage, game, () -> itemDetails(item));
        return display;
    }

    private Table createCompactItemTile(ShopManager.ShopItem item, int index) {
        Table tile = new Table();
        tile.setBackground(UiStyles.panelSurface());
        tile.pad(5);
        String status = itemStatus(item);
        com.badlogic.gdx.graphics.Color textColor = "已售罄".equals(status)
                ? com.badlogic.gdx.graphics.Color.GRAY : com.badlogic.gdx.graphics.Color.WHITE;
        Label icon = new Label(item.getType() == ShopManager.ShopItem.ItemType.RELIC ? "藏" : "药",
                new Label.LabelStyle(game.getFontForScale(1.15f),
                        "已售罄".equals(status) ? com.badlogic.gdx.graphics.Color.GRAY : UiTheme.ACCENT_GOLD));
        Table iconFrame = new Table();
        iconFrame.setBackground(UiStyles.lightPanelSurface());
        iconFrame.add(icon);
        Table information = new Table();
        information.top().left();
        Label name = new Label(item.getName(), new Label.LabelStyle(game.getFont(), textColor));
        name.setEllipsis(true);
        information.add(name).width(78).left();
        information.row();
        information.add(priceLabel(item)).left().padTop(2);
        information.row();
        information.add(new Label(status, new Label.LabelStyle(game.getFontForScale(0.76f),
                "可购买".equals(status) ? com.badlogic.gdx.graphics.Color.LIGHT_GRAY : UiTheme.ACCENT_GOLD)))
                .left().padTop(2);
        tile.add(iconFrame).width(36).height(36).top().padRight(5);
        tile.add(information).width(88).top().left();
        bindPurchase(tile, item, index);
        bindItemDetails(tile, item);
        UiTooltip.bind(tile, stage, game, () -> itemDetails(item));
        return tile;
    }

    private Label priceLabel(ShopManager.ShopItem item) {
        return new Label("金币 " + item.getPrice(), new Label.LabelStyle(game.getFont(),
                item.isPurchased() ? com.badlogic.gdx.graphics.Color.GRAY : UiTheme.ACCENT_GOLD));
    }

    private Label statusLabel(ShopManager.ShopItem item) {
        String status = itemStatus(item);
        return new Label("可购买".equals(status) ? "点击购买" : status,
                new Label.LabelStyle(game.getFontForScale(0.72f),
                        "可购买".equals(status) ? com.badlogic.gdx.graphics.Color.LIGHT_GRAY : UiTheme.ACCENT_GOLD));
    }

    /** Product actors are the purchase controls; no separate action row competes with the merchandise. */
    private void bindPurchase(Actor actor, ShopManager.ShopItem item, int index) {
        actor.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (item.isPurchased()) {
                    feedbackLabel.show("该商品已售罄。", UiFeedback.Tone.INFO);
                } else {
                    purchaseItem(index, item);
                }
            }
        });
    }

    private String itemStatus(ShopManager.ShopItem item) {
        if (item.isPurchased()) return "已售罄";
        if (player.getGold() < item.getPrice()) return "金币不足";
        if (item.getType() == ShopManager.ShopItem.ItemType.POTION && !player.canAddPotion()) return "药水栏满";
        return "可购买";
    }

    private void bindItemDetails(Actor actor, ShopManager.ShopItem item) {
        actor.addListener(new InputListener() {
            @Override public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                showItemDetails(item);
            }

            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                showItemDetails(item);
                return false;
            }
        });
    }

    private void showItemDetails(ShopManager.ShopItem item) {
        if (itemDetailsLabel != null) itemDetailsLabel.setText(itemDetails(item));
    }

    private String itemDetails(ShopManager.ShopItem item) {
        String status = itemStatus(item);
        String detail = itemTypeLabel(item.getType()) + " · " + item.getPrice() + " 金币\n"
                + item.getDescription() + "\n状态：" + status;
        if (item.getType() == ShopManager.ShopItem.ItemType.POTION) {
            return UiGlossary.potionDetails(item.getName(), detail);
        }
        return detail;
    }

    private String itemTypeLabel(ShopManager.ShopItem.ItemType type) {
        return switch (type) {
            case CARD -> "卡牌";
            case RELIC -> "藏品";
            case POTION -> "药水";
        };
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
        boolean cannotAfford = player.getGold() < shopManager.getRemoveCost();
        boolean emptyDeck = player.getDrawPile().isEmpty();
        String action = shopManager.isRemovePurchased() ? "已使用"
                : cannotAfford ? "金币不足" : emptyDeck ? "牌组为空" : "选择卡牌";
        TextButton button = new TextButton(action,
                UiStyles.buttonStyle(game));
        button.setDisabled(shopManager.isRemovePurchased() || cannotAfford || emptyDeck);
        button.addListener(new ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (player.getGold() < shopManager.getRemoveCost()) {
                    feedbackLabel.show("金币不足：还需要 "
                            + (shopManager.getRemoveCost() - player.getGold()) + " 金币。", UiFeedback.Tone.WARNING);
                } else if (player.getDrawPile().isEmpty()) {
                    feedbackLabel.show("牌组为空，无法移除卡牌。", UiFeedback.Tone.WARNING);
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
            feedbackLabel.show("金币不足：还需要 " + (item.getPrice() - player.getGold()) + " 金币。", UiFeedback.Tone.WARNING);
            return;
        }
        if (item.getType() == ShopManager.ShopItem.ItemType.POTION
                && player.getPotions().size() >= player.getMaxPotionSlots()) {
            feedbackLabel.show("药水栏已满，无法购买药水。", UiFeedback.Tone.WARNING);
            return;
        }
        if (shopManager.purchase(index)) {
            feedbackLabel.show("已购买：" + item.getName(), UiFeedback.Tone.SUCCESS);
            game.autosaveCurrentRun();
            rememberItemScroll();
            renderItems();
        } else {
            feedbackLabel.show("购买失败，请检查商品状态。", UiFeedback.Tone.ERROR);
        }
    }

    private void showRemoveModal() {
        if (removeModal != null) removeModal.remove();
        removeModal = UiModal.open(stage);
        Table panel = UiModal.panel(840, 470);
        panel.pad(UiLayout.PANEL_PADDING);
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
        ScrollPane scroll = UiScrollPane.vertical(cards);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        panel.add(scroll).width(760).height(300).padBottom(14);
        panel.row();

        Table actions = new Table();
        TextButton cancel = new TextButton("取消", UiStyles.buttonStyle(game));
        cancel.addListener(new ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                selectedRemoveIndex = -1;
                UiModal.close(removeModal);
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
                    feedbackLabel.show("已移除：" + selected.getName(), UiFeedback.Tone.SUCCESS);
                    game.autosaveCurrentRun();
                    rememberItemScroll();
                    renderItems();
                } else {
                    feedbackLabel.show("移除失败，请检查金币与牌组状态。", UiFeedback.Tone.ERROR);
                }
                selectedRemoveIndex = -1;
                UiModal.close(removeModal);
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
            if (removeModal != null && removeModal.hasParent()) {
                selectedRemoveIndex = -1;
                UiModal.close(removeModal);
                removeModal = null;
                return;
            }
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
        for (CardActor preview : itemCardPreviews) preview.dispose();
        itemCardPreviews.clear();
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

    private void rememberItemScroll() {
        if (itemScroll != null) itemScrollPosition = itemScroll.getScrollY();
    }
}
