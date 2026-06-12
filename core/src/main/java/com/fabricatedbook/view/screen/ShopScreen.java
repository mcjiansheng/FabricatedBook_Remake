package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.shop.ShopManager;
import com.fabricatedbook.view.FabricBookGame;

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
        stage = new Stage(new FitViewport(FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        // 标题
        Label titleLabel = new Label("诡异行商", new Label.LabelStyle(
                game.getFont(), com.badlogic.gdx.graphics.Color.GOLD));
        titleLabel.setFontScale(2.0f);
        titleLabel.setPosition(FabricBookGame.SCREEN_WIDTH / 2f - 80,
                FabricBookGame.SCREEN_HEIGHT - 60);
        stage.addActor(titleLabel);

        // 金币显示
        goldLabel = new Label("金币: " + player.getGold(),
                new Label.LabelStyle(game.getFont(),
                        com.badlogic.gdx.graphics.Color.WHITE));
        goldLabel.setPosition(20, FabricBookGame.SCREEN_HEIGHT - 60);
        stage.addActor(goldLabel);

        // 商品列表
        itemTable = new Table();
        itemTable.setFillParent(true);
        itemTable.top().padTop(100);
        renderItems();
        stage.addActor(itemTable);

        // 返回按钮
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = game.getFont();
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

        for (int i = 0; i < items.size(); i++) {
            ShopManager.ShopItem item = items.get(i);
            final int index = i;

            String itemStr = item.getName() + " - " + item.getPrice() + "金币"
                    + (item.isPurchased() ? " [已购]" : "");
            Label itemLabel = new Label(itemStr, new Label.LabelStyle(
                    game.getFont(),
                    item.isPurchased() ? com.badlogic.gdx.graphics.Color.GRAY
                            : com.badlogic.gdx.graphics.Color.WHITE));
            itemTable.add(itemLabel).pad(5);

            Label descLabel = new Label(item.getDescription(), new Label.LabelStyle(
                    game.getFont(), com.badlogic.gdx.graphics.Color.LIGHT_GRAY));
            descLabel.setWrap(true);
            itemTable.add(descLabel).width(420).pad(5);

            if (!item.isPurchased()) {
                TextButton.TextButtonStyle buyStyle = new TextButton.TextButtonStyle();
                buyStyle.font = game.getFont();
                TextButton buyBtn = new TextButton("购买", buyStyle);
                buyBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(
                            com.badlogic.gdx.scenes.scene2d.InputEvent event,
                            float x, float y) {
                        if (shopManager.purchase(index)) {
                            goldLabel.setText("金币: " + player.getGold());
                            renderItems();
                        }
                    }
                });
                itemTable.add(buyBtn).width(80).height(30).padLeft(10);
            }

            itemTable.row();
        }

        // 弃牌选项
        String removeStr = "弃牌 (" + shopManager.getRemoveCost() + "金币)"
                + (shopManager.isRemovePurchased() ? " [已购]" : "");
        Label removeLabel = new Label(removeStr, new Label.LabelStyle(
                game.getFont(), com.badlogic.gdx.graphics.Color.WHITE));
        itemTable.add(removeLabel).pad(5);
        itemTable.row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.12f, 0.1f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {
        Gdx.input.setInputProcessor(null);
    }
    @Override public void dispose() {
        stage.dispose();
    }
}
