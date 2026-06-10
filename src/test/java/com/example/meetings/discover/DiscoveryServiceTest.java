package com.example.meetings.discover;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {

    private DiscoveredEvent makeEvent(String title, String url, String start) {
        return new DiscoveredEvent(
                "TestSource", "id-" + title, title, null,
                Instant.parse(start), null, url, null);
    }

    @Test
    void search_nullQuery_returnsEmptyList() {
        DiscoveryService service = new DiscoveryService(List.of());
        assertTrue(service.search(null).isEmpty());
    }

    @Test
    void search_blankQuery_returnsEmptyList() {
        DiscoveryService service = new DiscoveryService(List.of());
        assertTrue(service.search("   ").isEmpty());
    }

    @Test
    void search_noConfiguredProviders_returnsEmptyList() {
        EventProvider unconfigured = new EventProvider() {
            public String name() { return "Fake"; }
            public boolean isConfigured() { return false; }
            public List<DiscoveredEvent> search(String q) { return List.of(makeEvent("Event", "http://x.com", "2025-09-01T10:00:00Z")); }
        };

        DiscoveryService service = new DiscoveryService(List.of(unconfigured));
        assertTrue(service.search("test").isEmpty());
    }

    @Test
    void search_duplicateUrls_deduplicates() {
        DiscoveredEvent event1 = makeEvent("Event A", "http://same-url.com", "2025-09-01T10:00:00Z");
        DiscoveredEvent event2 = makeEvent("Event A duplicate", "http://same-url.com", "2025-09-01T10:00:00Z");

        EventProvider provider = new EventProvider() {
            public String name() { return "Fake"; }
            public boolean isConfigured() { return true; }
            public List<DiscoveredEvent> search(String q) { return List.of(event1, event2); }
        };

        DiscoveryService service = new DiscoveryService(List.of(provider));
        List<DiscoveredEvent> results = service.search("test");

        assertEquals(1, results.size());
    }

    @Test
    void search_multipleEvents_sortedByStartTime() {
        DiscoveredEvent later  = makeEvent("Later",  "http://later.com",  "2025-10-01T10:00:00Z");
        DiscoveredEvent earlier = makeEvent("Earlier", "http://earlier.com", "2025-09-01T10:00:00Z");

        EventProvider provider = new EventProvider() {
            public String name() { return "Fake"; }
            public boolean isConfigured() { return true; }
            public List<DiscoveredEvent> search(String q) { return List.of(later, earlier); }
        };

        DiscoveryService service = new DiscoveryService(List.of(provider));
        List<DiscoveredEvent> results = service.search("test");

        assertEquals("Earlier", results.get(0).title());
        assertEquals("Later", results.get(1).title());
    }

    @Test
    void search_twoProviders_mergesResults() {
        DiscoveredEvent e1 = makeEvent("Event 1", "http://url1.com", "2025-09-01T10:00:00Z");
        DiscoveredEvent e2 = makeEvent("Event 2", "http://url2.com", "2025-09-02T10:00:00Z");

        EventProvider p1 = new EventProvider() {
            public String name() { return "P1"; }
            public boolean isConfigured() { return true; }
            public List<DiscoveredEvent> search(String q) { return List.of(e1); }
        };
        EventProvider p2 = new EventProvider() {
            public String name() { return "P2"; }
            public boolean isConfigured() { return true; }
            public List<DiscoveredEvent> search(String q) { return List.of(e2); }
        };

        DiscoveryService service = new DiscoveryService(List.of(p1, p2));
        List<DiscoveredEvent> results = service.search("test");

        assertEquals(2, results.size());
    }
}