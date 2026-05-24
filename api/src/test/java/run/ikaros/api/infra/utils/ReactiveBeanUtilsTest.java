package run.ikaros.api.infra.utils;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class ReactiveBeanUtilsTest {

    @Test
    void copyProperties_WithValidSourceAndTarget_ShouldCopyProperties() {
        TestSource source = new TestSource("test", 123);
        TestTarget target = new TestTarget();
        
        Mono<TestTarget> result = ReactiveBeanUtils.copyProperties(source, target);
        
        StepVerifier.create(result)
            .assertNext(t -> {
                assertEquals("test", t.getName());
                assertEquals(123, t.getValue());
            })
            .verifyComplete();
    }

    @Test
    void copyProperties_WithNullSource_ShouldReturnEmptyMono() {
        TestTarget target = new TestTarget();
        
        Mono<TestTarget> result = ReactiveBeanUtils.copyProperties(null, target);
        
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void copyProperties_WithIgnoreProperties_ShouldNotCopyIgnoredProperties() {
        TestSource source = new TestSource("test", 123);
        TestTarget target = new TestTarget();
        
        Mono<TestTarget> result = ReactiveBeanUtils.copyProperties(source, target, "name");
        
        StepVerifier.create(result)
            .assertNext(t -> {
                assertNull(t.getName());
                assertEquals(123, t.getValue());
            })
            .verifyComplete();
    }

    @Test
    void copyProperties_WithMultipleIgnoreProperties_ShouldNotCopyIgnoredProperties() {
        TestSource source = new TestSource("test", 123);
        TestTarget target = new TestTarget();
        
        Mono<TestTarget> result = ReactiveBeanUtils.copyProperties(source, target, "name", "value");
        
        StepVerifier.create(result)
            .assertNext(t -> {
                assertNull(t.getName());
                assertEquals(0, t.getValue());
            })
            .verifyComplete();
    }

    @Test
    void copyProperties_WithNullSourceAndIgnoreProperties_ShouldReturnEmptyMono() {
        TestTarget target = new TestTarget();
        
        Mono<TestTarget> result = ReactiveBeanUtils.copyProperties(null, target, "name");
        
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void copyProperties_WithEmptyIgnoreProperties_ShouldCopyAllProperties() {
        TestSource source = new TestSource("test", 123);
        TestTarget target = new TestTarget();
        
        Mono<TestTarget> result = ReactiveBeanUtils.copyProperties(source, target, new String[0]);
        
        StepVerifier.create(result)
            .assertNext(t -> {
                assertEquals("test", t.getName());
                assertEquals(123, t.getValue());
            })
            .verifyComplete();
    }

    // Test helper classes
    public static class TestSource {
        private String name;
        private int value;

        public TestSource(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class TestTarget {
        private String name;
        private int value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}