package com.example.meetings.discover.Agenda;

import com.example.meetings.discover.AgendaLxProvider;
import com.example.meetings.discover.DiscoveredEvent;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class AgendaLxProviderWireMockTest {

    private WireMockServer wireMock;
    private AgendaLxProvider provider;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        provider = new AgendaLxProvider(wireMock.baseUrl());
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void search_serverError_returnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse().withStatus(500)));

        List<DiscoveredEvent> results = provider.search("teatro");
        assertNotNull(results);
    }

    @Test
    void search_nullResponse_returnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("null")));

        List<DiscoveredEvent> results = provider.search("teatro");
        assertTrue(results.isEmpty());
    }

    @Test
    void search_eventWithNullTitle_isSkipped() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            [{
                              "id": 1,
                              "title": null,
                              "occurences": ["2099-01-01"]
                            }]
                        """)));

        List<DiscoveredEvent> results = provider.search("teatro");
        assertTrue(results.isEmpty());
    }

    @Test
    void search_eventWithBlankTitle_isSkipped() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            [{
                              "id": 1,
                              "title": { "rendered": "   " },
                              "occurences": ["2099-01-01"]
                            }]
                        """)));

        List<DiscoveredEvent> results = provider.search("teatro");
        assertTrue(results.isEmpty());
    }

    @Test
    void search_eventWithNullOccurrences_isSkipped() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            [{
                              "id": 1,
                              "title": { "rendered": "Concerto" },
                              "occurences": null
                            }]
                        """)));

        List<DiscoveredEvent> results = provider.search("teatro");
        assertTrue(results.isEmpty());
    }

    @Test
    void search_validEvent_returnsIt() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            [{
                              "id": 1,
                              "title": { "rendered": "Concerto Jazz" },
                              "occurences": ["2099-01-01"],
                              "string_times": "21h30",
                              "link": "http://agendalx.pt/event/1"
                            }]
                        """)));

        List<DiscoveredEvent> results = provider.search("jazz");
        assertEquals(1, results.size());
        assertEquals("Concerto Jazz", results.get(0).title());
    }

    @Test
    void search_networkError_returnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        List<DiscoveredEvent> results = provider.search("teatro");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
