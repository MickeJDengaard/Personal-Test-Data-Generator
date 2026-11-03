package fakeinfo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
class MariaDbTestConfig {

    @Bean
    @ServiceConnection
    MariaDBContainer mariaDbContainer() {
        return new MariaDBContainer("mariadb:11")
            .withDatabaseName("addresses")
            .withCopyFileToContainer(
                MountableFile.forHostPath("db/addresses.sql"),
                "/docker-entrypoint-initdb.d/01-addresses.sql"
            );
    }
}
