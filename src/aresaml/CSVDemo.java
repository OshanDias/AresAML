package aresaml;

import java.util.List;
import java.util.Map;

/**
 * Alternative entry point that loads transactions from transactions.csv instead of
 * hardcoded demo data, then runs the exact same detection pipeline as AresAML.java.
 * This demonstrates real file-based input, as promised in the original proposal,
 * without changing any existing class.
 */
public class CSVDemo {

    public static void main(String[] args) {
        System.out.println("=== AresAML: Loading transactions from CSV ===");

        AMLGraph graph = new AMLGraph();
        String csvPath = "transactions.csv";

        try {
            int loaded = CSVTransactionLoader.loadFromCSV(csvPath, graph);
            System.out.println("[INFO] Loaded " + loaded + " transactions from " + csvPath);
        } catch (Exception e) {
            System.out.println("[ERROR] Could not load CSV file: " + e.getMessage());
            System.out.println("Make sure transactions.csv is in the project root folder.");
            return;
        }

        System.out.println("\n[1] Scanning for circular transaction rings...");
        List<List<Account>> cycles = graph.detectCycles();
        if (cycles.isEmpty()) {
            System.out.println("    No laundering rings detected.");
        } else {
            for (int i = 0; i < cycles.size(); i++) {
                List<Account> ring = cycles.get(i);
                StringBuilder sb = new StringBuilder("    Ring #" + (i + 1) + ": ");
                for (Account acc : ring) sb.append(acc.getOwnerName()).append(" -> ");
                sb.append(ring.get(0).getOwnerName());
                System.out.println(sb);
            }
        }

        System.out.println("\n[2] Scanning for smurfing / structuring patterns...");
        Map<Account, List<Transaction>> smurfing = graph.detectSmurfing(5000.00, 9000.00, 60);
        if (smurfing.isEmpty()) {
            System.out.println("    No structuring patterns detected.");
        } else {
            for (Map.Entry<Account, List<Transaction>> entry : smurfing.entrySet()) {
                double total = entry.getValue().stream().mapToDouble(Transaction::getAmount).sum();
                System.out.printf("    %s split $%.2f across %d small transactions%n",
                        entry.getKey().getOwnerName(), total, entry.getValue().size());
            }
        }

        System.out.println("\n[3] Calculating risk scores...");
        Map<Account, Integer> scores = graph.calculateRiskScores(cycles, smurfing);
        scores.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .filter(e -> e.getValue() > 0)
                .forEach(e -> System.out.printf("    %-25s Risk Score: %d/100%n",
                        e.getKey().getOwnerName(), e.getValue()));

        System.out.println("\n=== Analysis Complete ===");
    }
}
