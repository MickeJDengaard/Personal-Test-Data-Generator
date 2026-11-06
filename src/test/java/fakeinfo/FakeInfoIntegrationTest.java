package fakeinfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MariaDbTestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // needed so @BeforeAll can be non-static
class FakeInfoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PersonNames personNames;

    @Autowired
    private TownRepository townRepository;

    private int townCount;
    private List<TownRepository.Town> allTowns;

    // get all towns once
    @BeforeAll
    void loadTowns() {
        townCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM postal_code", Integer.class);
        allTowns = new ArrayList<>();

        for (int i = 0; i < townCount; i++) {

            final int row = i;
            Random fixedRandom = new Random() {
                @Override
                public int nextInt(int bound) {
                    return row;
                }
            };
            allTowns.add(townRepository.getRandomTown(fixedRandom));
        }
    }

    // town tests
    @Test
    void randomTownIsSameAsInDatabase() {
        TownRepository.Town town = townRepository.getRandomTown(new Random());

        String townNameInDatabase = jdbcTemplate.queryForObject(
                "SELECT cTownName FROM postal_code WHERE cPostalCode = ?",
                String.class,
                town.postalCode()
        );

        assertEquals(townNameInDatabase, town.townName());
    }

    @Test
    void allTownsCanBeFound() {
        assertEquals(townCount, allTowns.stream().distinct().count());
    }

    @Test
    void townNamesKeepDanishLetters() {
        assertTrue(allTowns.contains(new TownRepository.Town("2100", "København Ø")));
    }

    // names file test
    @Test
    void namesFileHasValidPersons() {
        List<PersonNames.Person> persons = personNames.persons();
        assertFalse(persons.isEmpty());

        for (PersonNames.Person person : persons) {
            assertFalse(person.firstName().isBlank());
            assertFalse(person.lastName().isBlank());
            assertTrue(List.of(FakeInfo.GENDER_FEMININE, FakeInfo.GENDER_MASCULINE)
                    .contains(person.gender()));
        }
    }

    // controller tests
    @Test
    void personHasAllFields() throws Exception {
        String body = mockMvc.perform(get("/person"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode person = objectMapper.readTree(body);

        List<String> fields = new ArrayList<>();
        person.fieldNames().forEachRemaining(fields::add);

        assertEquals(
                List.of("CPR", "firstName", "lastName", "gender", "birthDate", "address", "phoneNumber"),
                fields
        );
    }

    @Test
    void personUsesDataFromDatabaseAndNamesFile() throws Exception {
        String body = mockMvc.perform(get("/person"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode person = objectMapper.readTree(body);

        JsonNode address = person.get("address");
        String townNameInDatabase = jdbcTemplate.queryForObject(
                "SELECT cTownName FROM postal_code WHERE cPostalCode = ?",
                String.class,
                address.get("postal_code").asText()
        );
        assertEquals(townNameInDatabase, address.get("town_name").asText());
        PersonNames.Person personFromResponse = new PersonNames.Person(
                person.get("firstName").asText(),
                person.get("lastName").asText(),
                person.get("gender").asText()
        );
        assertTrue(personNames.persons().contains(personFromResponse));
    }

    @Test
    void canGetThreePersons() throws Exception {
        String body = mockMvc.perform(get("/person?n=3"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode persons = objectMapper.readTree(body);

        assertTrue(persons.isArray());
        assertEquals(3, persons.size());

        for (JsonNode person : persons) {
            JsonNode address = person.get("address");
            String townNameInDatabase = jdbcTemplate.queryForObject(
                    "SELECT cTownName FROM postal_code WHERE cPostalCode = ?",
                    String.class,
                    address.get("postal_code").asText()
            );
            assertEquals(townNameInDatabase, address.get("town_name").asText());

            PersonNames.Person personFromResponse = new PersonNames.Person(
                    person.get("firstName").asText(),
                    person.get("lastName").asText(),
                    person.get("gender").asText()
            );
            assertTrue(personNames.persons().contains(personFromResponse));
        }
    }
}
