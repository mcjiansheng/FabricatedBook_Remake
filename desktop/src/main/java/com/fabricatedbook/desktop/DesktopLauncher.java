package com.fabricatedbook.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.fabricatedbook.view.FabricBookGame;

/**
 * DesktopLauncher — Desktop 启动器
 * <p>
 * main 方法创建 Lwjgl3Application 并启动 FabricBookGame。
 * 设置窗口标题、尺寸和 V-Sync 等配置。
 * <p>
 * 引用方：游戏桌面版的可执行入口点
 */
public class DesktopLauncher {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config =
                new Lwjgl3ApplicationConfiguration();

        // 窗口配置
        config.setTitle("Fabricated Book");
        config.setWindowedMode(FabricBookGame.SCREEN_WIDTH,
                FabricBookGame.SCREEN_HEIGHT);
        config.setWindowSizeLimits(FabricBookGame.MIN_WINDOW_WIDTH,
                FabricBookGame.MIN_WINDOW_HEIGHT,
                FabricBookGame.MAX_WINDOW_WIDTH,
                FabricBookGame.MAX_WINDOW_HEIGHT);
        config.setForegroundFPS(60);
        config.useVsync(true);
        config.setResizable(true);
        config.setDecorated(true);

        // 启动游戏
        new Lwjgl3Application(new FabricBookGame(), config);
    }
}
