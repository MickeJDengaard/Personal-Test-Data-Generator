
package fakeinfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class FakeInfoIntegrationTest {

    @Container
    @ServiceConnection
    static MariaDBContainer<?> MYSQL =
            new MariaDBContainer<>("mariadb:11")
                    .withDatabaseName("addresses")
                    .withUsername("foo")
                    .withPassword("secret")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath("db/addresses.sql"),
                            "/docker-entrypoint-initdb.d/01-addresses.sql"
                    );

    @Autowired
    private PersonNames personNames;

    @Autowired
    private TownRepository townRepository;

    private FakeInfo fakeInfo;

    @BeforeEach
    void setUp() {
        fakeInfo = new FakeInfo(
                personNames,
                townRepository,
                new Random(42)
        );
    }

    @Test
    void testPersonNamesAreLoaded() {
        List<PersonNames.Person> persons = personNames.persons();

        assertFalse(persons.isEmpty());

        for (PersonNames.Person person : persons) {
            assertNotNull(person.firstName());
            assertNotNull(person.lastName());

            assertTrue(
                    List.of(
                            FakeInfo.GENDER_FEMININE,
                            FakeInfo.GENDER_MASCULINE
                    ).contains(person.gender())
            );
        }
    }

    @Test
    void testGetRandomTown() {
        TownRepository.Town town =
                townRepository.getRandomTown(new Random(42));

        assertNotNull(town);
        assertNotNull(town.postalCode());
        assertNotNull(town.townName());
    }

    @Test
    void testGenerateFakePerson() {
        Map<String, Object> person = fakeInfo.getFakePerson();

        assertEquals(7, person.size());

        assertNotNull(person.get("CPR"));
        assertNotNull(person.get("firstName"));
        assertNotNull(person.get("lastName"));
        assertNotNull(person.get("gender"));
        assertNotNull(person.get("birthDate"));
        assertNotNull(person.get("address"));
        assertNotNull(person.get("phoneNumber"));
    }

    @Test
    void testCprMatchesBirthDate() {
        Map<String, Object> person = fakeInfo.getFakePerson();

        String cpr = (String) person.get("CPR");

        LocalDate birthDate = LocalDate.parse(
                (String) person.get("birthDate")
        );

        String expectedDate = birthDate.format(
                DateTimeFormatter.ofPattern("ddMMyy")
        );

        assertEquals(10, cpr.length());
        assertTrue(cpr.matches("\\d{10}"));
        assertEquals(expectedDate, cpr.substring(0, 6));
    }

    @Test
    void testCprMatchesGender() {
        Map<String, Object> person = fakeInfo.getFakePerson();

        String cpr = (String) person.get("CPR");
        String gender = (String) person.get("gender");

        int lastDigit = Character.getNumericValue(
                cpr.charAt(9)
        );

        if (FakeInfo.GENDER_FEMININE.equals(gender)) {
            assertEquals(0, lastDigit % 2);
        } else {
            assertEquals(1, lastDigit % 2);
        }
    }

    @Test
    void testGeneratePhoneNumber() {
        String phone = fakeInfo.getPhoneNumber();

        assertNotNull(phone);
        assertEquals(8, phone.length());
        assertTrue(phone.matches("\\d{8}"));

        assertTrue(
                List.of(FakeInfo.PHONE_PREFIXES)
                        .stream()
                        .anyMatch(phone::startsWith)
        );
    }

    @Test
    void testGenerateAddress() {
        Map<String, Object> result = fakeInfo.getAddress();

        @SuppressWarnings("unchecked")
        Map<String, Object> address =
                (Map<String, Object>) result.get("address");

        assertNotNull(address);
        assertNotNull(address.get("street"));
        assertNotNull(address.get("number"));
        assertNotNull(address.get("floor"));
        assertNotNull(address.get("door"));
        assertNotNull(address.get("postal_code"));
        assertNotNull(address.get("town_name"));
    }

    @Test
    void testGenerateMultiplePersons() {
        List<Map<String, Object>> persons =
                fakeInfo.getFakePersons(5);

        assertEquals(5, persons.size());

        for (Map<String, Object> person : persons) {
            assertNotNull(person.get("CPR"));
            assertNotNull(person.get("firstName"));
            assertNotNull(person.get("lastName"));
            assertNotNull(person.get("gender"));
            assertNotNull(person.get("birthDate"));
            assertNotNull(person.get("address"));
            assertNotNull(person.get("phoneNumber"));
        }
    }

    @Test
    void testMinimumNumberOfPersons() {
        List<Map<String, Object>> persons =
                fakeInfo.getFakePersons(-2);

        assertEquals(
                FakeInfo.MIN_BULK_PERSONS,
                persons.size()
        );
    }

    @Test
    void testMaximumNumberOfPersons() {
        List<Map<String, Object>> persons =
                fakeInfo.getFakePersons(150);

        assertEquals(
                FakeInfo.MAX_BULK_PERSONS,
                persons.size()
        );
    }

    @Test
    void testDefaultNumberOfPersons() {
        List<Map<String, Object>> persons =
                fakeInfo.getFakePersons();

        assertEquals(
                FakeInfo.MIN_BULK_PERSONS,
                persons.size()
        );
    }
}
