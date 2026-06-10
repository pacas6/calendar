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

    @Test
    void constructor_default_isConfigured() {
        SeatGeekProvider real = new SeatGeekProvider("my-client-id");
        assertTrue(real.isConfigured());
    }

    @Test
    void isConfigured_nullClientId_returnsFalse() {
        SeatGeekProvider nullId = new SeatGeekProvider(null, wireMock.baseUrl());
        assertFalse(nullId.isConfigured());
    }

    @Test
    void search_nullClientId_returnsEmptyList() {
        SeatGeekProvider nullId = new SeatGeekProvider(null, wireMock.baseUrl());
        assertTrue(nullId.search("rock").isEmpty());
    }

    @Test
    void search_bodyIsNull_returnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("null")));

        List<DiscoveredEvent> results = provider.search("rock");
        assertTrue(results.isEmpty());
    }

    @Test
    void search_eventWithoutDatetime_isSkipped() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "events": [{
                            "id": 1,
                            "title": "No Date Event"
                          }]
                        }
                    """)));

        List<DiscoveredEvent> results = provider.search("rock");
        assertTrue(results.isEmpty());
    }

    @Test
    void search_eventWithInvalidDatetime_isSkipped() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "events": [{
                            "id": 1,
                            "title": "Bad Date",
                            "datetime_utc": "not-a-date"
                          }]
                        }
                    """)));

        List<DiscoveredEvent> results = provider.search("rock");
        assertTrue(results.isEmpty());
    }

    @Test
    void search_eventWithoutVenue_venueIsNull() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "events": [{
                            "id": 1,
                            "title": "No Venue",
                            "datetime_utc": "2025-09-01T20:00:00"
                          }]
                        }
                    """)));

        List<DiscoveredEvent> results = provider.search("rock");
        assertEquals(1, results.size());
        assertNull(results.get(0).venue());
    }

    @Test
    void search_eventWithShortTitleOnly_usesShortTitle() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "events": [{
                            "id": 1,
                            "short_title": "Short Title",
                            "datetime_utc": "2025-09-01T20:00:00"
                          }]
                        }
                    """)));

        List<DiscoveredEvent> results = provider.search("rock");
        assertEquals(1, results.size());
        assertEquals("Short Title", results.get(0).title());
    }

    @Test
    void search_networkError_returnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        List<DiscoveredEvent> results = provider.search("rock");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void search_eventsFieldIsNull_returnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/events.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"events\": null}")));

        List<DiscoveredEvent> results = provider.search("rock");
        assertTrue(results.isEmpty());
    }
}