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

    @Test
    void render_attendeeAccepted_partStatIsAccepted() {
        Meeting meeting = createMeeting("Sync", owner);
        meeting.addParticipant(new MeetingParticipant(meeting, owner, InviteStatus.ACCEPTED));

        String result = iCalService.render(owner, List.of(meeting));

        assertTrue(result.contains("PARTSTAT=ACCEPTED"));
    }

    @Test
    void render_attendeeDeclined_partStatIsDeclined() {
        User bob = new User("bob", "bob@example.com", "hash");
        Meeting meeting = createMeeting("Sync", owner);
        meeting.addParticipant(new MeetingParticipant(meeting, owner, InviteStatus.ACCEPTED));
        meeting.addParticipant(new MeetingParticipant(meeting, bob, InviteStatus.DECLINED));

        String result = iCalService.render(owner, List.of(meeting));

        assertTrue(result.contains("PARTSTAT=DECLINED"));
    }

    @Test
    void render_attendeePending_partStatIsNeedsAction() {
        User bob = new User("bob", "bob@example.com", "hash");
        Meeting meeting = createMeeting("Sync", owner);
        meeting.addParticipant(new MeetingParticipant(meeting, owner, InviteStatus.ACCEPTED));
        meeting.addParticipant(new MeetingParticipant(meeting, bob, InviteStatus.PENDING));

        String result = iCalService.render(owner, List.of(meeting));

        assertTrue(result.contains("PARTSTAT=NEEDS-ACTION"));
    }

    @Test
    void render_descriptionWithNewline_isEscaped() {
        Meeting meeting = new Meeting(
                "Sync",
                "line1\nline2",
                Instant.parse("2025-09-01T10:00:00Z"),
                Instant.parse("2025-09-01T11:00:00Z"),
                owner);
        meeting.addParticipant(new MeetingParticipant(meeting, owner, InviteStatus.ACCEPTED));

        String result = iCalService.render(owner, List.of(meeting));

        assertTrue(result.contains("DESCRIPTION:line1\\nline2"));
    }

    @Test
    void render_descriptionWithBackslash_isEscaped() {
        Meeting meeting = new Meeting(
                "Sync",
                "path\\to\\file",
                Instant.parse("2025-09-01T10:00:00Z"),
                Instant.parse("2025-09-01T11:00:00Z"),
                owner);
        meeting.addParticipant(new MeetingParticipant(meeting, owner, InviteStatus.ACCEPTED));

        String result = iCalService.render(owner, List.of(meeting));

        assertTrue(result.contains("DESCRIPTION:path\\\\to\\\\file"));
    }

    @Test
    void render_noDescription_descriptionLineAbsent() {
        Meeting meeting = new Meeting(
                "Sync",
                null,
                Instant.parse("2025-09-01T10:00:00Z"),
                Instant.parse("2025-09-01T11:00:00Z"),
                owner);
        meeting.addParticipant(new MeetingParticipant(meeting, owner, InviteStatus.ACCEPTED));

        String result = iCalService.render(owner, List.of(meeting));

        assertFalse(result.contains("DESCRIPTION:"));
    }

    @Test
    void render_blankDescription_descriptionLineAbsent() {
        Meeting meeting = new Meeting(
                "Sync",
                "   ",  // blank mas não null
                Instant.parse("2025-09-01T10:00:00Z"),
                Instant.parse("2025-09-01T11:00:00Z"),
                owner);
        meeting.addParticipant(new MeetingParticipant(meeting, owner, InviteStatus.ACCEPTED));

        String result = iCalService.render(owner, List.of(meeting));

        assertFalse(result.contains("DESCRIPTION:"));
    }

    @Test
    void render_nullEmail_doesNotCrash() {
        // Testa o escape() com valor null — via organizer com email null
        User userNullEmail = new User("nomail", null, "hash");
        Meeting meeting = new Meeting(
                "Sync", null,
                Instant.parse("2025-09-01T10:00:00Z"),
                Instant.parse("2025-09-01T11:00:00Z"),
                userNullEmail);
        meeting.addParticipant(new MeetingParticipant(meeting, userNullEmail, InviteStatus.ACCEPTED));

        assertDoesNotThrow(() -> iCalService.render(userNullEmail, List.of(meeting)));
    }
}
