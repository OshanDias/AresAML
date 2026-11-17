package aresaml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The single entry point for running AML analysis.
 *
 * Any UI - the Swing dashboard, a future web API, or a test - builds an AMLGraph
 * and calls analyze(graph). It gets back one AnalysisResult containing every alert
 * and a scored, sorted risk profile for each account. No caller ever has to
 * orchestrate the three detectors or know the scoring rules.
 *
 * The scoring rules below match the original tool exactly (+40 per ring, +35 for
 * smurfing, +10 for a large transaction, capped at 100) so the numbers are
 * identical to what was demonstrated at viva - we have only added the reasons
 * and the structured output around them.
 */
public class AnalysisEngine {

    // --- Detection thresholds (same defaults the original tool used) ---
    public static final double SMURF_PER_TX_LIMIT    = 5000.00; // below this = a "small" transfer
    public static final double SMURF_SUSPICIOUS_TOTAL = 9000.00; // small transfers adding to this = suspicious
    public static final int    SMURF_WINDOW_MINUTES   = 60;      // how close together they must be
    public static final double LARGE_TX_THRESHOLD     = 5000.00; // a single transfer this big is notable

    // --- Risk points per rule ---
    static final int POINTS_PER_CYCLE      = 40;
    static final int POINTS_FOR_SMURFING   = 35;
    static final int POINTS_FOR_LARGE_TX   = 10;

    /**
     * Runs all three detectors against the graph and assembles the result.
     */
    public AnalysisResult analyze(AMLGraph graph) {
        List<Alert> alerts = new ArrayList<>();

        // One risk profile per account, keyed so we can accumulate points as rules fire.
        Map<Account, RiskProfile> profiles = new LinkedHashMap<>();
        for (Account account : graph.getAllAccounts()) {
            profiles.put(account, new RiskProfile(account));
        }

        // ---- Rule 1: circular laundering rings (DFS cycle detection) ----
        List<List<Account>> cycles = graph.detectCycles();
        for (List<Account> ring : cycles) {
            String path = describeRing(ring);
            alerts.add(new Alert(Alert.Type.CYCLE, new ArrayList<>(ring), POINTS_PER_CYCLE,
                    "Circular money flow detected: " + path));
            for (Account acc : ring) {
                profiles.get(acc).addReason(POINTS_PER_CYCLE, "Part of laundering ring " + path);
            }
        }

        // ---- Rule 2: smurfing / structuring (sliding time window) ----
        Map<Account, List<Transaction>> smurfing =
                graph.detectSmurfing(SMURF_PER_TX_LIMIT, SMURF_SUSPICIOUS_TOTAL, SMURF_WINDOW_MINUTES);
        for (Map.Entry<Account, List<Transaction>> entry : smurfing.entrySet()) {
            Account acc = entry.getKey();
            List<Transaction> window = entry.getValue();
            double total = window.stream().mapToDouble(Transaction::getAmount).sum();
            String detail = String.format("Split $%,.2f across %d small transfers within %d minutes",
                    total, window.size(), SMURF_WINDOW_MINUTES);
            alerts.add(new Alert(Alert.Type.SMURFING, List.of(acc), POINTS_FOR_SMURFING,
                    acc.getOwnerName() + ": " + detail));
            profiles.get(acc).addReason(POINTS_FOR_SMURFING, detail);
        }

        // ---- Rule 3: unusually large single transactions ----
        for (Account account : graph.getAllAccounts()) {
            for (Transaction tx : graph.getOutgoingTransactions(account)) {
                if (tx.getAmount() >= LARGE_TX_THRESHOLD) {
                    String detail = String.format("Sent a large transaction of $%,.2f (%s)",
                            tx.getAmount(), tx.getTransactionId());
                    alerts.add(new Alert(Alert.Type.LARGE_TRANSACTION, List.of(account), POINTS_FOR_LARGE_TX,
                            account.getOwnerName() + ": " + detail));
                    profiles.get(account).addReason(POINTS_FOR_LARGE_TX, detail);
                    break; // only count the large-transaction rule once per account
                }
            }
        }

        // ---- Finalise: cap at 100 and sort most-risky-first ----
        List<RiskProfile> sorted = new ArrayList<>(profiles.values());
        for (RiskProfile p : sorted) {
            p.capScore();
        }
        sorted.sort(Comparator.comparingInt(RiskProfile::getScore).reversed());

        return new AnalysisResult(alerts, sorted);
    }

    /** Renders a ring as "Alice -> Bob -> Charlie -> Alice" for readable alerts. */
    private String describeRing(List<Account> ring) {
        StringBuilder sb = new StringBuilder();
        for (Account acc : ring) {
            sb.append(acc.getOwnerName()).append(" -> ");
        }
        sb.append(ring.get(0).getOwnerName());
        return sb.toString();
    }
}
