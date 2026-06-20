package com.fabricatedbook.view;

/** Parsed command line options for launching directly into frontend states. */
public class FrontendDebugConfig {

    public enum ScreenKind {
        TITLE,
        FONT_DEBUG,
        MAP,
        BATTLE,
        BATTLE_DEFEAT,
        REWARD_POTION,
        SHOP,
        EVENT,
        INVENTORY
    }

    private final ScreenKind screenKind;
    private final int layer;
    private final String eventName;

    private FrontendDebugConfig(ScreenKind screenKind, int layer, String eventName) {
        this.screenKind = screenKind;
        this.layer = layer;
        this.eventName = eventName;
    }

    public static FrontendDebugConfig title() {
        return new FrontendDebugConfig(ScreenKind.TITLE, 1, null);
    }

    public static FrontendDebugConfig fontDebug() {
        return new FrontendDebugConfig(ScreenKind.FONT_DEBUG, 1, null);
    }

    public static FrontendDebugConfig map(int layer) {
        return new FrontendDebugConfig(ScreenKind.MAP, layer, null);
    }

    public static FrontendDebugConfig battle() {
        return new FrontendDebugConfig(ScreenKind.BATTLE, 1, null);
    }

    public static FrontendDebugConfig battleDefeat() {
        return new FrontendDebugConfig(ScreenKind.BATTLE_DEFEAT, 1, null);
    }

    public static FrontendDebugConfig shop() {
        return new FrontendDebugConfig(ScreenKind.SHOP, 1, null);
    }

    public static FrontendDebugConfig event(String eventName) {
        return new FrontendDebugConfig(ScreenKind.EVENT, 1, eventName);
    }

    public static FrontendDebugConfig inventory() {
        return new FrontendDebugConfig(ScreenKind.INVENTORY, 1, null);
    }

    public static FrontendDebugConfig parse(String[] args) {
        if (args == null || args.length == 0) return title();
        String screen = null;
        int layer = 1;
        String eventName = null;

        for (String raw : args) {
            if (raw == null || raw.isBlank()) continue;
            String arg = raw.trim();
            if (arg.startsWith("--screen=")) {
                screen = arg.substring("--screen=".length());
            } else if (arg.startsWith("--layer=")) {
                layer = parseLayer(arg.substring("--layer=".length()));
            } else if (arg.startsWith("--event=")) {
                eventName = arg.substring("--event=".length());
            } else if (screen == null) {
                screen = arg;
            }
        }

        if (screen == null || screen.isBlank()) return title();
        String normalized = screen.toLowerCase();
        if (normalized.startsWith("map:")) {
            return map(parseLayer(normalized.substring("map:".length())));
        }
        return switch (normalized) {
            case "title" -> title();
            case "font", "fonts", "fontdebug", "font-debug" -> fontDebug();
            case "map" -> map(layer);
            case "battle", "fight" -> battle();
            case "battle-defeat", "defeat" -> battleDefeat();
            case "reward-potion" -> new FrontendDebugConfig(ScreenKind.REWARD_POTION, 1, null);
            case "shop" -> shop();
            case "event" -> event(eventName);
            case "inventory", "deck" -> inventory();
            default -> title();
        };
    }

    private static int parseLayer(String raw) {
        try {
            return Math.max(1, Math.min(5, Integer.parseInt(raw)));
        } catch (Exception ignored) {
            return 1;
        }
    }

    public ScreenKind getScreenKind() {
        return screenKind;
    }

    public int getLayer() {
        return layer;
    }

    public String getEventName() {
        return eventName;
    }
}
