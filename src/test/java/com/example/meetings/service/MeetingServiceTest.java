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
}