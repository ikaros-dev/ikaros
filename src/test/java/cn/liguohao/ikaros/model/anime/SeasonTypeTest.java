package cn.liguohao.ikaros.model.anime;

import cn.liguohao.ikaros.model.entity.anime.SeasonEntity;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author guohao
 * @date 2022/10/23
 */
public class SeasonTypeTest {

    @Test
    void comparator() {
        SeasonEntity.Type[] values = SeasonEntity.Type.values();
        List<SeasonEntity.Type> types = Arrays.asList(values);
        Collections.sort(types, new SeasonEntity.Type.OrderComparator());

        int firstIndex = 0;
        int secondIndex = 0;
        int sixthIndex = 0;
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i) == SeasonEntity.Type.FIRST) {
                firstIndex = i;
            }
            if (types.get(i) == SeasonEntity.Type.SECOND) {
                secondIndex = i;
            }

            if (types.get(i) == SeasonEntity.Type.SIXTH) {
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
