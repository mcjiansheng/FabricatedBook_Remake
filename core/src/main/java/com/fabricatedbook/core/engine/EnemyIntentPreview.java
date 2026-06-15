package com.fabricatedbook.core.engine;

/**
 * Immutable preview data for an enemy intent.
 */
public class EnemyIntentPreview {

    private final String detail;
    private final String debuffDetail;

    public EnemyIntentPreview(String detail) {
        this(detail, "");
    }

    public EnemyIntentPreview(String detail, String debuffDetail) {
        this.detail = detail != null ? detail : "";
        this.debuffDetail = debuffDetail != null ? debuffDetail : "";
    }

    public String getDetail() {
        return detail;
    }

    public String getDebuffDetail() {
        return debuffDetail;
    }

    public boolean hasDebuff() {
        return !debuffDetail.isBlank();
    }
}
