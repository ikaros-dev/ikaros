package run.ikaros.server.common;

import java.util.UUID;

/**
 * Reusable UUID constants for tests.
 */
public abstract class TestEntityIds {

    public static final UUID USER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID USER_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    public static final UUID ROLE_10 = UUID.fromString("00000000-0000-0000-0000-000000000010");
    public static final UUID ROLE_99 = UUID.fromString("00000000-0000-0000-0000-000000000099");

    public static final UUID AUTH_100 = UUID.fromString("00000000-0000-0000-0000-000000000100");
    public static final UUID AUTH_101 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    public static final UUID AUTH_102 = UUID.fromString("00000000-0000-0000-0000-000000000102");
    public static final UUID AUTH_200 = UUID.fromString("00000000-0000-0000-0000-000000000200");

    public static final UUID ROLE_AUTH_1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    public static final UUID ROLE_AUTH_2 = UUID.fromString("00000000-0000-0000-0000-000000000102");

    public static final UUID ENTITY_42 = UUID.fromString("00000000-0000-0000-0000-000000000042");
}
