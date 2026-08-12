package aresaml;
 
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
 
/**
 * GUI for AresAML using Java Swing.
 * The table and alert panel are now wired directly to the AMLGraph data
 * structure (previously the table used hardcoded rows).
 */
public class AresDashboard extends JFrame {
    private AMLGraph graph;
    private JTextArea alertArea;
    private JTable transactionTable;
    private DefaultTableModel tableModel;
 
    public AresDashboard() {
        setTitle("AresAML - Anti-Money Laundering Dashboard");
        setSize(850, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
 
        graph = new AMLGraph();
        setupSampleData();
 
        setupTopPanel();
        setupCenterPanel();
        setupBottomPanel();
 
        refreshTable(); // populate the table from the graph, not hardcoded values
 
        getContentPane().setBackground(new Color(240, 240, 240));
    }
 
    private void setupTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(41, 128, 185));
        JLabel titleLabel = new JLabel("AresAML SECURITY MONITOR");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        topPanel.add(titleLabel);
        add(topPanel, BorderLayout.NORTH);
    }
 
    private void setupCenterPanel() {
        String[] columns = {"ID", "Sender", "Receiver", "Amount", "Timestamp"};
        tableModel = new DefaultTableModel(columns, 0);
        transactionTable = new JTable(tableModel);
 
        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Live Transaction Feed"));
        add(scrollPane, BorderLayout.CENTER);
    }
 
    private void setupBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
 
        JButton analyzeBtn = new JButton("RUN NETWORK ANALYSIS");
        analyzeBtn.setBackground(new Color(192, 57, 43));
        analyzeBtn.setForeground(Color.WHITE);
        analyzeBtn.setFont(new Font("Arial", Font.BOLD, 16));
        analyzeBtn.addActionListener(e -> runAnalysis());
 
        alertArea = new JTextArea(12, 20);
        alertArea.setEditable(false);
        alertArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane alertScroll = new JScrollPane(alertArea);
        alertScroll.setBorder(BorderFactory.createTitledBorder("Cycle Detection / Smurfing Alerts / Risk Scores"));
 
        bottomPanel.add(analyzeBtn, BorderLayout.NORTH);
        bottomPanel.add(alertScroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
 
    private void setupSampleData() {
        Account alice   = new Account("ACC001", "Alice");
        Account bob     = new Account("ACC002", "Bob");
        Account charlie = new Account("ACC003", "Charlie");
        Account smurfer = new Account("ACC777", "Suspicious Smurfer");
        Account mule    = new Account("ACC888", "Money Mule");
 
        LocalDateTime now = LocalDateTime.now();
 
        graph.addTransaction(new Transaction("TX101", alice, bob, 5000.00, now));
        graph.addTransaction(new Transaction("TX102", bob, charlie, 4800.00, now.plusMinutes(5)));
        graph.addTransaction(new Transaction("TX103", charlie, alice, 4500.00, now.plusMinutes(10)));
 
        graph.addTransaction(new Transaction("TX201", smurfer, mule, 2500.00, now));
        graph.addTransaction(new Transaction("TX202", smurfer, mule, 2400.00, now.plusMinutes(10)));
        graph.addTransaction(new Transaction("TX203", smurfer, mule, 2300.00, now.plusMinutes(20)));
        graph.addTransaction(new Transaction("TX204", smurfer, mule, 2200.00, now.plusMinutes(30)));
        graph.addTransaction(new Transaction("TX205", smurfer, mule, 2100.00, now.plusMinutes(40)));
    }
 
    // Reads directly from the graph's real transaction data - this was the broken part before
    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Account account : graph.getAllAccounts()) {
            for (Transaction tx : graph.getOutgoingTransactions(account)) {
                tableModel.addRow(new Object[]{
                        tx.getTransactionId(),
                        tx.getSender().getOwnerName(),
                        tx.getReceiver().getOwnerName(),
                        String.format("$%.2f", tx.getAmount()),
                        tx.getTimestamp().toLocalTime()
                });
            }
        }
    }
 
    private void runAnalysis() {
        alertArea.setText("Scanning network...\n\n");
 
        List<List<Account>> cycles = graph.detectCycles();
        alertArea.append("--- CYCLE DETECTION ---\n");
        if (cycles.isEmpty()) {
            alertArea.append("[SAFE] No circular transaction rings found.\n");
        } else {
            for (List<Account> ring : cycles) {
                alertArea.append("[ALERT] Ring: ");
                for (Account acc : ring) alertArea.append(acc.getOwnerName() + " -> ");
                alertArea.append(ring.get(0).getOwnerName() + "\n");
            }
        }
 
        Map<Account, List<Transaction>> smurfing = graph.detectSmurfing(5000.00, 9000.00, 60);
        alertArea.append("\n--- SMURFING DETECTION ---\n");
        if (smurfing.isEmpty()) {
            alertArea.append("[SAFE] No structuring patterns found.\n");
        } else {
            for (Map.Entry<Account, List<Transaction>> entry : smurfing.entrySet()) {
                double total = entry.getValue().stream().mapToDouble(Transaction::getAmount).sum();
                alertArea.append(String.format("[ALERT] %s split $%.2f across %d transactions\n",
                        entry.getKey().getOwnerName(), total, entry.getValue().size()));
            }
        }
 
        Map<Account, Integer> scores = graph.calculateRiskScores(cycles, smurfing);
        alertArea.append("\n--- RISK SCORES ---\n");
        scores.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> alertArea.append(String.format("%-25s %d/100\n", e.getKey().getOwnerName(), e.getValue())));
    }
 
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AresDashboard().setVisible(true));
    }
}
