package fakeinfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MariaDbTestConfig.class)
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

    private FakeInfo fakeInfo;

    @BeforeEach
    void setUp() {
        fakeInfo = new FakeInfo(personNames, townRepository, new Random());
    }

    // -------------------------------------------------
    // TownRepository + database tests
    // -------------------------------------------------

    @Test
    void randomTownShouldMatchRowInDatabase() {
        TownRepository.Town town = townRepository.getRandomTown(new Random());

        assertEquals(getTownNameFromDatabase(town.postalCode()), town.townName());
    }

    @Test
    void everyTownShouldBeReachable() {
        int count = getTownCount();
        TownRepository repository = new TownRepository(jdbcTemplate);

        List<TownRepository.Town> towns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            towns.add(repository.getRandomTown(new FixedRandom(i)));
        }

        assertEquals(count, towns.stream().distinct().count());
    }

    @Test
    void townNamesShouldKeepDanishCharacters() {
        int count = getTownCount();
        TownRepository repository = new TownRepository(jdbcTemplate);

        List<TownRepository.Town> towns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            towns.add(repository.getRandomTown(new FixedRandom(i)));
        }

        assertTrue(towns.contains(new TownRepository.Town("2100", "København Ø")));
    }

    // -------------------------------------------------
    // PersonNames + names file tests
    // -------------------------------------------------

    @Test
    void namesFileShouldLoadPersons() {
        List<PersonNames.Person> persons = personNames.persons();

        assertFalse(persons.isEmpty());
    }

    @Test
    void namesFileShouldOnlyContainValidPersons() {
        List<PersonNames.Person> persons = personNames.persons();

        for (PersonNames.Person person : persons) {
            assertFalse(person.firstName().isBlank());
            assertFalse(person.lastName().isBlank());
            assertTrue(List.of(FakeInfo.GENDER_FEMININE, FakeInfo.GENDER_MASCULINE)
                    .contains(person.gender()));
        }
    }

    // -------------------------------------------------
    // FakeInfo + database + names file tests
    // -------------------------------------------------

    @Test
    void fakePersonShouldHaveNameFromNamesFile() {
        Map<String, Object> person = fakeInfo.getFakePerson();

        PersonNames.Person expected = new PersonNames.Person(
                (String) person.get("firstName"),
                (String) person.get("lastName"),
                (String) person.get("gender")
        );

        assertTrue(personNames.persons().contains(expected));
    }

    @Test
    void cprShouldMatchGenderFromNamesFile() {
        List<Map<String, Object>> persons = fakeInfo.getFakePersons(FakeInfo.MAX_BULK_PERSONS);

        for (Map<String, Object> person : persons) {
            String cpr = (String) person.get("CPR");
            int lastDigit = Character.getNumericValue(cpr.charAt(9));
            int expectedParity = FakeInfo.GENDER_MASCULINE.equals(person.get("gender")) ? 1 : 0;

            assertEquals(expectedParity, lastDigit % 2);
        }
    }

    @Test
    void fakePersonsShouldHaveTownsFromDatabase() {
        List<Map<String, Object>> persons = fakeInfo.getFakePersons(FakeInfo.MAX_BULK_PERSONS);

        assertEquals(FakeInfo.MAX_BULK_PERSONS, persons.size());
        for (Map<String, Object> person : persons) {
            Map<String, Object> address = getAddress(person);

            assertEquals(
                    getTownNameFromDatabase((String) address.get("postal_code")),
                    address.get("town_name")
            );
        }
    }

    // -------------------------------------------------
    // Controller tests
    // -------------------------------------------------

    @Test
    void personEndpointShouldReturnPersonWithAllFields() throws Exception {
        JsonNode person = getJson("/person");

        List<String> fields = new ArrayList<>();
        person.fieldNames().forEachRemaining(fields::add);

        assertEquals(
                List.of("CPR", "firstName", "lastName", "gender", "birthDate", "address", "phoneNumber"),
                fields
        );
    }

    @Test
    void personEndpointShouldReturnDataFromDatabaseAndNamesFile() throws Exception {
        JsonNode person = getJson("/person");

        assertPersonUsesRealData(person);
    }

    @Test
    void personEndpointShouldReturnThreePersons() throws Exception {
        JsonNode persons = getJson("/person?n=3");

        assertTrue(persons.isArray());
        assertEquals(3, persons.size());
        for (JsonNode person : persons) {
            assertPersonUsesRealData(person);
        }
    }

    // -------------------------------------------------
    // Helpers
    // -------------------------------------------------

    private JsonNode getJson(String url) throws Exception {
        String body = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        return objectMapper.readTree(body);
    }

    private void assertPersonUsesRealData(JsonNode person) {
        JsonNode address = person.get("address");

        assertEquals(
                getTownNameFromDatabase(address.get("postal_code").asText()),
                address.get("town_name").asText()
        );

        PersonNames.Person expected = new PersonNames.Person(
                person.get("firstName").asText(),
                person.get("lastName").asText(),
                person.get("gender").asText()
        );

        assertTrue(personNames.persons().contains(expected));
    }

    private String getTownNameFromDatabase(String postalCode) {
        return jdbcTemplate.queryForObject(
                "SELECT cTownName FROM postal_code WHERE cPostalCode = ?",
                String.class,
                postalCode
        );
    }

    private int getTownCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM postal_code", Integer.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getAddress(Map<String, Object> person) {
        return (Map<String, Object>) person.get("address");
    }

    private static class FixedRandom extends Random {
        private final int value;

        FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return value;
        }
    }
}
