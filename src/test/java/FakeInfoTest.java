import fakeinfo.FakeInfo;
import fakeinfo.PersonNames;
import fakeinfo.TownRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FakeInfoTest {
    private PersonNames personNames;
    private TownRepository townRepository;
    private Random random;

    private FakeInfo fakeInfo;

    private static final String FIRST_NAME = "Anders";
    private static final String LAST_NAME = "Jensen";
    private static final String GENDER = FakeInfo.GENDER_MASCULINE;

    private static final String POSTAL_CODE = "2100";
    private static final String TOWN_NAME = "Copenhagen";

    @BeforeEach
    void setUp() {
        personNames = mock(PersonNames.class);
        townRepository = mock(TownRepository.class);
        random = new Random(42);

        PersonNames.Person person = mock(PersonNames.Person.class);

        when(person.firstName()).thenReturn(FIRST_NAME);
        when(person.lastName()).thenReturn(LAST_NAME);
        when(person.gender()).thenReturn(GENDER);

        when(personNames.getRandomPerson(any(Random.class)))
                .thenReturn(person);

        TownRepository.Town town = mock(TownRepository.Town.class);

        when(town.postalCode()).thenReturn(String.valueOf(POSTAL_CODE));
        when(town.townName()).thenReturn(TOWN_NAME);

        when(townRepository.getRandomTown(any(Random.class)))
                .thenReturn(town);

        fakeInfo = new FakeInfo(personNames, townRepository, random);
    }

    // -------------------------------------------------
    // CPR tests
    // -------------------------------------------------

    @Test
    void cprShouldContainTenDigits() {
        assertTrue(fakeInfo.getCpr().matches("\\d{10}"));
    }

    @Test
    void cprShouldContainCorrectBirthDate() {
        String expectedDate = LocalDate.parse(
                fakeInfo.getFullNameGenderAndBirthDate()
                        .get("birthDate").toString()
        ).format(DateTimeFormatter.ofPattern("ddMMyy"));

        assertEquals(expectedDate, fakeInfo.getCpr().substring(0, 6));
    }

    @ParameterizedTest
    @CsvSource({
            "male, 1",
            "female, 0"
    })
    void cprShouldHaveCorrectGenderDigit(String gender, int expectedParity) {

        PersonNames.Person person = mock(PersonNames.Person.class);

        when(person.firstName()).thenReturn(FIRST_NAME);
        when(person.lastName()).thenReturn(LAST_NAME);
        when(person.gender()).thenReturn(gender);

        when(personNames.getRandomPerson(any(Random.class)))
                .thenReturn(person);

        FakeInfo info = new FakeInfo(
                personNames,
                townRepository,
                new Random(42)
        );

        int finalDigit = Character.getNumericValue(
                info.getCpr().charAt(9)
        );

        assertEquals(expectedParity, finalDigit % 2);
    }

    // -------------------------------------------------
    // Birth date tests
    // -------------------------------------------------

    @Test
    void birthDateShouldBeWithinValidRange() {
        LocalDate birthDate = LocalDate.parse(
                fakeInfo.getFullNameGenderAndBirthDate()
                        .get("birthDate").toString()
        );

        assertFalse(birthDate.isBefore(LocalDate.of(1900, 1, 1)));
        assertFalse(birthDate.isAfter(LocalDate.now()));
    }

    //Maybe change above to assertTrue

    @Test
    void birthDateShouldUseIsoFormat() {
        String birthDate = fakeInfo
                .getFullNameGenderAndBirthDate()
                .get("birthDate").toString();

        assertTrue(birthDate.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    // -------------------------------------------------
    // Name and gender tests
    // -------------------------------------------------

    @Test
    void getFullNameAndGenderShouldReturnCorrectValues() {
        Map<String, Object> result = fakeInfo.getFullNameAndGender();

        assertEquals(FIRST_NAME, result.get("firstName"));
        assertEquals(LAST_NAME, result.get("lastName"));
        assertEquals(GENDER, result.get("gender"));
    }

    @Test
    void getFullNameGenderAndBirthDateShouldContainAllFields() {
        Map<String, Object> result =
                fakeInfo.getFullNameGenderAndBirthDate();

        assertEquals(4, result.size());
        assertTrue(result.containsKey("birthDate"));
    }

    // -------------------------------------------------
    // Address tests
    // -------------------------------------------------

    @Test
    void addressShouldContainCorrectPostalInformation() {
        Map<String, Object> address = getAddress();

        assertEquals(POSTAL_CODE, address.get("postal_code"));
        assertEquals(TOWN_NAME, address.get("town_name"));
    }

    @Test
    void streetShouldHaveCorrectLength() {
        String street = getAddress().get("street").toString();

        assertEquals(FakeInfo.STREET_LENGTH, street.length());
    }

    @Test
    void streetShouldOnlyContainLettersAndSpaces() {
        String street = getAddress().get("street").toString();

        assertTrue(street.matches("[a-zA-ZæøåÆØÅ ]+"));
    }

    @Test
    void houseNumberShouldHaveValidFormat() {
        String number = getAddress().get("number").toString();

        assertTrue(number.matches("\\d{1,3}[A-Z]?"));

        int numericPart = Integer.parseInt(
                number.replaceAll("[A-Z]", "")
        );

        assertTrue(numericPart >= 1 && numericPart <= 999);
    }

    @Test
    void floorShouldBeValid() {
        Object floor = getAddress().get("floor");

        if (floor instanceof String) {
            assertEquals("st", floor);
        } else {
            int floorNumber = (int) floor;
            assertTrue(floorNumber >= 1 && floorNumber <= 99);
        }
    }

    @Test
    void doorShouldHaveValidFormat() {
        Object door = getAddress().get("door");

        assertTrue(
                door.equals("th")
                        || door.equals("tv")
                        || door.equals("mf")
                        || door.toString().matches("[a-zøæå]-?\\d{1,3}")
        );
    }

    // -------------------------------------------------
    // Phone tests
    // -------------------------------------------------

    @Test
    void phoneNumberShouldContainEightDigits() {
        assertTrue(fakeInfo.getPhoneNumber().matches("\\d{8}"));
    }

    @Test
    void phoneNumberShouldStartWithValidPrefix() {
        String phone = fakeInfo.getPhoneNumber();

        assertTrue(
                Stream.of(FakeInfo.PHONE_PREFIXES)
                        .anyMatch(phone::startsWith)
        );
    }
    
    //Test for fake prefixes

    // -------------------------------------------------
    // Other tests
    // -------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "-100, 2",
            "-2, 2",
            "-1, 2",
            "0, 2",
            "1, 2",
            "2, 2",
            "3, 3",
            "50, 50",
            "99, 99",
            "100, 100",
            "101, 100",
            "102, 100",
            "1000, 100"
    })
    void getFakePersonsShouldRespectAmountBoundaries(int input, int expected) {
        List<Map<String, Object>> result =
                fakeInfo.getFakePersons(input);

        assertEquals(expected, result.size());
    }

    // -------------------------------------------------
    // Helpers
    // -------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> getAddress() {
        return (Map<String, Object>) fakeInfo
                .getAddress()
                .get("address");
    }

}
