package com.fabricatedbook.core.engine;

/**
 * Immutable preview data for an enemy intent.
 */
public class EnemyIntentPreview {

    private final String detail;

    public EnemyIntentPreview(String detail) {
        this.detail = detail != null ? detail : "";
    }

    public String getDetail() {
        return detail;
    }
}
