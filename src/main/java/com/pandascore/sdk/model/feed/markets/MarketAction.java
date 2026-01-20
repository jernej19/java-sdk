package com.pandascore.sdk.model.feed.markets;

/**
 * Possible actions that can occur on a market message.
 */
public enum MarketAction {
    created, margin_changed, odds_changed, suspended,
    deactivated, settled, rollback_settlement,
    opponent_updated, partially_settled
}
