package org.friesoft.porturl.config;

import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class StorageConfig {

    private final PorturlProperties properties;
    private final DataSourceProperties dataSourceProperties;

    public StorageConfig(PorturlProperties properties, DataSourceProperties dataSourceProperties) {
        this.properties = properties;
        this.dataSourceProperties = dataSourceProperties;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(dataSourceProperties.getDriverClassName());
        
        if (properties.getStorage().getType() == PorturlProperties.StorageType.YAML) {
            // Use in-memory H2 for YAML mode
            dataSource.setUrl("jdbc:h2:mem:porturl;DB_CLOSE_DELAY=-1");
        } else {
            // Use persistent SQLite/H2 file as configured
            dataSource.setUrl(dataSourceProperties.getUrl());
        }
        
        dataSource.setUsername(dataSourceProperties.getUsername());
        dataSource.setPassword(dataSourceProperties.getPassword());
        
        return dataSource;
    }
}
