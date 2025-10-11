import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

public class FlightSearchTest {

    private static final ZoneId MEL = ZoneId.of("Australia/Melbourne");

    // Helper: format a Melbourne-local date as dd/MM/yyyy
    private String fmt(LocalDate d) {
        return String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    // Helper: today + n days, already formatted
    private String todayPlus(int days) {
        return fmt(LocalDate.now(MEL).plusDays(days));
    }

    private boolean call(
            String origin, String destination,
            String dep, String ret,
            String clazz,
            int adults, int children, int infants,
            boolean emergency
    ) {
        return new FlightSearch().runFlightSearch(origin, destination, dep, ret, clazz, adults, children, infants, emergency);
    }

    // ---- 1) Total passengers 1..9 ----
    @Nested
    class TotalPassengersRange {
        @Test
        void zero_or_ten_invalid() {
            String dep = todayPlus(2), ret = todayPlus(5);
            assertFalse(call("mel","syd", dep, ret, "economy", 0,0,0,false)); // total 0
            assertFalse(call("mel","syd", dep, ret, "economy",10,0,0,false)); // total 10
        }
        @Test
        void one_and_nine_valid() {
            String dep = todayPlus(2), ret = todayPlus(5);
            assertTrue(call("mel","syd", dep, ret, "economy", 1,0,0,false));  // total 1
            assertTrue(call("mel","syd", dep, ret, "economy", 9,0,0,false));  // total 9
        }
    }

    // ---- 2) Children: not in first; not in emergency rows ----
    @Nested
    class ChildrenConstraints {
        @Test
        void children_in_first_or_emergency_invalid() {
            String dep = todayPlus(3), ret = todayPlus(6);
            assertFalse(call("mel","syd", dep, ret, "first", 1,1,0,false));   // first + child
            assertFalse(call("mel","syd", dep, ret, "economy",1,1,0,true));   // emergency + child
        }
        @Test
        void child_in_economy_non_emergency_valid() {
            String dep = todayPlus(3), ret = todayPlus(6);
            assertTrue(call("mel","syd", dep, ret, "economy",1,1,0,false));
        }
    }

    // ---- 3) Infants: not in business; not in emergency rows ----
    @Nested
    class InfantConstraints {
        @Test
        void infant_in_business_or_emergency_invalid() {
            String dep = todayPlus(4), ret = todayPlus(7);
            assertFalse(call("mel","syd", dep, ret, "business",1,0,1,false)); // business + infant
            assertFalse(call("mel","syd", dep, ret, "economy", 1,0,1,true));  // emergency + infant
        }
        @Test
        void infant_in_economy_non_emergency_valid() {
            String dep = todayPlus(4), ret = todayPlus(7);
            assertTrue(call("mel","syd", dep, ret, "economy",1,0,1,false));
        }
    }

    // ---- 4) Children need adults; ≤2 children per adult ----
    @Nested
    class ChildrenAdultRatio {
        @Test
        void no_adult_or_over_ratio_invalid() {
            String dep = todayPlus(2), ret = todayPlus(3);
            assertFalse(call("mel","syd", dep, ret, "economy",0,1,0,false));  // no adult
            assertFalse(call("mel","syd", dep, ret, "economy",1,3,0,false));  // 3 > 2*1
        }
        @Test
        void boundary_two_per_adult_valid() {
            String dep = todayPlus(2), ret = todayPlus(3);
            assertTrue(call("mel","syd", dep, ret, "economy",2,4,0,false));   // 4 == 2*2
        }
    }

    // ---- 5) Infants: ≤ adults (1 per adult) ----
    @Nested
    class InfantPerAdult {
        @Test
        void more_infants_than_adults_invalid() {
            String dep = todayPlus(2), ret = todayPlus(4);
            assertFalse(call("mel","syd", dep, ret, "economy",1,0,2,false));
        }
        @Test
        void infants_equal_adults_valid() {
            String dep = todayPlus(2), ret = todayPlus(4);
            assertTrue(call("mel","syd", dep, ret, "economy",2,0,2,false));
        }
    }

    // ---- 6) Date format & calendar validity ----
    @Nested
    class DateParsing {
        @Test
        void bad_format_or_impossible_date_invalid() {
            String badDep = "2025-12-01";   // wrong format
            String dep = todayPlus(5);
            String badRet = "29/02/2026";   // 2026 is not a leap year
            assertFalse(call("mel","syd", badDep, todayPlus(7), "economy", 1,0,0,false));
            assertFalse(call("mel","syd", dep, badRet, "economy", 1,0,0,false));
        }
        @Test
        void strict_ddMMyyyy_valid() {
            assertTrue(call("mel","syd", todayPlus(10), todayPlus(12), "economy", 1,0,0,false));
        }
    }

    // ---- 7) Departure not in the past (Melbourne) ----
    @Nested
    class DepartNotPast {
        @Test
        void yesterday_invalid_today_valid() {
            LocalDate y = LocalDate.now(MEL).minusDays(1);
            assertFalse(call("mel","syd", fmt(y), todayPlus(3), "economy", 1,0,0,false));
            assertTrue(call("mel","syd", todayPlus(0), todayPlus(3), "economy", 1,0,0,false));
        }
    }

    // ---- 8) Two-way only: return >= depart ----
    @Nested
    class ReturnAfterOrSameDay {
        @Test
        void return_before_depart_invalid_same_or_after_valid() {
            String dep = todayPlus(5);
            assertFalse(call("mel","syd", dep, todayPlus(4), "economy", 1,0,0,false)); // before
            assertTrue(call("mel","syd", dep, dep,            "economy", 1,0,0,false)); // same
            assertTrue(call("mel","syd", dep, todayPlus(6), "economy", 1,0,0,false));   // after
        }
    }

    // ---- 9) Class membership + 10) Emergency row only in economy ----
    @Nested
    class ClassAndEmergency {
        @Test
        void unknown_class_invalid_and_emergency_in_non_economy_invalid() {
            String dep = todayPlus(6), ret = todayPlus(8);
            assertFalse(call("mel","syd", dep, ret, "ultra", 1,0,0,false));     // not in set
            assertFalse(call("mel","syd", dep, ret, "business",1,0,0,true));     // emergency only economy
        }
        @Test
        void economy_emergency_ok_when_no_kids_or_infants() {
            String dep = todayPlus(6), ret = todayPlus(8);
            assertTrue(call("mel","syd", dep, ret, "economy",2,0,0,true));
        }
    }

    // ---- 11) Airport membership + origin != destination ----
    @Nested
    class Airports {
        @Test
        void must_be_allowed_and_different() {
            String dep = todayPlus(2), ret = todayPlus(3);
            assertFalse(call("xyz","syd", dep, ret, "economy", 1,0,0,false)); // not allowed
            assertFalse(call("mel","mel", dep, ret, "economy", 1,0,0,false)); // equal
        }
        @Test
        void allowed_pair_valid() {
            String dep = todayPlus(2), ret = todayPlus(3);
            assertTrue(call("mel","syd", dep, ret, "economy", 1,0,0,false));
        }
    }

    // ---- All-valid scenarios (≥4 combos) ----
    @Nested
    class AllValidCombos {
        @Test
        void four_variations_all_true() {
            assertTrue(call("mel","syd", todayPlus(3),  todayPlus(6),  "economy",          1,0,0,false));
            assertTrue(call("syd","lax", todayPlus(10), todayPlus(20), "premium economy",  2,0,0,false));
            assertTrue(call("cdg","del", todayPlus(15), todayPlus(18), "economy",          2,2,0,false)); // 2 adults, 2 kids
            assertTrue(call("pvg","doh", todayPlus(7),  todayPlus(9),  "economy",          2,0,2,false)); // infants==adults
        }
    }
}