package fakeinfo;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST endpoints.
 * Trailing slashes are accepted.
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class FakeInfoController {

    private static final Pattern LEADING_INTEGER = Pattern.compile("^\\s*([+-]?\\d+)");

    private final PersonNames personNames;
    private final TownRepository townRepository;
    private final Random random = new Random();

    public FakeInfoController(PersonNames personNames, TownRepository townRepository) {
        this.personNames = personNames;
        this.townRepository = townRepository;
    }

    private FakeInfo newFakeInfo() {
        return new FakeInfo(personNames, townRepository, random);
    }

    @GetMapping({"/cpr", "/cpr/"})
    public Map<String, Object> cpr() {
        return Map.of("CPR", newFakeInfo().getCpr());
    }

    @GetMapping({"/name-gender", "/name-gender/"})
    public Map<String, Object> nameGender() {
        return newFakeInfo().getFullNameAndGender();
    }

    @GetMapping({"/name-gender-dob", "/name-gender-dob/"})
    public Map<String, Object> nameGenderDob() {
        return newFakeInfo().getFullNameGenderAndBirthDate();
    }

    @GetMapping({"/cpr-name-gender", "/cpr-name-gender/"})
    public Map<String, Object> cprNameGender() {
        return newFakeInfo().getCprFullNameAndGender();
    }

    @GetMapping({"/cpr-name-gender-dob", "/cpr-name-gender-dob/"})
    public Map<String, Object> cprNameGenderDob() {
        return newFakeInfo().getCprFullNameGenderAndBirthDate();
    }

    @GetMapping({"/address", "/address/"})
    public Map<String, Object> address() {
        return newFakeInfo().getAddress();
    }

    @GetMapping({"/phone", "/phone/"})
    public Map<String, Object> phone() {
        return Map.of("phoneNumber", newFakeInfo().getPhoneNumber());
    }

    @GetMapping({"/person", "/person/"})
    public Object person(@RequestParam(name = "n", required = false) String n) {
        long numPersons = n == null ? 1 : parseAmount(n);
        if (numPersons == 1) {
            return newFakeInfo().getFakePerson();
        }
        if (numPersons > 1 && numPersons <= FakeInfo.MAX_BULK_PERSONS) {
            return newFakeInfo().getFakePersons((int) numPersons);
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, ApiException.INCORRECT_PARAMS);
    }

    /**
     * Parses the amount of persons: the leading integer is used,
     * a non-numeric value becomes 0, and the sign is dropped.
     */
    static long parseAmount(String value) {
        Matcher matcher = LEADING_INTEGER.matcher(value);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Math.abs(Long.parseLong(matcher.group(1)));
        } catch (NumberFormatException e) {
            // Too large to parse, which is out of range anyway
            return Long.MAX_VALUE;
        }
    }
}
