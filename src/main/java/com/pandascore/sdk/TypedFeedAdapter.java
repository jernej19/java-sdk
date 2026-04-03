package com.pandascore.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandascore.sdk.config.JsonMapperFactory;
import com.pandascore.sdk.model.feed.fixtures.FixtureMessage;
import com.pandascore.sdk.model.feed.markets.MarketsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Adapter that wraps a {@link FeedListener} into a {@code Consumer<Object>}
 * suitable for {@link com.pandascore.sdk.rmq.RabbitMQFeed#connect(Consumer)}.
 * <p>
 * Inspects the {@code type} field of each incoming JSON message,
 * deserializes it to the appropriate model class, and dispatches
 * to the corresponding listener method.
 */
public final class TypedFeedAdapter implements Consumer<Object> {

    private static final Logger logger = LoggerFactory.getLogger(TypedFeedAdapter.class);
    private static final ObjectMapper mapper = JsonMapperFactory.create();

    private final FeedListener listener;

    /**
     * @param listener the typed listener to dispatch to
     */
    public TypedFeedAdapter(FeedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("FeedListener must not be null");
        }
        this.listener = listener;
    }

    @Override
    public void accept(Object message) {
        if (!(message instanceof JsonNode)) {
            logger.warn("Unexpected message type: {}", message.getClass().getName());
            return;
        }
        JsonNode json = (JsonNode) message;
        String type = json.has("type") ? json.get("type").asText() : null;

        if (type == null) {
            listener.onUnknown(json);
            return;
        }

        try {
            switch (type) {
                case "markets":
                    listener.onMarkets(mapper.treeToValue(json, MarketsMessage.class));
                    break;
                case "fixture":
                    listener.onFixture(mapper.treeToValue(json, FixtureMessage.class));
                    break;
                case "scoreboard":
                    String scoreboardType = json.has("scoreboard_type")
                        ? json.get("scoreboard_type").asText()
                        : "unknown";
                    listener.onScoreboard(json, scoreboardType);
                    break;
                default:
                    listener.onUnknown(json);
                    break;
            }
        } catch (Exception e) {
            logger.error("Failed to deserialize message of type '{}', dispatching as unknown", type, e);
            listener.onUnknown(json);
        }
    }
}
