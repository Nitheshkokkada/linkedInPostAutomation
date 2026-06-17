package com.linkedinagent.util;

import com.linkedinagent.entity.User;
import com.linkedinagent.repository.ScheduledPostRepository;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

public final class ScheduleTimeUtil {

    private ScheduleTimeUtil() {
    }

    public static OffsetDateTime resolveNextAvailableSlot(User user, ScheduledPostRepository scheduledPostRepository) {
        ZoneId zoneId = ZoneId.of(user.getTimezone());
        LocalTime preferredTime = user.getPreferredPostTime();
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        ZonedDateTime candidate = now
                .withHour(preferredTime.getHour())
                .withMinute(preferredTime.getMinute())
                .withSecond(0)
                .withNano(0);

        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }

        while (hasSameDayConflict(user.getId(), candidate, scheduledPostRepository)) {
            candidate = candidate.plusDays(1);
        }

        return candidate.toOffsetDateTime();
    }

    static boolean hasSameDayConflict(UUID userId, ZonedDateTime candidate, ScheduledPostRepository repository) {
        ZonedDateTime dayStart = candidate.toLocalDate().atStartOfDay(candidate.getZone());
        ZonedDateTime dayEnd = dayStart.plusDays(1);

        return !repository.findByUserIdAndScheduledForBetween(
                userId,
                dayStart.toOffsetDateTime(),
                dayEnd.toOffsetDateTime()
        ).isEmpty();
    }
}
