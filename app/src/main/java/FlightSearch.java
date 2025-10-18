import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Set;

/**
 * FlightSearch
 * A tiny “validation + initialise on success” class is used
 * The idea is: if all rules pass, we store the values and return true.
 * If any rule fails, the program returns false and leaves the object state unchanged.
 *
 * Notes:
 * - All string inputs are expected to be lowercase (per brief). To be safe,
 *   I normalise them to lowercase internally anyway.
 * - Date validation is strict using dd/MM/uuuu so invalid combos (e.g. 31/11/2025,
 *   or 29/02 on a non-leap year) are rejected by the parser, not by ad-hoc logic.
 */
public class FlightSearch {

    // stored attributes (visible via getters for tests) 
    private String origin;
    private String destination;
    private LocalDate departDate;
    private LocalDate returnDate;
    private int adults;
    private int children;
    private int infants;
    private boolean emergencyRow;
    private String seatClass; // economy, premium economy, business, first

    // Allowed values (kept here so tests can be short and the code is readable and easier to understand)
    private static final Set<String> ALLOWED_AIRPORTS =
            Set.of("syd", "mel", "lax", "cdg", "del", "pvg", "doh");

    private static final Set<String> ALLOWED_CLASSES =
            Set.of("economy", "premium economy", "business", "first");

    // Strict formatter: dd/MM/uuuu + STRICT = real calendar validation
    private static final DateTimeFormatter DF =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /**
     * Validates the search. If everything is valid, initialises fields and returns true.
     * If any validation fails, returns false and leaves the object state unchanged.
     *
     * Parameters are simple primitives/strings to keep the tests focused on logic.
     */
    public boolean runFlightSearch(
            String origin,
            String destination,
            String departDate,
            String returnDate,
            int adults,
            int children,
            int infants,
            boolean emergencyRow,
            String seatClass
    ) {
        // Copy params and normalise (so the program doesn't accidentally mutate fields on failure)
        String o = safeLower(origin);
        String d = safeLower(destination);
        String c = safeLower(seatClass);

        //  Condition 7: strict date format & real calendar dates 
        LocalDate dep;
        LocalDate ret;
        try {
            dep = LocalDate.parse(departDate, DF);
            ret = LocalDate.parse(returnDate, DF);
        } catch (Exception ex) {
            return false; // bad format or impossible date like 31/11/2025
        }

        //  Condition 6: departure not in the past (relative to now) 
        if (dep.isBefore(LocalDate.now())) return false;

        // Condition 8: two-way only + return >= depart 
        if (ret.isBefore(dep)) return false;

        // Condition 11: airports from the fixed set and not the same 
        if (!ALLOWED_AIRPORTS.contains(o)) return false;
        if (!ALLOWED_AIRPORTS.contains(d)) return false;
        if (o.equals(d)) return false;

        // Condition 9: class must be one of the four 
        if (!ALLOWED_CLASSES.contains(c)) return false;

        // Condition 1: total passengers in [1..9] 
        int total = adults + children + infants;
        if (total < 1 || total > 9) return false;

        // sanity: must have at least as many adults as required by ratios below
        if (adults < 0 || children < 0 || infants < 0) return false;

        // Condition 4: up to 2 children per adult (children aged 2–11) 
        if (children > adults * 2) return false;

        //  Condition 5: up to 1 infant per adult (infants <2, on lap) 
        if (infants > adults) return false;

        // Condition 2: children cannot be in emergency rows or first class 
        if (children > 0) {
            if (emergencyRow) return false;
            if ("first".equals(c)) return false;
        }

        // Condition 3: infants cannot be in emergency rows or business class 
        if (infants > 0) {
            if (emergencyRow) return false;
            if ("business".equals(c)) return false;
        }

        // Condition 10: only ECONOMY can have emergency rows 
        // All classes can be non-emergency, but if emergencyRow==true, class MUST be economy.
        if (emergencyRow && !"economy".equals(c)) return false;

        // If this point is reached, all validations passed.
        // initialise attributes to match the parameters.
        this.origin = o;
        this.destination = d;
        this.departDate = dep;
        this.returnDate = ret;
        this.adults = adults;
        this.children = children;
        this.infants = infants;
        this.emergencyRow = emergencyRow;
        this.seatClass = c;
        return true;
    }

    // helpers 

    private static String safeLower(String s) {
        return (s == null) ? null : s.toLowerCase();
    }

    // getters (for video demo) 
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public LocalDate getDepartDate() { return departDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public int getAdults() { return adults; }
    public int getChildren() { return children; }
    public int getInfants() { return infants; }
    public boolean isEmergencyRow() { return emergencyRow; }
    public String getSeatClass() { return seatClass; }
}