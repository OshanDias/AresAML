package aresaml;

import java.io.IOException;
import java.util.List;

/**
 * Demonstrates the new engine-driven pipeline end to end:
 *   CSV file  ->  AMLGraph  ->  AnalysisEngine.analyze()  ->  readable report
 *
 * Notice how short this is compared to the old AresAML.main(): it never calls the
 * detectors directly or knows the scoring rules. It just loads data, asks the
 * engine to analyse, and prints the result. A web controller would do the exact
 * same thing and return the result as JSON instead of printing it.
 *
 * Run this file directly (right-click -> Run File in NetBeans).
 */
public class AresEngineDemo {

    public static void main(String[] args) {
        String csvPath = (args.length > 0) ? args[0] : "transactions.csv";

        AMLGraph graph = new AMLGraph();
        try {
            int loaded = CSVTransactionLoader.loadFromCSV(csvPath, graph);
            System.out.println("Loaded " + loaded + " transactions from " + csvPath);
        } catch (IOException e) {
            System.out.println("Could not read " + csvPath + ": " + e.getMessage());
            return;
        }

        AnalysisResult result = new AnalysisEngine().analyze(graph);

        System.out.println("\n================ AresAML ANALYSIS REPORT ================");
        System.out.printf("Accounts analysed: %d   |   Alerts raised: %d%n",
                graph.getAllAccounts().size(), result.getAlerts().size());
        System.out.printf("Rings: %d   Smurfing: %d   Large transfers: %d%n",
                result.countAlertsOfType(Alert.Type.CYCLE),
                result.countAlertsOfType(Alert.Type.SMURFING),
                result.countAlertsOfType(Alert.Type.LARGE_TRANSACTION));

        System.out.println("\n---- ALERTS ----");
        if (!result.hasFindings()) {
            System.out.println("No suspicious activity detected.");
        } else {
            int n = 1;
            for (Alert alert : result.getAlerts()) {
                System.out.printf("%2d. %s%n", n++, alert);
            }
        }

        System.out.println("\n---- RISK RANKING (accounts worth reviewing) ----");
        List<RiskProfile> flagged = result.getProfilesAbove(1);
        if (flagged.isEmpty()) {
            System.out.println("No account scored above zero.");
        } else {
            for (RiskProfile profile : flagged) {
                System.out.println(profile); // name, score, band
                for (String reason : profile.getReasons()) {
                    System.out.println("      - " + reason);
                }
            }
        }
        System.out.println("\n========================================================");
    }
}
