package com.pandascore.sdk.model.feed.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FixtureActionTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void testDeserialize_opponentsSwapped() throws Exception {
        String json = "{\"action\":\"opponents_swapped\",\"type\":\"fixture\",\"event_type\":\"match\"}";
        FixtureMessage msg = mapper.readValue(json, FixtureMessage.class);
        assertEquals(FixtureAction.opponents_swapped, msg.getAction());
        assertEquals("opponents_swapped", msg.getAction().getValue());
    }

    @Test
    void testDeserialize_opponentsUpdated() throws Exception {
        String json = "{\"action\":\"opponents_updated\",\"type\":\"fixture\",\"event_type\":\"match\"}";
        FixtureMessage msg = mapper.readValue(json, FixtureMessage.class);
        assertEquals(FixtureAction.opponents_updated, msg.getAction());
        assertEquals("opponents_updated", msg.getAction().getValue());
    }

    @Test
    void testDeserialize_knownAction_updated() throws Exception {
        String json = "{\"action\":\"updated\",\"type\":\"fixture\"}";
        FixtureMessage msg = mapper.readValue(json, FixtureMessage.class);
        assertEquals(FixtureAction.updated, msg.getAction());
        assertEquals("updated", msg.getAction().getValue());
    }

    @Test
    void testDeserialize_unknownAction_mapsToUnknown() throws Exception {
        String json = "{\"action\":\"some_future_action\",\"type\":\"fixture\"}";
        FixtureMessage msg = mapper.readValue(json, FixtureMessage.class);
        assertEquals(FixtureAction.UNKNOWN, msg.getAction());
    }

    @Test
    void testDeserialize_nullAction() throws Exception {
        String json = "{\"type\":\"fixture\"}";
        FixtureMessage msg = mapper.readValue(json, FixtureMessage.class);
        assertNull(msg.getAction());
    }

    @Test
    void testFromValue_allKnownActions() {
        assertEquals(FixtureAction.created, FixtureAction.fromValue("created"));
        assertEquals(FixtureAction.booked, FixtureAction.fromValue("booked"));
        assertEquals(FixtureAction.unbooked, FixtureAction.fromValue("unbooked"));
        assertEquals(FixtureAction.reviewed, FixtureAction.fromValue("reviewed"));
        assertEquals(FixtureAction.updated, FixtureAction.fromValue("updated"));
        assertEquals(FixtureAction.rescheduled, FixtureAction.fromValue("rescheduled"));
        assertEquals(FixtureAction.postponed, FixtureAction.fromValue("postponed"));
        assertEquals(FixtureAction.started, FixtureAction.fromValue("started"));
        assertEquals(FixtureAction.finished, FixtureAction.fromValue("finished"));
        assertEquals(FixtureAction.settled, FixtureAction.fromValue("settled"));
        assertEquals(FixtureAction.coverage_changed, FixtureAction.fromValue("coverage_changed"));
        assertEquals(FixtureAction.canceled, FixtureAction.fromValue("canceled"));
        assertEquals(FixtureAction.deleted, FixtureAction.fromValue("deleted"));
        assertEquals(FixtureAction.opponents_swapped, FixtureAction.fromValue("opponents_swapped"));
        assertEquals(FixtureAction.opponents_updated, FixtureAction.fromValue("opponents_updated"));
        assertEquals(FixtureAction.live_available, FixtureAction.fromValue("live_available"));
        assertEquals(FixtureAction.live_not_available, FixtureAction.fromValue("live_not_available"));
    }

    @Test
    void testFromValue_unknownString_returnsUnknown() {
        assertEquals(FixtureAction.UNKNOWN, FixtureAction.fromValue("never_seen_before"));
    }

    @Test
    void testFromValue_null_returnsNull() {
        assertNull(FixtureAction.fromValue(null));
    }
}
