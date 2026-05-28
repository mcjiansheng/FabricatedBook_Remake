package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.data.SaveManager;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.actor.ButtonActor;

/**
 * TitleScreen — 标题画面
 * <p>
 * 游戏启动时显示的标题画面。
 * 提供"开始游戏"、"继续游戏"和"退出游戏"按钮。
 * <p>
 * 引用方：FabricBookGame（初始屏幕）、MapScreen（新游戏）
 */
public class TitleScreen implements Screen {

    private final FabricBookGame game;
    private Stage stage;
    private Skin skin;
    private OrthographicCamera camera;

    public TitleScreen(FabricBookGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT);
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        // 创建UI
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // 标题
        Label titleLabel = new Label("Fabricated Book", new Label.LabelStyle(
                game.getFont(), com.badlogic.gdx.graphics.Color.WHITE));
        titleLabel.setFontScale(3.0f);
        table.add(titleLabel).padBottom(80);
        table.row();

        // 按钮样式
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = game.getFont();

        // 开始游戏按钮
        TextButton newGameBtn = new TextButton("开始游戏", btnStyle);
        newGameBtn.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                // 创建新玩家并进入地图
                Player newPlayer = new Player("战士", Profession.WARRIOR, 80);
                game.setScreen(new MapScreen(game, newPlayer));
            }
        });
        table.add(newGameBtn).width(250).height(50).padBottom(20);
        table.row();

        // 继续游戏按钮
        TextButton continueBtn = new TextButton("继续游戏", btnStyle);
        continueBtn.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                SaveManager saveManager = game.getSaveManager();
                if (saveManager.hasSave()) {
                    var player = saveManager.load();
                    if (player != null) {
                        game.setScreen(new MapScreen(game, player));
                    }
                }
            }
        });
        table.add(continueBtn).width(250).height(50).padBottom(20);
        table.row();

        // 退出按钮
        TextButton exitBtn = new TextButton("退出", btnStyle);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                Gdx.app.exit();
            }
        });
        table.add(exitBtn).width(250).height(50);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
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
