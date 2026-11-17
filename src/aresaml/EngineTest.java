package aresaml;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A lightweight, dependency-free self-check for the AnalysisEngine.
 *
 * It builds a known transaction network where we know the right answers by hand,
 * runs the engine, and verifies every number. No JUnit needed - just right-click
 * -> Run File. Prints PASS/FAIL per check and a final summary; exits non-zero if
 * anything fails, so it can also be wired into a build later.
 *
 * Being able to say "I can prove my detection logic is correct, here are the
 * tests" is worth a lot in an interview.
 */
public class EngineTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("Running AresAML engine self-check...\n");

        AMLGraph graph = buildKnownScenario();
        AnalysisResult result = new AnalysisEngine().analyze(graph);

        // Index the scored profiles by owner name for easy lookup.
        java.util.Map<String, Integer> scoreByName = new java.util.HashMap<>();
        for (RiskProfile p : result.getRiskProfiles()) {
            scoreByName.put(p.getAccount().getOwnerName(), p.getScore());
        }

        // Expected outcomes, worked out by hand from the scenario below:
        check("A laundering ring is detected",
                result.countAlertsOfType(Alert.Type.CYCLE) >= 1);
        check("A smurfing pattern is detected",
                result.countAlertsOfType(Alert.Type.SMURFING) == 1);
        check("Alice scores 50 (ring 40 + large transfer 10)",
                scoreByName.getOrDefault("Alice", -1) == 50);
        check("Bob scores 40 (ring only)",
                scoreByName.getOrDefault("Bob", -1) == 40);
        check("Charlie scores 40 (ring only)",
                scoreByName.getOrDefault("Charlie", -1) == 40);
        check("Smurfer scores 35 (structuring only)",
                scoreByName.getOrDefault("Suspicious Smurfer", -1) == 35);
        check("Launderer scores 10 (one large transfer)",
                scoreByName.getOrDefault("The Money Launderer", -1) == 10);
        check("Clean account (Money Mule) scores 0",
                scoreByName.getOrDefault("Money Mule", -1) == 0);
        check("No score ever exceeds 100",
                result.getRiskProfiles().stream().allMatch(p -> p.getScore() <= 100));
        check("Every alert carries a non-empty explanation",
                result.getAlerts().stream().allMatch(a -> a.getDescription() != null && !a.getDescription().isBlank()));
        check("Highest-risk account is sorted first",
                result.getRiskProfiles().get(0).getScore() == 50);

        System.out.printf("%n----------------------------------------%n");
        System.out.printf("RESULT: %d passed, %d failed%n", passed, failed);
        if (failed > 0) {
            System.out.println("SELF-CHECK FAILED");
            System.exit(1);
        } else {
            System.out.println("ALL CHECKS PASSED");
        }
    }

    /** Same shape as the original AresAML demo, so the expected numbers are known. */
    private static AMLGraph buildKnownScenario() {
        AMLGraph graph = new AMLGraph();

        Account alice   = new Account("ACC001", "Alice");
        Account bob     = new Account("ACC002", "Bob");
        Account charlie = new Account("ACC003", "Charlie");
        Account hacker  = new Account("ACC666", "The Money Launderer");
        Account smurfer = new Account("ACC777", "Suspicious Smurfer");
        Account mule    = new Account("ACC888", "Money Mule");

        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 9, 0);

        // Ring: Alice -> Bob -> Charlie -> Alice (Alice's 5000 transfer also trips the large-tx rule)
        graph.addTransaction(new Transaction("TX101", alice, bob, 5000.00, now));
        graph.addTransaction(new Transaction("TX102", bob, charlie, 4800.00, now.plusMinutes(5)));
        graph.addTransaction(new Transaction("TX103", charlie, alice, 4500.00, now.plusMinutes(10)));

        // Unrelated large transfer
        graph.addTransaction(new Transaction("TX104", hacker, bob, 10000.00, now));

        // Smurfing: 5 small transfers within 40 minutes (total 11,500)
        graph.addTransaction(new Transaction("TX201", smurfer, mule, 2500.00, now));
        graph.addTransaction(new Transaction("TX202", smurfer, mule, 2400.00, now.plusMinutes(10)));
        graph.addTransaction(new Transaction("TX203", smurfer, mule, 2300.00, now.plusMinutes(20)));
        graph.addTransaction(new Transaction("TX204", smurfer, mule, 2200.00, now.plusMinutes(30)));
        graph.addTransaction(new Transaction("TX205", smurfer, mule, 2100.00, now.plusMinutes(40)));

        return graph;
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + label);
        } else {
            failed++;
            System.out.println("  FAIL  " + label);
        }
    }
}
