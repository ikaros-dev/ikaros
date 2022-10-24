package run.ikaros.server.model.binary;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import run.ikaros.server.model.binary.Binary;
import run.ikaros.server.model.binary.BinaryPlace;
import run.ikaros.server.model.binary.BinaryStorge;
import run.ikaros.server.model.binary.LocalBinaryStorge;

/**
 * @author guohao
 * @date 2022/10/21
 */
class LocalBinaryStorgeTest {

    BinaryStorge binaryStorge = new LocalBinaryStorge();

    @Test
    void operate() {
        byte[] bytes = new String("Hello Ikaros").getBytes(StandardCharsets.UTF_8);
        Binary binary = new Binary()
            .setBytes(bytes).setName("test.txt");
        binary = binaryStorge.add(binary);
        Assertions.assertNotNull(binary);
        Assertions.assertNotNull(binary.getUrl());
        Assertions.assertNotNull(binary.getUploadedTime());

        Assertions.assertTrue(binaryStorge.exists(binary));
        binaryStorge.delete(binary);
        Assertions.assertFalse(binaryStorge.exists(binary));
    }

    @Test
    void getPlace() {
        BinaryPlace place = BinaryPlace.LOCAL;
        Binary binary = new Binary().setPlace(place);
        Assertions.assertEquals(place, binaryStorge.getPlace(binary));
    }
}