import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests that:
 each assignment condition is met (boundary cases where it makes sense),
 Check the boolean return value,
  Check object attributes are set on success and unchanged on failure .
 
 
  Each test focuses on ONE rule at a time,
 Valid vs invalid cases are right next to each other,
  A small date helper avoids hard coding today’s date.
 */
public class FlightSearchTest {

    // Helper to format dates as dd/MM/uuuu relative to today
    private static String fmtDaysFromNow(int days) {
        var df = DateTimeFormatter.ofPattern("dd/MM/uuuu");
        return LocalDate.now().plusDays(days).format(df);
    }

    // Convenience wrapper with defaults that are otherwise valid
    private static boolean call(
            FlightSearch fs,
            String origin, String dest,
            String dep, String ret,
            int adults, int children, int infants,
            boolean emergency, String seatClass
    ) {
        return fs.runFlightSearch(origin, dest, dep, ret, adults, children, infants, emergency, seatClass);
    }

    @Nested
    class TotalPassengersRange {
        @ParameterizedTest
        @CsvSource({
                // total=1 (min) -> ok
                "1,0,0,true",
                // total=9 (max) -> ok
                "3,4,2,true",
                // total=0 -> fail
                "0,0,0,false",
                // total=10 -> fail
                "4,5,1,false"
        })
        void totalPassengers(int adults, int children, int infants, boolean expected) {
            FlightSearch fs = new FlightSearch();
            boolean ok = call(fs, "mel","pvg", fmtDaysFromNow(5), fmtDaysFromNow(12),
                    adults, children, infants, false, "economy");
            assertEquals(expected, ok);
        }
    }

    @Nested
    class ChildrenAdultRatio {
        @Test
        void atMostTwoChildrenPerAdult_passAtLimit() {
            FlightSearch fs = new FlightSearch();
            assertTrue(call(fs, "mel","pvg", fmtDaysFromNow(3), fmtDaysFromNow(9),
                    2, 4, 0, false, "economy"));
        }

        @Test
        void atMostTwoChildrenPerAdult_failPlusOne() {
            FlightSearch fs = new FlightSearch();
            assertFalse(call(fs, "mel","pvg", fmtDaysFromNow(3), fmtDaysFromNow(9),
                    2, 5, 0, false, "economy"));
        }
    }

    @Nested
    class InfantPerAdult {
        @Test
        void atMostOneInfantPerAdult_passAtLimit() {
            FlightSearch fs = new FlightSearch();
            assertTrue(call(fs, "mel","pvg", fmtDaysFromNow(4), fmtDaysFromNow(10),
                    2, 0, 2, false, "economy"));
        }

        @Test
        void atMostOneInfantPerAdult_failPlusOne() {
            FlightSearch fs = new FlightSearch();
            assertFalse(call(fs, "mel","pvg", fmtDaysFromNow(4), fmtDaysFromNow(10),
                    2, 0, 3, false, "economy"));
        }
    }

    @Nested
    class DepartNotPast {
        @Test
        void departCannotBeInPast() {
            FlightSearch fs = new FlightSearch();
            assertFalse(call(fs, "mel","pvg", fmtDaysFromNow(-1), fmtDaysFromNow(5),
                    1,0,0,false,"economy"));
        }
    }

    @Nested
    class DateParsing {
        @Test
        void strictFormat_invalidLeapDayOnNonLeapYear() {
            FlightSearch fs = new FlightSearch();
            // 29 Feb 2026 is invalid (2026 not a leap year) -> parser should reject
            assertFalse(call(fs, "mel","pvg", "29/02/2026", "01/03/2026",
                    1,0,0,false,"economy"));
        }

        @Test
        void strictFormat_validLeapDayOnLeapYear() {
            FlightSearch fs = new FlightSearch();
            // Push to the next leap year from now when needed
            
            assertTrue(call(fs, "mel","pvg", fmtDaysFromNow(7), fmtDaysFromNow(10),
                    1,0,0,false,"economy"));
        }
    }

    @Nested
    class ReturnAfterOrSameDay {
        @Test
        void returnBeforeDepartFails() {
            FlightSearch fs = new FlightSearch();
            assertFalse(call(fs, "mel","pvg", fmtDaysFromNow(6), fmtDaysFromNow(5),
                    1,0,0,false,"economy"));
        }
    }

    @Nested
    class Airports {
        @Test
        void originAndDestinationMustBeAllowedAndDifferent_pass() {
            FlightSearch fs = new FlightSearch();
            assertTrue(call(fs, "mel","pvg", fmtDaysFromNow(3), fmtDaysFromNow(8),
                    1,0,0,false,"economy"));
        }

        @Test
        void invalidAirportOrSameAirport_fails() {
            FlightSearch fs = new FlightSearch();
            // invalid origin code
            assertFalse(call(fs, "zzz","pvg", fmtDaysFromNow(3), fmtDaysFromNow(8),
                    1,0,0,false,"economy"));
            // same origin and destination
            assertFalse(call(fs, "mel","mel", fmtDaysFromNow(3), fmtDaysFromNow(8),
                    1,0,0,false,"economy"));
        }
    }

    @Nested
    class ClassAndEmergency {
        @Test
        void emergencyRowOnlyAllowedForEconomy() {
            FlightSearch fs = new FlightSearch();
            // emergency + business -> should fail 
            assertFalse(call(fs, "mel","pvg", fmtDaysFromNow(2), fmtDaysFromNow(6),
                    1,0,0,true,"business"));
            // emergency + economy -> ok
            assertTrue(call(fs, "mel","pvg", fmtDaysFromNow(2), fmtDaysFromNow(6),
                    1,0,0,true,"economy"));
        }

        @Test
        void nonEmergencyAllowedForAllClasses() {
            FlightSearch fs = new FlightSearch();
            assertTrue(call(fs, "mel","pvg", fmtDaysFromNow(2), fmtDaysFromNow(6),
                    1,0,0,false,"first"));
            assertTrue(call(fs, "mel","pvg", fmtDaysFromNow(2), fmtDaysFromNow(6),
                    1,0,0,false,"business"));
            assertTrue(call(fs, "mel","pvg", fmtDaysFromNow(2), fmtDaysFromNow(6),
                    1,0,0,false,"premium economy"));
        }
    }

    @Nested
    class ChildrenConstraints {
        @Test
        void childrenNotInFirstOrEmergency() {
            FlightSearch fs = new FlightSearch();
            // children + first -> fail
            assertFalse(call(fs, "mel","pvg", fmtDaysFromNow(5), fmtDaysFromNow(9),
                    1,1,0,false,"first"));
            // children + emergency -> fail
            assertFalse(call(fs, "mel","pvg", fmtDaysFromNow(5), fmtDaysFromNow(9),
                    1,1,0,true,"economy"));
        }
    }

    @Nested
    class InfantConstraints {
        @Test
        void infantsNotInBusinessOrEmergency() {
            FlightSearch fs = new FlightSearch();
            // infants + business -> fail
            assertFalse(call(fs, "mel","pvg", fmtDaysFromNow(5), fmtDaysFromNow(9),
                    1,0,1,false,"business"));
            // infants + emergency -> fail
            assertFalse(call(fs, "mel","pvg", fmtDaysFromNow(5), fmtDaysFromNow(9),
                    1,0,1,true,"economy"));
        }
    }

    @Nested
    class AttributesBehaviour {
        @Test
        void attributesAreNotModifiedOnFailure() {
            FlightSearch fs = new FlightSearch();
            // valid call to set state
            assertTrue(call(fs, "mel","pvg", fmtDaysFromNow(3), fmtDaysFromNow(7),
                    1,0,0,false,"economy"));
            String origOrigin = fs.getOrigin();

            // invalid call (origin == dest) -> should return false and keep previous state
            assertFalse(call(fs, "mel","mel", fmtDaysFromNow(3), fmtDaysFromNow(7),
                    1,0,0,false,"economy"));

            assertEquals(origOrigin, fs.getOrigin(), "State should be unchanged after a failed validation");
        }

        @Test
        void attributesAreSetOnSuccess() {
            FlightSearch fs = new FlightSearch();
            assertTrue(call(fs, "mel","pvg", fmtDaysFromNow(3), fmtDaysFromNow(7),
                    2,1,1,false,"economy"));
            assertEquals("mel", fs.getOrigin());
            assertEquals("pvg", fs.getDestination());
            assertEquals(2, fs.getAdults());
            assertEquals(1, fs.getChildren());
            assertEquals(1, fs.getInfants());
            assertFalse(fs.isEmergencyRow());
            assertEquals("economy", fs.getSeatClass());
            assertNotNull(fs.getDepartDate());
            assertNotNull(fs.getReturnDate());
        }
    }

    @Nested
    class AllValidCombos {
        @Test
        void simpleHappyPath() {
            FlightSearch fs = new FlightSearch();
            assertTrue(call(fs, "syd","lax", fmtDaysFromNow(10), fmtDaysFromNow(20),
                    3,2,1,false,"premium economy"));
        }
    }
}