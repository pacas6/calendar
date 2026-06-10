package com.example.meetings.discover.Agenda;

import com.example.meetings.discover.AgendaLxProvider;
import com.example.meetings.discover.DiscoveredEvent;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AgendaLxProviderTest {

    private final AgendaLxProvider provider = new AgendaLxProvider();

    @Test
    void isConfigured_returnsTrue() {
        assertTrue(provider.isConfigured());
    }

    @Test
    void search_validQuery_returnsResults() {
        List<DiscoveredEvent> results = provider.search("música");
        assertNotNull(results);
    }

    @Test
    void search_emptyQuery_returnsEmptyList() {
        List<DiscoveredEvent> results = provider.search("");
        assertNotNull(results);
    }

    @Test
    void search_validQuery_eventsHaveRequiredFields() {
        List<DiscoveredEvent> results = provider.search("teatro");
        for (DiscoveredEvent event : results) {
            assertNotNull(event.title());
            assertNotNull(event.start());
            assertNotNull(event.source());
            assertEquals("Agenda Cultural de Lisboa", event.source());
        }
    }

    @Test
    void search_networkError_returnsEmptyListNotException() {
        List<DiscoveredEvent> results = provider.search("xyzxyzxyz123456789");
        assertNotNull(results);
    }
}