package run.ikaros.planning;

import jakarta.validation.constraints.Min;

public record CompletePlanningFocusSessionRequest(@Min(1) Integer actualMinutes, String note) {}
