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

    @Test
    void search_eventWithoutDateTime_isSkipped() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "_embedded": {
                            "events": [{
                              "id": "1",
                              "name": "TBA Event",
                              "url": "http://test.com",
                              "dates": {
                                "start": { "dateTime": null }
                              }
                            }]
                          }
                        }
                    """)));

        List<DiscoveredEvent> results = provider.search("concert");

        assertTrue(results.isEmpty());
    }

    @Test
    void search_eventWithInvalidDateTime_isSkipped() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "_embedded": {
                            "events": [{
                              "id": "1",
                              "name": "Bad Event",
                              "url": "http://test.com",
                              "dates": {
                                "start": { "dateTime": "not-a-date" }
                              }
                            }]
                          }
                        }
                    """)));

        List<DiscoveredEvent> results = provider.search("concert");

        assertTrue(results.isEmpty());
    }

    @Test
    void search_eventWithVenue_venueIsSet() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "_embedded": {
                            "events": [{
                              "id": "1",
                              "name": "Concert",
                              "url": "http://test.com",
                              "dates": {
                                "start": { "dateTime": "2025-09-01T20:00:00Z" }
                              },
                              "_embedded": {
                                "venues": [{ "name": "Altice Arena" }]
                              }
                            }]
                          }
                        }
                    """)));

        List<DiscoveredEvent> results = provider.search("concert");

        assertEquals(1, results.size());
        assertEquals("Altice Arena", results.get(0).venue());
    }

    @Test
    void search_eventWithoutVenue_venueIsNull() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "_embedded": {
                            "events": [{
                              "id": "1",
                              "name": "Concert",
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
        assertNull(results.get(0).venue());
    }

    @Test
    void isConfigured_withApiKey_returnsTrue() {
        assertTrue(provider.isConfigured());
    }

    @Test
    void isConfigured_withoutApiKey_returnsFalse() {
        TicketmasterProvider unconfigured = new TicketmasterProvider("", "PT", wireMock.baseUrl());
        assertFalse(unconfigured.isConfigured());
    }

    @Test
    void search_noCountryCode_stillWorks() {
        TicketmasterProvider noCountry = new TicketmasterProvider("fake-api-key", "", wireMock.baseUrl());
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "_embedded": {
                            "events": [{
                              "id": "1",
                              "name": "Concert",
                              "url": "http://test.com",
                              "dates": {
                                "start": { "dateTime": "2025-09-01T20:00:00Z" }
                              }
                            }]
                          }
                        }
                    """)));

        List<DiscoveredEvent> results = noCountry.search("concert");
        assertEquals(1, results.size());
    }

    @Test
    void search_eventWithoutDates_isSkipped() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "_embedded": {
                            "events": [{
                              "id": "1",
                              "name": "No Dates Event",
                              "url": "http://test.com"
                            }]
                          }
                        }
                    """)));

        List<DiscoveredEvent> results = provider.search("concert");
        assertTrue(results.isEmpty());
    }

    @Test
    void search_eventWithEmptyVenuesList_venueIsNull() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "_embedded": {
                            "events": [{
                              "id": "1",
                              "name": "Concert",
                              "url": "http://test.com",
                              "dates": {
                                "start": { "dateTime": "2025-09-01T20:00:00Z" }
                              },
                              "_embedded": {
                                "venues": []
                              }
                            }]
                          }
                        }
                    """)));

        List<DiscoveredEvent> results = provider.search("concert");
        assertEquals(1, results.size());
        assertNull(results.get(0).venue());
    }

    @Test
    void constructor_default_isConfiguredWithRealUrl() {
        // Testa o construtor principal (2 argumentos)
        TicketmasterProvider real = new TicketmasterProvider("my-key", "PT");
        assertTrue(real.isConfigured());
    }

    @Test
    void isConfigured_nullApiKey_returnsFalse() {
        TicketmasterProvider nullKey = new TicketmasterProvider(null, "PT", wireMock.baseUrl());
        assertFalse(nullKey.isConfigured());
    }

    @Test
    void search_notConfigured_returnsEmptyList() {
        TicketmasterProvider unconfigured = new TicketmasterProvider(null, "PT", wireMock.baseUrl());
        assertTrue(unconfigured.search("concert").isEmpty());
    }

    @Test
    void search_bodyIsNull_returnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("null")));

        List<DiscoveredEvent> results = provider.search("concert");
        assertTrue(results.isEmpty());
    }

    @Test
    void search_eventWithNullDatesStart_isSkipped() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "_embedded": {
                            "events": [{
                              "id": "1",
                              "name": "No Start",
                              "url": "http://test.com",
                              "dates": {}
                            }]
                          }
                        }
                    """)));

        List<DiscoveredEvent> results = provider.search("concert");
        assertTrue(results.isEmpty());
    }

    @Test
    void search_networkTimeout_returnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        List<DiscoveredEvent> results = provider.search("concert");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void search_nullCountryCode_stillWorks() {
        TicketmasterProvider nullCountry = new TicketmasterProvider("fake-api-key", null, wireMock.baseUrl());
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "_embedded": {
                            "events": [{
                              "id": "1",
                              "name": "Concert",
                              "url": "http://test.com",
                              "dates": {
                                "start": { "dateTime": "2025-09-01T20:00:00Z" }
                              }
                            }]
                          }
                        }
                    """)));

        List<DiscoveredEvent> results = nullCountry.search("concert");
        assertEquals(1, results.size());
    }

    @Test
    void search_embeddedIsNull_returnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"_embedded\": null}")));

        List<DiscoveredEvent> results = provider.search("concert");
        assertTrue(results.isEmpty());
    }

    @Test
    void search_eventWithNullVenuesList_venueIsNull() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "_embedded": {
                            "events": [{
                              "id": "1",
                              "name": "Concert",
                              "url": "http://test.com",
                              "dates": {
                                "start": { "dateTime": "2025-09-01T20:00:00Z" }
                              },
                              "_embedded": {
                                "venues": null
                              }
                            }]
                          }
                        }
                    """)));

        List<DiscoveredEvent> results = provider.search("concert");
        assertEquals(1, results.size());
        assertNull(results.get(0).venue());
    }

    @Test
    void search_eventsIsNull_returnsEmptyList() {
        wireMock.stubFor(get(urlMatching("/events.json.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "_embedded": {
                            "events": null
                          }
                        }
                    """)));

        List<DiscoveredEvent> results = provider.search("concert");
        assertTrue(results.isEmpty());
    }
}