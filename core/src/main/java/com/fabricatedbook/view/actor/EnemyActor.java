package com.fabricatedbook.view.actor;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.fabricatedbook.core.buff.BuffHook;
import com.fabricatedbook.core.engine.EnemyIntentPreview;
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
    private IntentPreviewProvider intentPreviewProvider;
    private final Map<String, Texture> buffIcons = new HashMap<>();
    private final Map<IntentType, Texture> intentIcons = new HashMap<>();

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
        loadIntentIcons();
        loadBuffIcons();
    }

    private void loadIntentIcons() {
        loadIntentIcon(IntentType.ATTACK, "atk");
        loadIntentIcon(IntentType.DEFEND, "def");
        loadIntentIcon(IntentType.BUFF, "inc");
        loadIntentIcon(IntentType.DEBUFF, "weak");
        loadIntentIcon(IntentType.UNKNOWN, "ukn");
    }

    private void loadIntentIcon(IntentType intentType, String fileName) {
        try {
            intentIcons.put(intentType, new Texture("img/" + fileName + ".png"));
        } catch (Exception ignored) {
            // Text fallback is drawn if the image is missing.
        }
    }

    private void loadBuffIcons() {
        String[] names = {"Poison", "Fragile", "Weak", "Withering", "Strength",
                "Resistance", "ArmorBuff", "BlockIncrease", "BlockReduction",
                "Dizziness", "ExtraEnergyBuff", "UndeadBuff"};
        for (String name : names) {
            try {
                buffIcons.put(name, new Texture("img/" + buffIconFile(name) + ".png"));
            } catch (Exception ignored) {
                // Missing icons fall back to text in drawBuffs.
            }
        }
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    public void setIntentPreviewProvider(IntentPreviewProvider intentPreviewProvider) {
        this.intentPreviewProvider = intentPreviewProvider;
    }

    public interface IntentPreviewProvider {
        EnemyIntentPreview getIntentPreview(Enemy enemy);
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
        shapeRenderer.rect(getX() + 14, getY() + 10, hpBarWidth, 10);
        if (hpRatio > 0.5f) {
            shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1f);
        } else if (hpRatio > 0.25f) {
            shapeRenderer.setColor(0.8f, 0.8f, 0.2f, 1f);
        } else {
            shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 1f);
        }
        shapeRenderer.rect(getX() + 14, getY() + 10, hpBarWidth * hpRatio, 10);
        if (enemy.getBlock() > 0) {
            shapeRenderer.setColor(0.25f, 0.45f, 0.95f, 1f);
            shapeRenderer.rect(getX() + 14, getY() + 24,
                    Math.min(hpBarWidth, enemy.getBlock() * 6f), 6);
        }
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

        // 绘制敌人名称、意图、生命和状态。后端 action id 不直接露出。
        font.draw(batch, enemy.getName(), getX() + 8, getY() + ENEMY_HEIGHT - 8);
        drawIntent(batch);

        font.draw(batch, enemy.getHp() + "/" + enemy.getMaxHp(),
                getX() + 44, getY() + 22);
        if (enemy.getBlock() > 0) {
            font.draw(batch, String.valueOf(enemy.getBlock()), getX() + 44, getY() + 36);
        }
        drawBuffs(batch);
    }

    private void drawIntent(Batch batch) {
        IntentType intent = enemy.getIntent() != null ? enemy.getIntent() : IntentType.UNKNOWN;
        Texture icon = intentIcons.getOrDefault(intent, intentIcons.get(IntentType.UNKNOWN));
        float iconX = getX() + ENEMY_WIDTH / 2f - 24;
        float iconY = getY() + ENEMY_HEIGHT - 64;
        if (icon != null) {
            batch.draw(icon, iconX, iconY, 32, 32);
        }
        EnemyIntentPreview preview = intentPreviewProvider != null
                ? intentPreviewProvider.getIntentPreview(enemy)
                : new EnemyIntentPreview(intentDetail(
                        EnemyActionResolver.describeIntent(enemy.peekCurrentAction())));
        float detailX = iconX + 38;
        if (preview.hasDebuff()) {
            Texture debuffIcon = intentIcons.get(IntentType.DEBUFF);
            if (debuffIcon != null) {
                batch.draw(debuffIcon, iconX + 34, iconY, 32, 32);
                detailX = iconX + 72;
            }
        }
        String detail = preview.getDetail();
        if (preview.hasDebuff()) {
            detail = detail.isBlank()
                    ? preview.getDebuffDetail()
                    : detail + " / " + preview.getDebuffDetail();
        }
        if (!detail.isBlank()) {
            font.draw(batch, detail, detailX, iconY + 23);
        }
    }

    private String intentDetail(String description) {
        if (description == null || description.isBlank()) return "";
        String cleaned = description
                .replace("攻击", "")
                .replace("防御", "")
                .replace("强化", "")
                .replace("削弱", "")
                .replace("未知", "")
                .replace("回复", "")
                .replace("群体", "")
                .replace("/", "")
                .trim();
        return cleaned.length() > 12 ? cleaned.substring(0, 12) : cleaned;
    }

    private void drawBuffs(Batch batch) {
        float x = getX() + 12;
        float y = getY() - 30;
        int shown = 0;
        for (BuffHook buff : enemy.getBuffs()) {
            if (buff.getStack() <= 0) continue;
            Texture icon = buffIcons.get(buff.getBuffName());
            float iconX = x + shown * 38f;
            if (icon != null) {
                batch.draw(icon, iconX, y, 26, 26);
            } else {
                font.draw(batch, buffLabel(buff.getBuffName()), iconX, y + 22);
            }
            font.draw(batch, String.valueOf(buff.getStack()), iconX + 20, y + 10);
            shown++;
        }
    }

    private String buffIconFile(String buffName) {
        return switch (buffName) {
            case "Poison" -> "poisoning";
            case "Fragile" -> "fragile";
            case "Weak" -> "weak";
            case "Withering" -> "withering";
            case "Strength" -> "strength";
            case "Resistance" -> "resistance";
            case "ArmorBuff" -> "armor";
            case "BlockIncrease" -> "block_increase";
            case "BlockReduction" -> "block_reduction";
            case "Dizziness" -> "dizziness";
            case "ExtraEnergyBuff" -> "extra_energy";
            case "UndeadBuff" -> "undead";
            default -> "ukn";
        };
    }

    private String buffLabel(String buffName) {
        return switch (buffName) {
            case "Poison" -> "毒";
            case "Fragile" -> "脆";
            case "Weak" -> "弱";
            case "Withering" -> "凋";
            case "Strength" -> "力";
            case "ArmorBuff" -> "甲";
            case "ExtraEnergyBuff" -> "能";
            case "UndeadBuff" -> "死";
            default -> "?";
        };
    }

    /**
     * 释放纹理资源。
     */
    public void dispose() {
        if (sprite != null) sprite.dispose();
        for (Texture texture : intentIcons.values()) {
            texture.dispose();
        }
        for (Texture texture : buffIcons.values()) {
            texture.dispose();
        }
    }
}
