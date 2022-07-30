package cn.liguohao.ikaros.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;


class BeanKitTest {

    private TestA getTestA() {
        Random random = new Random();
        return new TestA()
                .setNum(random.nextInt())
                .setTemp(random.nextFloat())
                .setDescription("prefix_" + random.nextDouble());
    }

    @Test
    void copyProperties() {
        TestA testA1 = getTestA();
        TestA testA2 = new TestA();
        Assertions.assertNull(testA2.getDescription());
        BeanKit.copyProperties(testA1, testA2);
        Assertions.assertEquals(testA1.getDescription(), testA2.getDescription());
    }

    @Test
    void copyFieldValue() throws IllegalAccessException {
        TestA testA = getTestA();
        TestB testB = new TestB();
        Assertions.assertNull(testB.getDescription());
        BeanKit.copyFieldValue(testA, testB);
        Assertions.assertEquals(testA.getDescription(), testB.getDescription());
    }

    static class TestA {
        private int num;
        private float temp;
        private String description;

        public int getNum() {
            return num;
        }

        public TestA setNum(int num) {
            this.num = num;
            return this;
        }

        public float getTemp() {
            return temp;
        }

        public TestA setTemp(float temp) {
            this.temp = temp;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public TestA setDescription(String description) {
            this.description = description;
            return this;
        }
    }

    static class TestB {
        private String description;

        public String getDescription() {
            return description;
        }

        public TestB setDescription(String description) {
            this.description = description;
            return this;
        }
    }
}