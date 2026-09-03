package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;

public record PlanningCalendarItemView(PlanningCalendarItemType type, UUID id, UUID sourceId,
    String title, Instant startAt, Instant endAt, String timeZone) {}
