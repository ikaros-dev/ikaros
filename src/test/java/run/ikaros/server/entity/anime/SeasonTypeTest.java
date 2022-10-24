package run.ikaros.server.entity.anime;

import run.ikaros.server.entity.SeasonEntity;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import run.ikaros.server.enums.SeasonType;

/**
 * @author guohao
 * @date 2022/10/23
 */
public class SeasonTypeTest {

    @Test
    void comparator() {
        SeasonType[] values = SeasonType.values();
        List<SeasonType> types = Arrays.asList(values);
        Collections.sort(types, new SeasonType.OrderComparator());

        int firstIndex = 0;
        int secondIndex = 0;
        int sixthIndex = 0;
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i) == SeasonType.FIRST) {
                firstIndex = i;
            }
            if (types.get(i) == SeasonType.SECOND) {
                secondIndex = i;
            }

            if (types.get(i) == SeasonType.SIXTH) {
                sixthIndex = i;
            }
        }
        Assertions.assertNotEquals(0, firstIndex);
        Assertions.assertNotEquals(0, secondIndex);
        Assertions.assertNotEquals(0, sixthIndex);
        Assertions.assertTrue(firstIndex < secondIndex);
        Assertions.assertTrue(secondIndex < sixthIndex);

    }

}
