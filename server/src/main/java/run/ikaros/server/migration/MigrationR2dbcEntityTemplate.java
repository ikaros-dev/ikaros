package run.ikaros.server.migration;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

public class MigrationR2dbcEntityTemplate extends R2dbcEntityTemplate {
    public MigrationR2dbcEntityTemplate(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }
}
