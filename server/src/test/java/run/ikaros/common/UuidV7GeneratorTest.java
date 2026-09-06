package run.ikaros.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class UuidV7GeneratorTest {
    @Test
    void generatesVersionSevenAndMonotonicValuesWhenClockDoesNotAdvance() {
        UuidV7Generator generator = new UuidV7Generator(Clock.fixed(Instant.ofEpochMilli(1000), ZoneOffset.UTC));
        var first = generator.next();
        var second = generator.next();
        assertEquals(7, first.version());
        assertEquals(2, first.variant());
        assertTrue(first.compareTo(second) < 0);
    }
}
