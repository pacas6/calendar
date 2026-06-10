package com.example.meetings.service;

import com.example.meetings.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ICalServiceTest {

    private ICalService iCalService;
    private User owner;

    @BeforeEach
    void setUp() {
        iCalService = new ICalService();
        owner = new User("alice", "alice@example.com", "hash");
    }

    @Test
    void render_emptyMeetings_containsCalendarHeaders() {
        String result = iCalService.render(owner, List.of());

        assertTrue(result.contains("BEGIN:VCALENDAR"));
        assertTrue(result.contains("END:VCALENDAR"));
        assertTrue(result.contains("VERSION:2.0"));
    }

    @Test
    void render_meetingIsConfirmed_statusIsConfirmed() {
        Meeting meeting = createMeeting("Team Sync", owner);
        // Só o organizer (ACCEPTED) → isConfirmed() = true
        meeting.addParticipant(new MeetingParticipant(meeting, owner, InviteStatus.ACCEPTED));

        String result = iCalService.render(owner, List.of(meeting));

        assertTrue(result.contains("STATUS:CONFIRMED"));
    }

    @Test
    void render_meetingHasPendingInvite_statusIsTentative() {
        Meeting meeting = createMeeting("Team Sync", owner);
        User bob = new User("bob", "bob@example.com", "hash");
        // bob ainda não respondeu → isConfirmed() = false
        meeting.addParticipant(new MeetingParticipant(meeting, owner, InviteStatus.ACCEPTED));
        meeting.addParticipant(new MeetingParticipant(meeting, bob, InviteStatus.PENDING));

        String result = iCalService.render(owner, List.of(meeting));

        assertTrue(result.contains("STATUS:TENTATIVE"));
    }

    @Test
    void render_titleWithSpecialChars_isEscaped() {
        Meeting meeting = createMeeting("Meeting, with; commas", owner);
        meeting.addParticipant(new MeetingParticipant(meeting, owner, InviteStatus.ACCEPTED));

        String result = iCalService.render(owner, List.of(meeting));

        assertTrue(result.contains("SUMMARY:Meeting\\, with\\; commas"));
    }

    @Test
    void render_usesCrlfLineEndings() {
        String result = iCalService.render(owner, List.of());

        assertTrue(result.contains("\r\n"));
    }

    @Test
    void render_containsOwnerCalendarName() {
        String result = iCalService.render(owner, List.of());

        assertTrue(result.contains("X-WR-CALNAME:alice"));
    }

    // Helper
    private Meeting createMeeting(String title, User organizer) {
        return new Meeting(
                title,
                "description",
                Instant.parse("2025-09-01T10:00:00Z"),
                Instant.parse("2025-09-01T11:00:00Z"),
                organizer
        );
    }
}
