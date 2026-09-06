package run.ikaros.common;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;
import run.ikaros.common.api.UuidV7Generator;

/** Foundation 提供的线程安全 UUIDv7 生成实现。 */
@Component
public final class DefaultUuidV7Generator implements UuidV7Generator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Clock clock;
    private final AtomicLong lastMillis = new AtomicLong(Long.MIN_VALUE);

    public DefaultUuidV7Generator() {
        this(Clock.systemUTC());
    }

    public DefaultUuidV7Generator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public UUID next() {
        long millis = clock.millis();
        long previous;
        do {
            previous = lastMillis.get();
            if (millis <= previous) {
                millis = previous + 1;
            }
        } while (!lastMillis.compareAndSet(previous, millis));

        long most = (millis & 0xFFFFFFFFFFFFL) << 16;
        most |= 0x7000L | (RANDOM.nextLong() & 0x0FFFL);
        long least = RANDOM.nextLong();
        least = (least & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(most, least);
    }
}
