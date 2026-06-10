package com.example.meetings.discover;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class SeatGeekProviderTest {

    private WireMockServer wireMock;
    private SeatGeekProvider provider;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        provider = new SeatGeekProvider("fake-client-id", wireMock.baseUrl());
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void search_validResponse_returnsEvents() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "events": [{
                                "id": 1,
                                "title": "Rock Concert",
                                "datetime_utc": "2025-09-01T20:00:00",
                                "url": "http://seatgeek.com/event/1",
                                "venue": { "name": "Altice Arena" }
                              }]
                            }
                        """)));

        List<DiscoveredEvent> results = provider.search("rock");

        assertEquals(1, results.size());
        assertEquals("Rock Concert", results.get(0).title());
        assertEquals("Altice Arena", results.get(0).venue());
    }

    @Test
    void search_emptyResponse_returnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"events\": []}")));

        List<DiscoveredEvent> results = provider.search("rock");

        assertTrue(results.isEmpty());
    }

    @Test
    void search_serverError_returnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse().withStatus(500)));

        List<DiscoveredEvent> results = provider.search("rock");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void search_notConfigured_returnsEmptyList() {
        SeatGeekProvider unconfigured = new SeatGeekProvider("", wireMock.baseUrl());

        List<DiscoveredEvent> results = unconfigured.search("rock");

        assertTrue(results.isEmpty());
    }
}