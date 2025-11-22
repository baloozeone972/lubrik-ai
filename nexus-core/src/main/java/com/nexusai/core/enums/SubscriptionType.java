package com.nexusai.core.enums;

import lombok.Getter;

/**
 * Subscription tiers with their limits.
 */
@Getter
public enum SubscriptionType {
    FREE(100, 1, "basic"),
    STANDARD(1000, 3, "standard"),
    PREMIUM(5000, 10, "premium"),
    VIP(20000, 50, "vip"),
    VIP_PLUS(-1, -1, "all"); // Unlimited

    private final int tokensPerMonth;
    private final int maxCompanions;
    private final String features;

    SubscriptionType(int tokensPerMonth, int maxCompanions, String features) {
        this.tokensPerMonth = tokensPerMonth;
        this.maxCompanions = maxCompanions;
        this.features = features;
    }

    public boolean isUnlimited() {
        return this == VIP_PLUS;
    }

    public boolean canAccessFeature(String feature) {
        return switch (this) {
            case FREE -> "basic".equals(feature);
            case STANDARD -> "basic".equals(feature) || "standard".equals(feature);
            case PREMIUM -> !"vip".equals(feature) && !"all".equals(feature);
            case VIP, VIP_PLUS -> true;
        };
    }
}
