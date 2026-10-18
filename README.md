# Fake Data Generator

## Purpose
Java (Spring Boot) REST API that generates fake data of nonexistent Danish persons.
Port of the PHP [Fake Data Generator](https://github.com/arturomorarioja/fake_info).
It works with the [Fake Data Generator Frontend](https://github.com/arturomorarioja/js_fake_info_frontend).

## Dependencies

- The fake persons' first name, last name, and gender are extracted from the file `src/main/resources/data/person-names.json`.
- The fake persons' postal code and town are extracted from the MariaDB/MySQL database `addresses` (`db/addresses.sql`).

## Usage
- Start: `docker compose up --build -d`
- Stop: `docker compose down -v`

The API runs at `http://localhost:8080`.

## API Endpoints
|Method|Endpoint|
|------|--------|
|GET|/cpr|
|GET|/name-gender|
|GET|/name-gender-dob|
|GET|/cpr-name-gender|
|GET|/cpr-name-gender-dob|
|GET|/address|
|GET|/phone|
|GET|/person|
|GET|/person?n=<number_of_fake_persons>|

## API Sample Output
`GET /cpr`
```json
{
    "CPR": "0412489054"
}
```

`GET /person`
```json
{
    "CPR": "0107832910",
    "firstName": "Michelle W.",
    "lastName": "Henriksen",
    "gender": "female",
    "birthDate": "1983-07-01",
    "address": {
        "street": "GYØVCoØMeceOjøtÆgvYrøQQDascNFCHArnSNrxub",
        "number": "521",
        "floor": 74,
        "door": "tv",
        "postal_code": "8670",
        "town_name": "Låsby"
    },
    "phoneNumber": "58676658"
}
```

`GET /person?n=3` returns an array of 3 persons (between 2 and 100 allowed).

Errors are returned as `{"error": "<message>"}` with status 400, 404, 405 or 500.

## Class `FakeInfo` - Public methods

```java
- getCpr(): String
- getFullNameAndGender(): Map
- getFullNameGenderAndBirthDate(): Map
- getCprFullNameAndGender(): Map
- getCprFullNameGenderAndBirthDate(): Map
- getAddress(): Map
- getPhoneNumber(): String
- getFakePerson(): Map
- getFakePersons(int amount): List<Map>
```

## Differences from the PHP version
- The last CPR digit is always even for females and odd for males.
- Birth dates are never in the future, and leap years are taken into account.
- Street names never contain double or trailing spaces.

## Tools
Java 21 / Spring Boot / MariaDB
