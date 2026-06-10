package com.example.meetings.repository;

import com.example.meetings.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class MeetingParticipantRepositoryTest {

    @Autowired private MeetingParticipantRepository participantRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private UserRepository userRepository;

    private User alice;
    private User bob;
    private Meeting meeting;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "alice@example.com", "hash"));
        bob   = userRepository.save(new User("bob", "bob@example.com", "hash"));

        meeting = new Meeting("Sync", null,
                Instant.parse("2025-09-01T10:00:00Z"),
                Instant.parse("2025-09-01T11:00:00Z"),
                alice);
        meeting.addParticipant(new MeetingParticipant(meeting, alice, InviteStatus.ACCEPTED));
        meeting.addParticipant(new MeetingParticipant(meeting, bob, InviteStatus.PENDING));
        meetingRepository.save(meeting);
    }

    @Test
    void findByUserAndStatus_pending_returnsBob() {
        List<MeetingParticipant> results = participantRepository
                .findByUserAndStatus(bob, InviteStatus.PENDING);

        assertEquals(1, results.size());
        assertEquals("bob", results.get(0).getUser().getUsername());
    }

    @Test
    void findByUserAndStatus_accepted_returnsAlice() {
        List<MeetingParticipant> results = participantRepository
                .findByUserAndStatus(alice, InviteStatus.ACCEPTED);

        assertEquals(1, results.size());
        assertEquals("alice", results.get(0).getUser().getUsername());
    }

    @Test
    void findByUserAndStatus_noMatch_returnsEmpty() {
        List<MeetingParticipant> results = participantRepository
                .findByUserAndStatus(alice, InviteStatus.DECLINED);

        assertTrue(results.isEmpty());
    }

    @Test
    void findByMeetingIdAndUserId_existingParticipant_returnsIt() {
        Optional<MeetingParticipant> result = participantRepository
                .findByMeetingIdAndUserId(meeting.getId(), bob.getId());

        assertTrue(result.isPresent());
        assertEquals(InviteStatus.PENDING, result.get().getStatus());
    }

    @Test
    void findByMeetingIdAndUserId_unknownUser_returnsEmpty() {
        Optional<MeetingParticipant> result = participantRepository
                .findByMeetingIdAndUserId(meeting.getId(), 999L);

        assertTrue(result.isEmpty());
    }
}