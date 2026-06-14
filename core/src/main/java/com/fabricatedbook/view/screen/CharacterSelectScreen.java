package com.fabricatedbook.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.fabricatedbook.core.entity.Player;
import com.fabricatedbook.core.entity.Profession;
import com.fabricatedbook.view.FabricBookGame;
import com.fabricatedbook.view.ui.ResponsiveViewport;

/**
 * CharacterSelectScreen — 新游戏角色选择。
 */
public class CharacterSelectScreen implements Screen {

    private final FabricBookGame game;
    private Stage stage;
    private Texture background;
    private Texture warriorTexture;
    private Texture mageTexture;
    private Texture witchTexture;

    public CharacterSelectScreen(FabricBookGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(ResponsiveViewport.create());
        Gdx.input.setInputProcessor(stage);

        background = loadTexture("img/background.png");
        warriorTexture = loadTexture("img/Character_Selection_Warrior.png");
        mageTexture = loadTexture("img/Character_Selection_Mage.png");
        witchTexture = loadTexture("img/Character_Selection_Witch.png");

        Table root = new Table();
        root.setFillParent(true);
        root.top().padTop(36);
        stage.addActor(root);

        Label title = new Label("角色选择", new Label.LabelStyle(
                game.getFont(), com.badlogic.gdx.graphics.Color.WHITE));
        title.setFontScale(2.2f);
        root.add(title).colspan(3).padBottom(24);
        root.row();

        addProfession(root, Profession.WARRIOR, warriorTexture,
                "战士", "生命 80 / 胜利后回复生命");
        addProfession(root, Profession.MAGE, mageTexture,
                "法师", "生命 60 / 试作职业");
        addProfession(root, Profession.WITCH, witchTexture,
                "女巫", "生命 60 / 试作职业");

        root.row();
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = game.getFont();
        TextButton back = new TextButton("返回", style);
        back.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                game.setScreen(new TitleScreen(game));
            }
        });
        root.add(back).colspan(3).width(160).height(44).padTop(28);
    }

    private void addProfession(Table root, Profession profession, Texture texture,
                               String name, String description) {
        Table card = new Table();
        card.defaults().pad(6);

        if (texture != null) {
            Image image = new Image(texture);
            card.add(image).width(170).height(220);
            card.row();
        }

        Label nameLabel = new Label(name, new Label.LabelStyle(
                game.getFont(), com.badlogic.gdx.graphics.Color.WHITE));
        nameLabel.setFontScale(1.4f);
        card.add(nameLabel);
        card.row();

        Label desc = new Label(description, new Label.LabelStyle(
                game.getFont(), com.badlogic.gdx.graphics.Color.LIGHT_GRAY));
        card.add(desc);
        card.row();

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = game.getFont();
        TextButton choose = new TextButton("选择" + name, style);
        choose.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y) {
                Player player = new Player("player", name, profession);
                game.setScreen(new MapScreen(game, player));
            }
        });
        card.add(choose).width(150).height(44).padTop(8);

        root.add(card).width(300).height(420).pad(18);
    }

    private Texture loadTexture(String path) {
        try {
            return new Texture(path);
        } catch (Exception e) {
            return null;
        }
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
            batch.setColor(0.26f, 0.26f, 0.30f, 1f);
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
    @Override public void hide() { Gdx.input.setInputProcessor(null); }
    @Override public void dispose() {
        stage.dispose();
        if (background != null) background.dispose();
        if (warriorTexture != null) warriorTexture.dispose();
        if (mageTexture != null) mageTexture.dispose();
        if (witchTexture != null) witchTexture.dispose();
    }
}
