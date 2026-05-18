package run.ikaros.server.migration;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

public class MigrationR2dbcEntityTemplate {

    private final R2dbcEntityTemplate delegate;

    public MigrationR2dbcEntityTemplate(ConnectionFactory connectionFactory) {
        this.delegate = new R2dbcEntityTemplate(connectionFactory);
    }

    public R2dbcEntityTemplate getDelegate() {
        return delegate;
    }

    public org.springframework.r2dbc.core.DatabaseClient getDatabaseClient() {
        return delegate.getDatabaseClient();
    }
}
