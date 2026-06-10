package com.example.meetings.service;

import com.example.meetings.model.*;
import com.example.meetings.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private MeetingParticipantRepository participantRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private MeetingService meetingService;

    private User organizer;
    private Instant start;
    private Instant end;

    @BeforeEach
    void setUp() {
        organizer = new User("alice", "alice@example.com", "hash");
        start = Instant.parse("2025-09-01T10:00:00Z");
        end   = Instant.parse("2025-09-01T11:00:00Z");
    }

    @Test
    void propose_endBeforeStart_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                meetingService.propose(organizer, "Title", "Desc", end, start, List.of())
        );
    }

    @Test
    void propose_endEqualsStart_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                meetingService.propose(organizer, "Title", "Desc", start, start, List.of())
        );
    }

    @Test
    void propose_organizerIsAutoAccepted() {
        when(meetingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Meeting meeting = meetingService.propose(organizer, "Title", "Desc", start, end, List.of());

        boolean organizerAccepted = meeting.getParticipants().stream()
                .filter(p -> p.getUser().equals(organizer))
                .anyMatch(p -> p.getStatus() == InviteStatus.ACCEPTED);
        assertTrue(organizerAccepted);
    }

    @Test
    void propose_unknownInvitee_throwsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                meetingService.propose(organizer, "Title", "Desc", start, end, List.of("unknown"))
        );
    }

    @Test
    void propose_duplicateInviteesAreIgnored() {
        User bob = new User("bob", "bob@example.com", "hash");
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(meetingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Meeting meeting = meetingService.propose(organizer, "Title", "Desc", start, end,
                List.of("bob", "bob"));

        assertEquals(2, meeting.getParticipants().size());
    }

    @Test
    void respond_invalidStatus_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                meetingService.respond(1L, organizer, InviteStatus.PENDING)
        );
    }

    @Test
    void respond_noInviteFound_throwsException() {
        when(participantRepository.findByMeetingIdAndUserId(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                meetingService.respond(1L, organizer, InviteStatus.ACCEPTED)
        );
    }

    @Test
    void calendarForIcalToken_validToken_returnsMeetings() {
        User alice = new User("alice", "alice@example.com", "hash");
        when(userRepository.findByIcalToken(alice.getIcalToken())).thenReturn(Optional.of(alice));
        when(meetingRepository.findCalendarMeetings(alice)).thenReturn(List.of());

        List<Meeting> result = meetingService.calendarForIcalToken(alice.getIcalToken());

        assertNotNull(result);
    }

    @Test
    void calendarForIcalToken_invalidToken_throwsException() {
        when(userRepository.findByIcalToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                meetingService.calendarForIcalToken("invalid-token")
        );
    }


    @Test
    void calendarFor_returnsUserMeetings() {
        when(meetingRepository.findCalendarMeetings(organizer)).thenReturn(List.of());

        List<Meeting> result = meetingService.calendarFor(organizer);

        assertNotNull(result);
        verify(meetingRepository).findCalendarMeetings(organizer);
    }

    @Test
    void pendingInvitesFor_returnsPendingInvites() {
        when(participantRepository.findByUserAndStatus(organizer, InviteStatus.PENDING))
                .thenReturn(List.of());

        var result = meetingService.pendingInvitesFor(organizer);

        assertNotNull(result);
        verify(participantRepository).findByUserAndStatus(organizer, InviteStatus.PENDING);
    }

    @Test
    void copyFromDiscovered_withDescriptionAndVenue_buildsDescription() {
        var event = new com.example.meetings.discover.DiscoveredEvent(
                "ticketmaster", "123", "Concert",
                "Great show",        // description preenchida
                start, null,
                "http://test.com",   // url preenchida
                "Altice Arena");     // venue preenchida
        when(meetingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Meeting meeting = meetingService.copyFromDiscovered(organizer, event);

        assertTrue(meeting.getDescription().contains("Great show"));
        assertTrue(meeting.getDescription().contains("Altice Arena"));
        assertTrue(meeting.getDescription().contains("http://test.com"));
    }

    @Test
    void copyFromDiscovered_withEndTime_usesProvidedEnd() {
        var event = new com.example.meetings.discover.DiscoveredEvent(
                "ticketmaster", "123", "Concert",
                null, start, end, null, null);
        when(meetingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Meeting meeting = meetingService.copyFromDiscovered(organizer, event);

        assertEquals(end, meeting.getEndTime());
    }

    @Test
    void copyFromDiscovered_withNullDescriptionAndVenue_buildsMinimalDescription() {
        var event = new com.example.meetings.discover.DiscoveredEvent(
                "ticketmaster", "123", "Concert",
                null,   // description nula
                start, null,
                null,   // url nula
                null);  // venue nula
        when(meetingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Meeting meeting = meetingService.copyFromDiscovered(organizer, event);

        assertTrue(meeting.getDescription().contains("Source: ticketmaster"));
    }

    @Test
    void copyFromDiscovered_blankDescription_isNotIncluded() {
        var event = new com.example.meetings.discover.DiscoveredEvent(
                "ticketmaster", "123", "Concert",
                "   ",   // blank description
                start, null,
                null, null);
        when(meetingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Meeting meeting = meetingService.copyFromDiscovered(organizer, event);

        assertFalse(meeting.getDescription().contains("   "));
        assertTrue(meeting.getDescription().contains("Source: ticketmaster"));
    }

    @Test
    void copyFromDiscovered_blankVenue_isNotIncluded() {
        var event = new com.example.meetings.discover.DiscoveredEvent(
                "ticketmaster", "123", "Concert",
                null,
                start, null,
                null,
                "   ");  // blank venue
        when(meetingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Meeting meeting = meetingService.copyFromDiscovered(organizer, event);

        assertFalse(meeting.getDescription().contains("Venue:"));
        assertTrue(meeting.getDescription().contains("Source: ticketmaster"));
    }

    @Test
    void respond_accepted_setsStatusSuccessfully() {
        MeetingParticipant participant = new MeetingParticipant(
                new Meeting("T", null, start, end, organizer),
                organizer,
                InviteStatus.PENDING);
        when(participantRepository.findByMeetingIdAndUserId(any(), any()))
                .thenReturn(Optional.of(participant));

        meetingService.respond(1L, organizer, InviteStatus.ACCEPTED);

        assertEquals(InviteStatus.ACCEPTED, participant.getStatus());
    }

    @Test
    void respond_declined_setsStatusSuccessfully() {
        MeetingParticipant participant = new MeetingParticipant(
                new Meeting("T", null, start, end, organizer),
                organizer,
                InviteStatus.PENDING);
        when(participantRepository.findByMeetingIdAndUserId(any(), any()))
                .thenReturn(Optional.of(participant));

        meetingService.respond(1L, organizer, InviteStatus.DECLINED);

        assertEquals(InviteStatus.DECLINED, participant.getStatus());
    }

    @Test
    void propose_nullInvitee_isIgnored() {
        when(meetingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // null na lista de invitees deve ser ignorado
        Meeting meeting = meetingService.propose(organizer, "Title", "Desc", start, end,
                java.util.Arrays.asList(null, null));

        assertEquals(1, meeting.getParticipants().size()); // só o organizer
    }

    @Test
    void propose_emptyStringInvitee_isIgnored() {
        when(meetingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Meeting meeting = meetingService.propose(organizer, "Title", "Desc", start, end,
                List.of("", "  "));

        assertEquals(1, meeting.getParticipants().size()); // só o organizer
    }

    @Test
    void isConfirmed_noParticipants_returnsFalse() {
        Meeting meeting = new Meeting("T", null, start, end, organizer);
        assertFalse(meeting.isConfirmed());
    }

    @Test
    void isConfirmed_allAccepted_returnsTrue() {
        Meeting meeting = new Meeting("T", null, start, end, organizer);
        meeting.addParticipant(new MeetingParticipant(meeting, organizer, InviteStatus.ACCEPTED));
        assertTrue(meeting.isConfirmed());
    }

    @Test
    void isConfirmed_onePending_returnsFalse() {
        User bob = new User("bob", "bob@example.com", "hash");
        Meeting meeting = new Meeting("T", null, start, end, organizer);
        meeting.addParticipant(new MeetingParticipant(meeting, organizer, InviteStatus.ACCEPTED));
        meeting.addParticipant(new MeetingParticipant(meeting, bob, InviteStatus.PENDING));
        assertFalse(meeting.isConfirmed());
    }

    @Test
    void isConfirmed_oneDeclined_returnsFalse() {
        User bob = new User("bob", "bob@example.com", "hash");
        Meeting meeting = new Meeting("T", null, start, end, organizer);
        meeting.addParticipant(new MeetingParticipant(meeting, organizer, InviteStatus.ACCEPTED));
        meeting.addParticipant(new MeetingParticipant(meeting, bob, InviteStatus.DECLINED));
        assertFalse(meeting.isConfirmed());
    }
}