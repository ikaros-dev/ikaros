package run.ikaros.api.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringConstTest {

    @Test
    void SPACE_ShouldBeSpaceCharacter() {
        assertEquals(" ", StringConst.SPACE);
    }

    @Test
    void SPACE_ShouldHaveLengthOne() {
        assertEquals(1, StringConst.SPACE.length());
    }

    @Test
    void SPACE_ShouldNotBeEmpty() {
        assertFalse(StringConst.SPACE.isEmpty());
    }

    @Test
    void SPACE_ShouldBeTrimmedToEmpty() {
        assertEquals("", StringConst.SPACE.trim());
    }

    @Test
    void SPACE_ShouldBeWhitespace() {
        assertTrue(Character.isWhitespace(StringConst.SPACE.charAt(0)));
    }
}