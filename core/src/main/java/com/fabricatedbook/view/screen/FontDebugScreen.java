package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.ui.ResponsiveViewport;

import java.util.ArrayList;
import java.util.List;

/** Dedicated frontend screen for inspecting font glyphs, colors, sizes, and images. */
public class FontDebugScreen implements Screen {

    private static final String SAMPLE_EN =
            "Fabricated Book ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz 0123456789";
    private static final String SAMPLE_CN =
            "虚妄之书 探索者与虚妄之书 生命 金币 药水 第1层 荒野 战斗奖励";
    private static final String SAMPLE_SYMBOLS =
            "，。、；：！？（）【】《》“”‘’—…·～ / = + * # @ & % ￥ ①②③④⑤";

    private static final String[] IMAGE_FILES = {
            "fight.png", "Emergency.png", "unexpectedly.png", "decision.png",
            "shop.png", "reward.png", "safe_house.png", "boss.png",
            "atk.png", "def.png", "inc.png", "ukn.png",
            "strength.png", "resistance.png", "poisoning.png", "withering.png",
            "fragile.png", "weak.png", "dizziness.png", "undead.png",
            "Warrior.png", "ragpicker.png", "vulture.png", "Thief.png"
    };

    private final FabricBookGame game;
    private final List<Texture> textures = new ArrayList<>();
    private Stage stage;

    public FontDebugScreen(FabricBookGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(ResponsiveViewport.create());
        Gdx.input.setInputProcessor(stage);

        Table content = new Table();
        content.top().left().pad(24);
        content.defaults().left().padBottom(10);

        addLabel(content, "字体调试 / Font Debug", 2.4f, Color.WHITE);
        addLabel(content, "用于检查字体 atlas 是否缺字、不同字号是否清晰、颜色与图片是否正常。", 1f,
                Color.LIGHT_GRAY);
        content.row();

        float[] scales = {0.75f, 1f, 1.25f, 1.5f, 1.8f, 2.2f, 3f};
        Color[] colors = {Color.WHITE, Color.BLACK, Color.GOLD, Color.SCARLET,
                Color.CYAN, Color.LIME};
        for (float scale : scales) {
            addLabel(content, "字号 scale " + scale + " / size "
                    + Math.round(FabricBookGame.BASE_FONT_SIZE * scale), 1f, Color.GOLD);
            for (Color color : colors) {
                addLabel(content, SAMPLE_EN, scale, color);
                addLabel(content, SAMPLE_CN, scale, color);
                addLabel(content, SAMPLE_SYMBOLS, scale, color);
            }
            content.row();
        }

        addLabel(content, "图片资源 / Images", 1.8f, Color.GOLD);
        Table imageGrid = new Table();
        imageGrid.left();
        int col = 0;
        for (String file : IMAGE_FILES) {
            Table cell = new Table();
            Texture texture = loadTexture("img/" + file);
            if (texture != null) {
                textures.add(texture);
                cell.add(new Image(texture)).width(96).height(64).padBottom(4);
                cell.row();
            }
            cell.add(new Label(file, new Label.LabelStyle(game.getFontForScale(0.75f),
                    Color.WHITE))).width(130);
            imageGrid.add(cell).width(140).height(104).pad(8);
            col++;
            if (col % 4 == 0) {
                imageGrid.row();
            }
        }
        content.add(imageGrid).left();

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFillParent(true);
        scrollPane.setFadeScrollBars(false);
        stage.addActor(scrollPane);
    }

    private void addLabel(Table table, String text, float scale, Color color) {
        Label label = new Label(text, new Label.LabelStyle(game.getFontForScale(scale), color));
        label.setWrap(true);
        table.add(label).width(1180).left().padBottom(6);
        table.row();
    }

    private Texture loadTexture(String path) {
        try {
            return new Texture(path);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.10f, 0.10f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        stage.dispose();
        for (Texture texture : textures) {
            texture.dispose();
        }
    }
}
