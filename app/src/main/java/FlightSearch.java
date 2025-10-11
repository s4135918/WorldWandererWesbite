import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Objects;
import java.util.Set;

/**
 * Validates a flight-search request. If every rule passes:
 *  - saves inputs into fields (initialises state)
 *  - returns true
 * Otherwise returns false and leaves state unchanged.
 *
 * All input strings are expected to be lowercase.
 */
public class FlightSearch {

    // --- Saved only when inputs are valid (handy for the video/tests) ---
    private String origin;           // e.g., "mel"
    private String destination;      // e.g., "syd"
    private LocalDate departDate;    // parsed dd/MM/uuuu
    private LocalDate returnDate;    // parsed dd/MM/uuuu
    private String cabinClass;       // "economy", "premium economy", "business", "first"
    private int adults, children, infants;
    private boolean emergencyRow;

    // --- Constants from the brief ---
    private static final Set<String> ALLOWED_AIRPORTS =
            Set.of("syd", "mel", "lax", "cdg", "del", "pvg", "doh");

    private static final Set<String> ALLOWED_CLASSES =
            Set.of("economy", "premium economy", "business", "first");

    // Day-first strict parsing. "uuuu" + STRICT catches impossible dates like 31/11/2025, 29/02/2026 (non-leap).
    private static final DateTimeFormatter DMY =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    // Use Melbourne for "today" when checking "not in the past".
    private static final ZoneId MEL = ZoneId.of("Australia/Melbourne");

    public boolean runFlightSearch(
            String origin,
            String destination,
            String departDateStr,   // dd/MM/yyyy
            String returnDateStr,   // dd/MM/yyyy
            String cabinClass,
            int adults,
            int children,
            int infants,
            boolean requestEmergencyRow
    ) {
        // Work with locals first; only assign to fields at the very end on success.
        try {
            // 1) Airports: must be allowed and different
            if (!ALLOWED_AIRPORTS.contains(origin) || !ALLOWED_AIRPORTS.contains(destination)) return false;
            if (Objects.equals(origin, destination)) return false;

            // 2) Cabin class must be in the allowed set
            if (!ALLOWED_CLASSES.contains(cabinClass)) return false;

            // 3) Non-negative counts + total in [1..9]
            if (adults < 0 || children < 0 || infants < 0) return false;
            int total = adults + children + infants;
            if (total < 1 || total > 9) return false;

            // 4) Dates: strict dd/MM/yyyy; depart not in past; return >= depart
            LocalDate depart = parseStrict(departDateStr);
            LocalDate ret = parseStrict(returnDateStr);
            if (depart == null || ret == null) return false;

            LocalDate todayMel = LocalDate.now(MEL);
            if (depart.isBefore(todayMel)) return false;
            if (ret.isBefore(depart)) return false; // two-way only, not before

            // 5) Emergency row: economy only
            if (requestEmergencyRow && !"economy".equals(cabinClass)) return false;

            // 6) Children rules
            if (children > 0) {
                // not allowed in first class
                if ("first".equals(cabinClass)) return false;
                // not allowed in emergency rows at all
                if (requestEmergencyRow) return false;
                // must have at least 1 adult and at most 2 children per adult
                if (adults < 1) return false;
                if (children > adults * 2) return false;
            }

            // 7) Infant rules
            if (infants > 0) {
                // not allowed in business class
                if ("business".equals(cabinClass)) return false;
                // not allowed in emergency rows
                if (requestEmergencyRow) return false;
                // max 1 infant per adult
                if (infants > adults) return false;
            }

            // --- All rules passed: commit state and return true ---
            this.origin = origin;
            this.destination = destination;
            this.departDate = depart;
            this.returnDate = ret;
            this.cabinClass = cabinClass;
            this.adults = adults;
            this.children = children;
            this.infants = infants;
            this.emergencyRow = requestEmergencyRow;
            return true;

        } catch (Exception e) {
            // Any unexpected issue -> treat as invalid
            return false;
        }
    }

    private static LocalDate parseStrict(String dmy) {
        try {
            return LocalDate.parse(dmy, DMY);
        } catch (Exception e) {
            return null;
        }
    }

    // --- Getters for tests / demo video ---
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public LocalDate getDepartDate() { return departDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public String getCabinClass() { return cabinClass; }
    public int getAdults() { return adults; }
    public int getChildren() { return children; }
    public int getInfants() { return infants; }
    public boolean isEmergencyRow() { return emergencyRow; }
}