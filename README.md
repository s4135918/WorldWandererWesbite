# WorldWanderer — Assignment 4 (FlightSearch)

This project implements a single validation method `runFlightSearch(...)` for a flight search form.  
It returns **true** only when **all** assignment rules pass and, in that case, it **initialises** internal fields (origin, destination, dates, class, passenger counts, emergency row).  
If any rule fails, it returns **false** and does **not** change state.

## Tech
- Java 17+
- Gradle (JUnit 5)
- VS Code with “Extension Pack for Java”

## How to run tests
```bash
# Windows
gradlew test

# macOS/Linux
./gradlew test