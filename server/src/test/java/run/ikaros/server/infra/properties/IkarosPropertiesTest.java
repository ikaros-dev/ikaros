package run.ikaros.server.infra.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import run.ikaros.api.infra.properties.IkarosProperties;

/**
 * IkarosProperties unit test.
 *
 * @author: li-guohao
 * @see IkarosProperties
 */
class IkarosPropertiesTest {

    IkarosProperties ikarosProperties;

    @BeforeEach
    void setUp() {
        ikarosProperties = new IkarosProperties();
    }

    @Test
    void externalUrl() {
        URI uri = URI.create("http://localhost:50000");
        ikarosProperties.setExternalUrl(uri);
        assertThat(ikarosProperties.getExternalUrl()).isEqualTo(uri);
    }

    @Test
    void testEquals() {
        ikarosProperties.setExternalUrl(URI.create("http://localhost:50000"));
        IkarosProperties other = new IkarosProperties();
        assertThat(ikarosProperties).isNotEqualTo(other);
    }

    @Test
    void canEqual() {
        IkarosProperties other = new IkarosProperties();
        assertThat(ikarosProperties.canEqual(other)).isTrue();
        assertThat(ikarosProperties.canEqual(new Object())).isFalse();
    }

}