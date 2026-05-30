package run.ikaros.api.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StringConstTest {

    @Test
    void space_ShouldBeSpaceCharacter() {
        assertEquals(" ", StringConst.SPACE);
    }

    @Test
    void space_ShouldHaveLengthOne() {
        assertEquals(1, StringConst.SPACE.length());
    }

    @Test
    void space_ShouldNotBeEmpty() {
        assertFalse(StringConst.SPACE.isEmpty());
    }

    @Test
    void space_ShouldBeTrimmedToEmpty() {
        assertEquals("", StringConst.SPACE.trim());
    }

    @Test
    void space_ShouldBeWhitespace() {
        assertTrue(Character.isWhitespace(StringConst.SPACE.charAt(0)));
    }
}