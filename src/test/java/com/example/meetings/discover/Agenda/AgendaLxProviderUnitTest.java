package com.example.meetings.discover.Agenda;

import com.example.meetings.discover.AgendaLxProvider;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AgendaLxProviderUnitTest {

    @Test
    void nextOccurrence_nullList_returnsNull() {
        assertNull(AgendaLxProvider.nextOccurrence(null));
    }

    @Test
    void nextOccurrence_emptyList_returnsNull() {
        assertNull(AgendaLxProvider.nextOccurrence(List.of()));
    }

    @Test
    void nextOccurrence_allPastDates_returnsNull() {
        assertNull(AgendaLxProvider.nextOccurrence(List.of("2000-01-01", "2001-01-01")));
    }

    @Test
    void nextOccurrence_futureDatePresent_returnsIt() {
        String future = LocalDate.now().plusDays(10).toString();
        LocalDate result = AgendaLxProvider.nextOccurrence(List.of(future));
        assertNotNull(result);
    }

    @Test
    void nextOccurrence_invalidDate_isIgnored() {
        String future = LocalDate.now().plusDays(10).toString();
        LocalDate result = AgendaLxProvider.nextOccurrence(List.of("not-a-date", future));
        assertNotNull(result);
    }

    // --- parseTime() ---

    @Test
    void parseTime_nullString_returnsEmpty() {
        assertTrue(AgendaLxProvider.parseTime(null).isEmpty());
    }

    @Test
    void parseTime_blankString_returnsEmpty() {
        assertTrue(AgendaLxProvider.parseTime("   ").isEmpty());
    }

    @Test
    void parseTime_noTimePattern_returnsEmpty() {
        assertTrue(AgendaLxProvider.parseTime("sem hora").isEmpty());
    }

    @Test
    void parseTime_validTime_returnsIt() {
        var result = AgendaLxProvider.parseTime("21h30");
        assertTrue(result.isPresent());
        assertEquals(LocalTime.of(21, 30), result.get());
    }

    @Test
    void parseTime_validTimeNoMinutes_returnsIt() {
        var result = AgendaLxProvider.parseTime("20h");
        assertTrue(result.isPresent());
        assertEquals(LocalTime.of(20, 0), result.get());
    }

    @Test
    void parseTime_invalidHour_returnsEmpty() {
        assertTrue(AgendaLxProvider.parseTime("25h00").isEmpty());
    }

    @Test
    void parseTime_invalidMinutes_returnsEmpty() {
        assertTrue(AgendaLxProvider.parseTime("20h60").isEmpty());
    }

    // --- firstVenueName() ---

    @Test
    void firstVenueName_nullMap_returnsNull() {
        assertNull(AgendaLxProvider.firstVenueName(null));
    }

    @Test
    void firstVenueName_emptyMap_returnsNull() {
        assertNull(AgendaLxProvider.firstVenueName(Map.of()));
    }

    @Test
    void firstVenueName_withVenue_returnsName() {
        AgendaLxProvider.AlxVenue venue = new AgendaLxProvider.AlxVenue();
        venue.name = "Teatro Nacional";
        String result = AgendaLxProvider.firstVenueName(Map.of("1", venue));
        assertEquals("Teatro Nacional", result);
    }

    // --- joinDescription() ---

    @Test
    void joinDescription_nullList_returnsNull() {
        assertNull(AgendaLxProvider.joinDescription(null));
    }

    @Test
    void joinDescription_emptyList_returnsNull() {
        assertNull(AgendaLxProvider.joinDescription(List.of()));
    }

    @Test
    void joinDescription_withHtmlTags_stripsHtml() {
        String result = AgendaLxProvider.joinDescription(List.of("<p>Hello</p>"));
        assertNotNull(result);
        assertFalse(result.contains("<p>"));
        assertTrue(result.contains("Hello"));
    }

    @Test
    void joinDescription_longDescription_isTruncated() {
        String longText = "a".repeat(700);
        String result = AgendaLxProvider.joinDescription(List.of(longText));
        assertNotNull(result);
        assertTrue(result.endsWith("…"));
        assertTrue(result.length() <= 605);
    }

    @Test
    void joinDescription_withNullParts_skipsNull() {
        List<String> parts = new java.util.ArrayList<>();
        parts.add(null);
        parts.add("valid text");
        String result = AgendaLxProvider.joinDescription(parts);
        assertNotNull(result);
        assertTrue(result.contains("valid text"));
    }

    // --- firstVenueName com venue null ---
    @Test
    void firstVenueName_venueValueIsNull_returnsNull() {
        Map<String, AgendaLxProvider.AlxVenue> map = new java.util.HashMap<>();
        map.put("1", null);  // valor null no map
        assertNull(AgendaLxProvider.firstVenueName(map));
    }

    // --- parseTime com hora negativa (impossível pelo regex mas cobre o branch) ---
    @Test
    void parseTime_validHourZero_returnsIt() {
        var result = AgendaLxProvider.parseTime("0h00");
        assertTrue(result.isPresent());
        assertEquals(LocalTime.of(0, 0), result.get());
    }

    @Test
    void parseTime_hourAt23_returnsIt() {
        var result = AgendaLxProvider.parseTime("23h59");
        assertTrue(result.isPresent());
        assertEquals(LocalTime.of(23, 59), result.get());
    }

    @Test
    void parseTime_hourExactly24_returnsEmpty() {
        assertTrue(AgendaLxProvider.parseTime("24h00").isEmpty());
    }

    @Test
    void parseTime_minutesExactly60_returnsEmpty() {
        assertTrue(AgendaLxProvider.parseTime("20h60").isEmpty());
    }

    @Test
    void parseTime_minutesAt59_returnsIt() {
        var result = AgendaLxProvider.parseTime("10h59");
        assertTrue(result.isPresent());
        assertEquals(LocalTime.of(10, 59), result.get());
    }
}