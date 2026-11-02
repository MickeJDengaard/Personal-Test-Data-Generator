package fakeinfo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates information about fake persons.
 */
public class FakeInfo {

    public static final String GENDER_FEMININE = "female";
    public static final String GENDER_MASCULINE = "male";
    public static final String[] PHONE_PREFIXES = {
        "2", "30", "31", "40", "41", "42", "50", "51", "52", "53", "60", "61", "71", "81", "91", "92", "93", "342",
        "344", "345", "346", "347", "348", "349", "356", "357", "359", "362", "365", "366", "389", "398", "431",
        "441", "462", "466", "468", "472", "474", "476", "478", "485", "486", "488", "489", "493", "494", "495",
        "496", "498", "499", "542", "543", "545", "551", "552", "556", "571", "572", "573", "574", "577", "579",
        "584", "586", "587", "589", "597", "598", "627", "629", "641", "649", "658", "662", "663", "664", "665",
        "667", "692", "693", "694", "697", "771", "772", "782", "783", "785", "786", "788", "789", "826", "827", "829"
    };
    public static final int MIN_BULK_PERSONS = 2;
    public static final int MAX_BULK_PERSONS = 100;
    public static final int STREET_LENGTH = 40;

    private static final LocalDate MIN_BIRTH_DATE = LocalDate.of(1900, 1, 1);
    private static final DateTimeFormatter CPR_DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyy");

    private static final char[] LETTERS =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final char[] DANISH_LETTERS = "æøåÆØÅ".toCharArray();
    private static final char[] LOWER_CASE_LETTERS = "abcdefghijklmnopqrstuvwxyzøæå".toCharArray();

    private final PersonNames personNames;
    private final TownRepository townRepository;
    private final Random random;

    private String cpr;
    private String firstName;
    private String lastName;
    private String gender;
    private LocalDate birthDate;
    private final Map<String, Object> address = new LinkedHashMap<>();
    private String phone;

    public FakeInfo(PersonNames personNames, TownRepository townRepository, Random random) {
        this.personNames = personNames;
        this.townRepository = townRepository;
        this.random = random;

        setFullNameAndGender();
        setBirthDate();
        setCpr();
        setAddress();
        setPhone();
    }

    /**
     * Generates a fake CPR based on the birth date and gender:
     * ddMMyy + 3 random digits + a final digit that is even for females and odd for males.
     */
    private void setCpr() {
        int finalDigit = random.nextInt(5) * 2;
        if (GENDER_MASCULINE.equals(gender)) {
            finalDigit++;
        }

        cpr = birthDate.format(CPR_DATE_FORMAT)
            + getRandomDigit()
            + getRandomDigit()
            + getRandomDigit()
            + finalDigit;
    }

    /**
     * Generates a fake date of birth from 1900-01-01 to today.
     */
    private void setBirthDate() {
        long min = MIN_BIRTH_DATE.toEpochDay();
        long max = LocalDate.now().toEpochDay();
        birthDate = LocalDate.ofEpochDay(min + random.nextLong(max - min + 1));
    }

    /**
     * Generates a fake full name and gender,
     * extracting them randomly from the person names file.
     */
    private void setFullNameAndGender() {
        PersonNames.Person person = personNames.getRandomPerson(random);
        firstName = person.firstName();
        lastName = person.lastName();
        gender = person.gender();
    }

    /**
     * Generates a fake Danish address
     */
    private void setAddress() {
        address.put("street", getRandomStreet(STREET_LENGTH));

        String number = String.valueOf(randomBetween(1, 999));
        // Approx. 20% of Danish addresses include a letter
        if (randomBetween(1, 10) < 3) {
            number += Character.toUpperCase(LETTERS[random.nextInt(LETTERS.length)]);
        }
        address.put("number", number);

        // Approx. 30% of Danish addresses are in the ground floor
        if (randomBetween(1, 10) < 4) {
            address.put("floor", "st");
        } else {
            address.put("floor", randomBetween(1, 99));
        }

        /*
            The door is randomly generated based on the following distribution:
                1-7     35% th
                8-14    35% tv
                15-16   10% mf
                17-18   10% 1-50
                19      5% lowercase_letter + 1-999
                20      5% lowercase letter + '-' + 1-999
        */
        int doorType = randomBetween(1, 20);
        if (doorType < 8) {
            address.put("door", "th");
        } else if (doorType < 15) {
            address.put("door", "tv");
        } else if (doorType < 17) {
            address.put("door", "mf");
        } else if (doorType < 19) {
            address.put("door", randomBetween(1, 50));
        } else {
            String door = String.valueOf(LOWER_CASE_LETTERS[random.nextInt(LOWER_CASE_LETTERS.length)]);
            if (doorType == 20) {
                door += "-";
            }
            door += randomBetween(1, 999);
            address.put("door", door);
        }

        // Postal code and town
        TownRepository.Town town = townRepository.getRandomTown(random);
        address.put("postal_code", town.postalCode());
        address.put("town_name", town.townName());
    }

    /**
     * Returns a random street name made of letters (including Danish ones) and spaces.
     * The text never starts or ends with a space, nor contains two consecutive spaces.
     */

    private String getRandomStreet(int length) {
        StringBuilder text = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            boolean spaceAllowed = index > 0
                && index < length - 1
                && text.charAt(index - 1) != ' ';
            int choices = LETTERS.length + DANISH_LETTERS.length + (spaceAllowed ? 1 : 0);
            int pick = random.nextInt(choices);
            if (pick < LETTERS.length) {
                text.append(LETTERS[pick]);
            } else if (pick < LETTERS.length + DANISH_LETTERS.length) {
                text.append(DANISH_LETTERS[pick - LETTERS.length]);
            } else {
                text.append(' ');
            }
        }
        return text.toString();
    }

    /**
     * Generates a fake Danish phone number
     * starting with one of the prefixes in PHONE_PREFIXES
     */
    private void setPhone() {
        StringBuilder phoneNumber = new StringBuilder(PHONE_PREFIXES[random.nextInt(PHONE_PREFIXES.length)]);
        while (phoneNumber.length() < 8) {
            phoneNumber.append(getRandomDigit());
        }
        phone = phoneNumber.toString();
    }

    /**
     * @return The fake CPR
     */
    public String getCpr() {
        return cpr;
    }

    /**
     * @return {firstName, lastName, gender}
     */
    public Map<String, Object> getFullNameAndGender() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("firstName", firstName);
        result.put("lastName", lastName);
        result.put("gender", gender);
        return result;
    }

    /**
     * @return {firstName, lastName, gender, birthDate}
     */
    public Map<String, Object> getFullNameGenderAndBirthDate() {
        Map<String, Object> result = getFullNameAndGender();
        result.put("birthDate", birthDate.toString());
        return result;
    }

    /**
     * @return {CPR, firstName, lastName, gender}
     */
    public Map<String, Object> getCprFullNameAndGender() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("CPR", cpr);
        result.putAll(getFullNameAndGender());
        return result;
    }

    /**
     * @return {CPR, firstName, lastName, gender, birthDate}
     */
    public Map<String, Object> getCprFullNameGenderAndBirthDate() {
        Map<String, Object> result = getCprFullNameAndGender();
        result.put("birthDate", birthDate.toString());
        return result;
    }

    /**
     * @return {address: {street, number, floor, door, postal_code, town_name}}
     */
    public Map<String, Object> getAddress() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("address", new LinkedHashMap<>(address));
        return result;
    }

    /**
     * @return The fake Danish phone number
     */
    public String getPhoneNumber() {
        return phone;
    }

    /**
     * @return {CPR, firstName, lastName, gender, birthDate, address, phoneNumber}
     */
    public Map<String, Object> getFakePerson() {
        Map<String, Object> result = getCprFullNameGenderAndBirthDate();
        result.put("address", new LinkedHashMap<>(address));
        result.put("phoneNumber", phone);
        return result;
    }

    /**
     * Returns information about several fake persons
     *
     * @param amount The number of fake persons to generate, clamped between 2 and 100 inclusive
     */
    public List<Map<String, Object>> getFakePersons(int amount) {
        amount = Math.max(MIN_BULK_PERSONS, Math.min(MAX_BULK_PERSONS, amount));

        List<Map<String, Object>> bulkInfo = new ArrayList<>(amount);
        for (int index = 0; index < amount; index++) {
            bulkInfo.add(new FakeInfo(personNames, townRepository, random).getFakePerson());
        }
        return bulkInfo;
    }

    public List<Map<String, Object>> getFakePersons() {
        return getFakePersons(MIN_BULK_PERSONS);
    }

    private int randomBetween(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private String getRandomDigit() {
        return String.valueOf(random.nextInt(10));
    }
}
