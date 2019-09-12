package org.ofbiz.core.entity.transaction;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class HikariDatasourceFactory {

    /**
     * Create a hikari datasource
     * @param config
     * @return HikariConfig
     */
    public HikariDataSource createHikariDatasource(final HikariConfig config) {
        return new HikariDataSource(config);

    }
}
