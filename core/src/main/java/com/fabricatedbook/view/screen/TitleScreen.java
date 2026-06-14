package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.fabricatedbook.data.SaveManager;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.ui.ResponsiveViewport;
import com.fabricatedbook.view.ui.UiStyles;

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
    private OrthographicCamera camera;
    private Texture background;

    public TitleScreen(FabricBookGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT);
    }

    @Override
    public void show() {
        stage = new Stage(ResponsiveViewport.create());
        Gdx.input.setInputProcessor(stage);
        try {
            background = new Texture("img/background.png");
        } catch (Exception e) {
            background = null;
        }

        // 创建UI
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // 标题
        Label titleLabel = new Label("虚妄之书", new Label.LabelStyle(
                game.getFontForScale(3.0f), com.badlogic.gdx.graphics.Color.WHITE));
        table.add(titleLabel).padBottom(80);
        table.row();

        // 按钮样式
        TextButton.TextButtonStyle btnStyle = UiStyles.buttonStyle(game);

        // 开始游戏按钮
        TextButton newGameBtn = new TextButton("开始游戏", btnStyle);
        newGameBtn.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                game.setScreen(new CharacterSelectScreen(game));
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
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        SpriteBatch batch = game.getBatch();
        stage.getViewport().apply();
        batch.setProjectionMatrix(stage.getCamera().combined);
        batch.begin();
        if (background != null) {
            batch.setColor(0.34f, 0.34f, 0.38f, 1f);
            batch.draw(background, 0, 0, FabricBookGame.SCREEN_WIDTH,
                    FabricBookGame.SCREEN_HEIGHT);
            batch.setColor(1f, 1f, 1f, 1f);
        }
        batch.end();

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
        if (background != null) background.dispose();
    }
}
