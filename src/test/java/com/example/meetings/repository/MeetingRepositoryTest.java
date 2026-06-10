package com.example.meetings.repository;

import com.example.meetings.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class MeetingRepositoryTest {

    @Autowired private MeetingRepository meetingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MeetingParticipantRepository participantRepository;

    private User alice;
    private User bob;
    private Instant start;
    private Instant end;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "alice@example.com", "hash"));
        bob   = userRepository.save(new User("bob", "bob@example.com", "hash"));
        start = Instant.parse("2025-09-01T10:00:00Z");
        end   = Instant.parse("2025-09-01T11:00:00Z");
    }

    @Test
    void findCalendarMeetings_organizerSeesOwnMeeting() {
        Meeting meeting = meetingRepository.save(new Meeting("Sync", null, start, end, alice));
        meeting.addParticipant(new MeetingParticipant(meeting, alice, InviteStatus.ACCEPTED));
        meetingRepository.save(meeting);

        List<Meeting> results = meetingRepository.findCalendarMeetings(alice);

        assertEquals(1, results.size());
        assertEquals("Sync", results.get(0).getTitle());
    }

    @Test
    void findCalendarMeetings_declinedInvite_notVisible() {
        Meeting meeting = meetingRepository.save(new Meeting("Sync", null, start, end, alice));
        meeting.addParticipant(new MeetingParticipant(meeting, alice, InviteStatus.ACCEPTED));
        meeting.addParticipant(new MeetingParticipant(meeting, bob, InviteStatus.DECLINED));
        meetingRepository.save(meeting);

        List<Meeting> results = meetingRepository.findCalendarMeetings(bob);

        assertTrue(results.isEmpty());
    }

    @Test
    void findCalendarMeetings_pendingInvite_isVisible() {
        Meeting meeting = meetingRepository.save(new Meeting("Sync", null, start, end, alice));
        meeting.addParticipant(new MeetingParticipant(meeting, alice, InviteStatus.ACCEPTED));
        meeting.addParticipant(new MeetingParticipant(meeting, bob, InviteStatus.PENDING));
        meetingRepository.save(meeting);

        List<Meeting> results = meetingRepository.findCalendarMeetings(bob);

        assertEquals(1, results.size());
    }

    @Test
    void findOverlapping_overlappingMeeting_returnsIt() {
        Meeting meeting = meetingRepository.save(new Meeting("Sync", null, start, end, alice));
        meeting.addParticipant(new MeetingParticipant(meeting, alice, InviteStatus.ACCEPTED));
        meetingRepository.save(meeting);

        List<Meeting> results = meetingRepository.findOverlapping(alice,
                Instant.parse("2025-09-01T09:00:00Z"),
                Instant.parse("2025-09-01T10:30:00Z"));

        assertEquals(1, results.size());
    }

    @Test
    void findOverlapping_nonOverlappingMeeting_returnsEmpty() {
        Meeting meeting = meetingRepository.save(new Meeting("Sync", null, start, end, alice));
        meeting.addParticipant(new MeetingParticipant(meeting, alice, InviteStatus.ACCEPTED));
        meetingRepository.save(meeting);

        List<Meeting> results = meetingRepository.findOverlapping(alice,
                Instant.parse("2025-09-01T12:00:00Z"),
                Instant.parse("2025-09-01T13:00:00Z"));

        assertTrue(results.isEmpty());
    }
}