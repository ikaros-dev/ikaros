package run.ikaros.server.migration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;

public class MigrationConnectionFactoryBuilder {

    /**
     * Build a ConnectionFactory from migration datasource properties.
     */
    public static ConnectionFactory build(MigrationProperties properties) {
        MigrationProperties.Datasource ds = properties.getDatasource();
        ConnectionFactoryOptions.Builder builder = ConnectionFactoryOptions.parse(ds.getUrl())
            .mutate();
        if (ds.getUsername() != null) {
            builder.option(ConnectionFactoryOptions.USER, ds.getUsername());
        }
        if (ds.getPassword() != null) {
            builder.option(ConnectionFactoryOptions.PASSWORD, ds.getPassword());
        }
        return ConnectionFactories.get(builder.build());
    }
}
