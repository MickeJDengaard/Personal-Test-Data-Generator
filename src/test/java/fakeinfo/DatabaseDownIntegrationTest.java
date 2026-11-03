package fakeinfo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.mariadb.MariaDBContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MariaDbTestConfig.class)
@TestPropertySource(properties = "spring.datasource.hikari.connection-timeout=2000")
class DatabaseDownIntegrationTest {

    @Autowired
    private MariaDBContainer mariaDb;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void personEndpointShouldReturn500WhenDatabaseIsDown() throws Exception {
        mariaDb.stop();

        mockMvc.perform(get("/person"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(ApiException.UNKNOWN_ERROR));
    }
}
