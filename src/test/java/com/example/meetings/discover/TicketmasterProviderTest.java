package com.example.meetings.discover;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class TicketmasterProviderTest {

    private WireMockServer wireMock;
    private TicketmasterProvider provider;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        provider = new TicketmasterProvider("fake-api-key", "PT", wireMock.baseUrl());
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void search_validResponse_returnsEvents() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "_embedded": {
                                "events": [{
                                  "id": "1",
                                  "name": "Concert Test",
                                  "url": "http://test.com",
                                  "dates": {
                                    "start": { "dateTime": "2025-09-01T20:00:00Z" }
                                  }
                                }]
                              }
                            }
                        """)));

        List<DiscoveredEvent> results = provider.search("concert");

        assertEquals(1, results.size());
        assertEquals("Concert Test", results.get(0).title());
    }

    @Test
    void search_emptyResponse_returnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        List<DiscoveredEvent> results = provider.search("concert");

        assertTrue(results.isEmpty());
    }

    @Test
    void search_serverError_returnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse().withStatus(500)));

        List<DiscoveredEvent> results = provider.search("concert");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}