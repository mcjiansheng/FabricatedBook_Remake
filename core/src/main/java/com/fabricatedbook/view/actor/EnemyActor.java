package com.fabricatedbook.view.actor;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.fabricatedbook.core.engine.EnemyActionResolver;
import com.fabricatedbook.core.entity.Enemy;
import com.fabricatedbook.core.entity.IntentType;

import java.util.HashMap;
import java.util.Map;

/**
 * EnemyActor — 敌人 Actor
 * <p>
 * 在战斗界面中显示敌人立绘、生命值、格挡值和意图。
 * 根据敌人名称自动匹配对应的 PNG 立绘。
 * <p>
 * 引用方：BattleScreen（创建和更新敌人显示）
 * 资源文件：img/{敌人名}.png
 */
public class EnemyActor extends Actor {

    private final Enemy enemy;
    private final BitmapFont font;
    private final ShapeRenderer shapeRenderer;
    private Texture sprite;
    private boolean highlighted;

    public static final float ENEMY_WIDTH = 170;
    public static final float ENEMY_HEIGHT = 245;

    // 敌人名称→图片文件名映射表（英文名→中文图片名）
    private static final Map<String, String> NAME_TO_FILE = new HashMap<>();
    static {
        NAME_TO_FILE.put("拾荒者", "ragpicker");
        NAME_TO_FILE.put("秃鹫", "vulture");
        NAME_TO_FILE.put("盗贼", "Thief");
        NAME_TO_FILE.put("孤魂野鬼", "wandering_ghost");
        NAME_TO_FILE.put("猎人", "hunter");
        NAME_TO_FILE.put("树人", "treeman");
        NAME_TO_FILE.put("哥布林", "Goblin");
        NAME_TO_FILE.put("史莱姆", "slime");
        NAME_TO_FILE.put("老猎人", "oldhunter");
        NAME_TO_FILE.put("哥布林统领", "Goblin_Chief");
        NAME_TO_FILE.put("腐尸", "zombie");
        NAME_TO_FILE.put("堕落守林人", "Corrupted_Forest_Warden");
        NAME_TO_FILE.put("凋零之树", "withering_tree");
        NAME_TO_FILE.put("人中菌", "Human_Mushroom");
        NAME_TO_FILE.put("迷失的守林人", "The_Lost_Forest_Warden");
        NAME_TO_FILE.put("腐化的守林人", "Corrupted_Forest_Ranger");
        NAME_TO_FILE.put("雾鬼", "fog_ghost");
        NAME_TO_FILE.put("幻影", "Phantom");
        NAME_TO_FILE.put("雾行鸟", "Fog_walking_Bird");
        NAME_TO_FILE.put("腐烂山鬼", "Mountain_Zombie");
        NAME_TO_FILE.put("迷途旅人", "Lost_Traveler");
        NAME_TO_FILE.put("最后的哨兵", "The_Last_Sentinel");
        NAME_TO_FILE.put("炮塔", "turret");
        NAME_TO_FILE.put("剑卫", "Sword_Guardian");
        NAME_TO_FILE.put("盾卫", "Shield_Guard");
        NAME_TO_FILE.put("铳卫", "Gun_Guardian");
        NAME_TO_FILE.put("法师", "mage");
        NAME_TO_FILE.put("石像鬼", "Gargoyle");
        NAME_TO_FILE.put("卫士统领", "Guardian_Commander");
        NAME_TO_FILE.put("影卫", "shadow_guard");
        NAME_TO_FILE.put("魔王", "King");
        NAME_TO_FILE.put("幕后黑手", "Puppetmaster");
        NAME_TO_FILE.put("傀儡", "puppet");
        NAME_TO_FILE.put("战士", "Warrior");
        // 训练假人 fallback（对应 img/train.png）
        NAME_TO_FILE.put("训练假人", "train");
        NAME_TO_FILE.put("训练首领", "train");
        NAME_TO_FILE.put("精英训练假人", "train");
    }

    /**
     * 构造敌人 Actor。
     *
     * @param enemy    敌人实体
     * @param font     字体
     * @param renderer ShapeRenderer
     */
    public EnemyActor(Enemy enemy, BitmapFont font, ShapeRenderer renderer) {
        this.enemy = enemy;
        this.font = font;
        this.shapeRenderer = renderer;
        setSize(ENEMY_WIDTH, ENEMY_HEIGHT);

        // 加载对应立绘
        loadSprite();
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    /** 根据敌人名称加载对应立绘 */
    private void loadSprite() {
        String fileName = NAME_TO_FILE.get(enemy.getName());
        if (fileName == null) {
            // 尝试直接用敌人名作为文件名
            fileName = enemy.getName().toLowerCase();
        }
        try {
            sprite = new Texture("img/" + fileName + ".png");
        } catch (Exception e) {
            // 加载失败时用默认空纹理
            sprite = null;
        }
    }

    /** 获取敌人实体 */
    public Enemy getEnemy() { return enemy; }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (!enemy.isAlive()) {
            setVisible(false);
            return;
        }

        batch.end();

        // 绘制血条背景
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (highlighted) {
            shapeRenderer.setColor(1f, 0.55f, 0.25f, 0.35f);
            shapeRenderer.rect(getX() - 12, getY() - 12,
                    ENEMY_WIDTH + 24, ENEMY_HEIGHT + 24);
        }
        // 生命值条（红色→绿色根据血量比例）
        float hpRatio = (float) enemy.getHp() / enemy.getMaxHp();
        float hpBarWidth = ENEMY_WIDTH - 28;
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
        shapeRenderer.rect(getX() + 14, getY() + 10, hpBarWidth, 9);
        if (hpRatio > 0.5f) {
            shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1f);
        } else if (hpRatio > 0.25f) {
            shapeRenderer.setColor(0.8f, 0.8f, 0.2f, 1f);
        } else {
            shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 1f);
        }
        shapeRenderer.rect(getX() + 14, getY() + 10, hpBarWidth * hpRatio, 9);
        shapeRenderer.end();

        if (highlighted) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1f, 0.82f, 0.35f, 1f);
            shapeRenderer.rect(getX() - 12, getY() - 12,
                    ENEMY_WIDTH + 24, ENEMY_HEIGHT + 24);
            shapeRenderer.end();
        }

        batch.begin();

        // 绘制敌人立绘
        if (sprite != null) {
            float maxSpriteWidth = ENEMY_WIDTH - 24;
            float maxSpriteHeight = ENEMY_HEIGHT - 92;
            float scale = Math.min(maxSpriteWidth / sprite.getWidth(),
                    maxSpriteHeight / sprite.getHeight());
            float sWidth = sprite.getWidth() * scale;
            float sHeight = sprite.getHeight() * scale;
            float sx = getX() + (ENEMY_WIDTH - sWidth) / 2;
            float sy = getY() + 28;
            batch.draw(sprite, sx, sy, sWidth, sHeight);
        }

        // 绘制敌人名称
        font.draw(batch, enemy.getName(), getX() + 8, getY() + ENEMY_HEIGHT - 6);

        // 绘制生命值
        font.draw(batch, enemy.getHp() + "/" + enemy.getMaxHp(),
                getX() + 8, getY() + ENEMY_HEIGHT - 24);

        // 绘制格挡值
        if (enemy.getBlock() > 0) {
            font.draw(batch, "格挡 " + enemy.getBlock(),
                    getX() + 8, getY() + ENEMY_HEIGHT - 42);
        }

        // 绘制意图
        IntentType intent = enemy.getIntent();
        if (intent != null && intent != IntentType.UNKNOWN) {
            String intentStr = EnemyActionResolver.describeIntent(enemy.peekCurrentAction());
            font.draw(batch, intentStr, getX() + 8, getY() + ENEMY_HEIGHT - 60);
        }
    }

    /**
     * 释放纹理资源。
     */
    public void dispose() {
        if (sprite != null) sprite.dispose();
    }
}
