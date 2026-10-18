package fakeinfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Holds the person names extracted from data/person-names.json.
 * The file is read only once, when the application starts.
 */
@Component
public class PersonNames {

    private static final String FILE_PERSON_NAMES = "data/person-names.json";

    public record Person(String firstName, String lastName, String gender) {}

    private final List<Person> persons;

    @Autowired
    public PersonNames(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(FILE_PERSON_NAMES).getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            List<Person> loaded = new ArrayList<>();
            for (JsonNode node : root.get("persons")) {
                loaded.add(new Person(
                    node.get("firstName").asText(),
                    node.get("lastName").asText(),
                    node.get("gender").asText()
                ));
            }
            this.persons = List.copyOf(loaded);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + FILE_PERSON_NAMES, e);
        }
    }

    public PersonNames(List<Person> persons) {
        this.persons = List.copyOf(persons);
    }

    /**
     * Returns a random person from the names file
     */
    public Person getRandomPerson(Random random) {
        return persons.get(random.nextInt(persons.size()));
    }
}
