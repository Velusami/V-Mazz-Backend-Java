package com.example.catalog.event;

/**
 * Published inside the SQL transaction; handled AFTER the commit so that
 * Elasticsearch is only touched for data that was really saved.
 */
public record ProductIndexEvent(Long productId, Action action, boolean reembed) {

    public enum Action { UPSERT, DELETE }

    public static ProductIndexEvent upsert(Long productId, boolean reembed) {
        return new ProductIndexEvent(productId, Action.UPSERT, reembed);
    }

    public static ProductIndexEvent delete(Long productId) {
        return new ProductIndexEvent(productId, Action.DELETE, false);
    }
}
