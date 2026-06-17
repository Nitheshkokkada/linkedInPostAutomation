package com.linkedinagent.util;

import com.linkedinagent.entity.User;
import com.linkedinagent.repository.ScheduledPostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleTimeUtilTest {

    @Mock
    private ScheduledPostRepository scheduledPostRepository;

    @Test
    void resolveNextAvailableSlotReturnsFutureTime() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .timezone("UTC")
                .preferredPostTime(LocalTime.of(9, 0))
                .build();

        when(scheduledPostRepository.findByUserIdAndScheduledForBetween(
                eq(user.getId()), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(Collections.emptyList());

        OffsetDateTime slot = ScheduleTimeUtil.resolveNextAvailableSlot(user, scheduledPostRepository);

        assertThat(slot).isAfter(OffsetDateTime.now(ZoneId.of("UTC")).minusMinutes(1));
        assertThat(slot.getHour()).isEqualTo(9);
        assertThat(slot.getMinute()).isEqualTo(0);
    }

    @Test
    void resolveNextAvailableSlotSkipsConflictDay() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .timezone("UTC")
                .preferredPostTime(LocalTime.of(9, 0))
                .build();

        when(scheduledPostRepository.findByUserIdAndScheduledForBetween(
                eq(user.getId()), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(Collections.singletonList(
                        com.linkedinagent.entity.ScheduledPost.builder().build()))
                .thenReturn(Collections.emptyList());

        OffsetDateTime slot = ScheduleTimeUtil.resolveNextAvailableSlot(user, scheduledPostRepository);

        assertThat(slot.getHour()).isEqualTo(9);
        assertThat(slot.getMinute()).isEqualTo(0);
    }
}
