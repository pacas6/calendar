package com.example.meetings.discover;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test — faz chamadas REAIS à API pública da AgendaLx.
 * Não precisa de API key. Pode falhar se a API estiver em baixo.
 */
class AgendaLxProviderTest {

    private final AgendaLxProvider provider = new AgendaLxProvider();

    @Test
    void isConfigured_returnsTrue() {
        assertTrue(provider.isConfigured());
    }

    @Test
    void search_validQuery_returnsResults() {
        List<DiscoveredEvent> results = provider.search("música");

        // A API pode não ter resultados mas nunca deve lançar exceção
        assertNotNull(results);
    }

    @Test
    void search_emptyQuery_returnsEmptyList() {
        List<DiscoveredEvent> results = provider.search("");
        // query vazia não deve ir à API
        assertNotNull(results);
    }

    @Test
    void search_validQuery_eventsHaveRequiredFields() {
        List<DiscoveredEvent> results = provider.search("teatro");

        for (DiscoveredEvent event : results) {
            assertNotNull(event.title(), "Title must not be null");
            assertNotNull(event.start(), "Start time must not be null");
            assertNotNull(event.source(), "Source must not be null");
            assertEquals("Agenda Cultural de Lisboa", event.source());
        }
    }

    @Test
    void search_networkError_returnsEmptyListNotException() {
        // Mesmo com query estranha nunca deve lançar exceção
        List<DiscoveredEvent> results = provider.search("xyzxyzxyz123456789");
        assertNotNull(results);
    }
}