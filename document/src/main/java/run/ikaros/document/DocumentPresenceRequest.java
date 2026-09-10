package run.ikaros.document;

import jakarta.validation.constraints.NotBlank;

public record DocumentPresenceRequest(@NotBlank String clientId) {}
