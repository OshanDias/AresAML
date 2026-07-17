package aresaml;
 
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
 
/**
 * Main entry point. Builds a sample transaction network and runs all three
 * detection features: cycle detection, smurfing detection, and risk scoring.
 */
public class AresAML {
 
    public static void main(String[] args) {
        System.out.println("=== AresAML: Anti-Money Laundering Transaction Analysis Tool ===");
 
        AMLGraph graph = new AMLGraph();
 
        // ---- Accounts ----
        Account alice   = new Account("ACC001", "Alice");
        Account bob     = new Account("ACC002", "Bob");
        Account charlie = new Account("ACC003", "Charlie");
        Account hacker  = new Account("ACC666", "The Money Launderer");
        Account smurfer = new Account("ACC777", "Suspicious Smurfer");
        Account mule    = new Account("ACC888", "Money Mule");
 
        LocalDateTime now = LocalDateTime.now();
 
        // ---- 1. A circular laundering ring: Alice -> Bob -> Charlie -> Alice ----
        graph.addTransaction(new Transaction("TX101", alice, bob, 5000.00, now));
        graph.addTransaction(new Transaction("TX102", bob, charlie, 4800.00, now.plusMinutes(5)));
        graph.addTransaction(new Transaction("TX103", charlie, alice, 4500.00, now.plusMinutes(10)));
 
        // ---- 2. A normal, unrelated transaction ----
        graph.addTransaction(new Transaction("TX104", hacker, bob, 10000.00, now));
 
        // ---- 3. A smurfing / structuring pattern: 5 small transfers within 40 minutes ----
        graph.addTransaction(new Transaction("TX201", smurfer, mule, 2500.00, now));
        graph.addTransaction(new Transaction("TX202", smurfer, mule, 2400.00, now.plusMinutes(10)));
        graph.addTransaction(new Transaction("TX203", smurfer, mule, 2300.00, now.plusMinutes(20)));
        graph.addTransaction(new Transaction("TX204", smurfer, mule, 2200.00, now.plusMinutes(30)));
        graph.addTransaction(new Transaction("TX205", smurfer, mule, 2100.00, now.plusMinutes(40)));
 
        // ===== Run Feature 1: Cycle Detection =====
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
 
        // ===== Run Feature 2: Smurfing Detection =====
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
 
        // ===== Run Feature 3: Risk Scoring =====
        System.out.println("\n[3] Calculating risk scores...");
        Map<Account, Integer> scores = graph.calculateRiskScores(cycles, smurfing);
        scores.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> System.out.printf("    %-25s Risk Score: %d/100%n",
                        e.getKey().getOwnerName(), e.getValue()));
 
        System.out.println("\n=== Analysis Complete ===");
    }
}