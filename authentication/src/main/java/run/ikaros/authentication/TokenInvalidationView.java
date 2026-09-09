package run.ikaros.authentication;

import java.util.UUID;

/** 用户级 Token 安全纪元提升结果，不包含任何 Token 原文。 */
public record TokenInvalidationView(UUID userId, long securityVersion) { }
