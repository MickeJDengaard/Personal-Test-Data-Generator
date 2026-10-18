package fakeinfo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Random;

/**
 * Generates random postal codes and town names
 * based on the values in the addresses database.
 */
@Repository
public class TownRepository {

    public record Town(String postalCode, String townName) {}

    private final JdbcTemplate jdbcTemplate;

    /**
     * The total number of towns is cached,
     * so that the calculation is made only once
     */
    private volatile int townCount = 0;

    public TownRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @return a random postal code and town
     */
    public Town getRandomTown(Random random) {
        if (townCount == 0) {
            Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) AS total FROM postal_code", Integer.class);
            townCount = total == null ? 0 : total;
        }
        int randomTown = random.nextInt(townCount);
        return jdbcTemplate.queryForObject(
            "SELECT cPostalCode AS postal_code, cTownName AS town_name FROM postal_code LIMIT ?, 1",
            (rs, rowNum) -> new Town(rs.getString("postal_code"), rs.getString("town_name")),
            randomTown
        );
    }
}
