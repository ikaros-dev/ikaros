package run.ikaros.planning;

import java.time.Instant;

public record PlanningStatisticsView(Instant from, Instant to, long tasksCreated, long tasksCompleted,
    long estimatedMinutes, long trackedMinutes, long focusMinutes, long goalCount, long habitCheckIns) {}
