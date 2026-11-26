
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

public class SchedulerGUI extends JFrame {

    private DefaultTableModel inputModel;
    private JTable inputTable;
    private JComboBox<String> algoBox;
    private JSpinner csSpinner;
    private JSpinner qSpinner;
    private JSpinner agingSpinner;

    private GanttPanel ganttPanel;
    private DefaultTableModel metricsModel;
    private DefaultTableModel matrixModel;
    private JLabel avgLabel;
    private JTextArea logArea;
    private JTabbedPane rightTabs;

    public SchedulerGUI() {
        setTitle("CS241 - Cloud-Cluster CPU Scheduling Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 740);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        root.add(buildTopPanel(), BorderLayout.NORTH);
        root.add(buildCenterPanel(), BorderLayout.CENTER);
        root.add(buildBottomPanel(), BorderLayout.SOUTH);

        loadTestCase1();
    }

    private JPanel buildTopPanel() {
        JPanel top = new JPanel(new BorderLayout(10, 10));

        // Left: Process Input Table
        JPanel processPanel = new JPanel(new BorderLayout(5, 5));
        processPanel.setBorder(new TitledBorder("Processes"));

        inputModel = new DefaultTableModel(new String[]{"PID", "Arrival Time", "Burst Time", "Priority"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return Integer.class;
            }
        };
        inputTable = new JTable(inputModel);
        inputTable.setRowHeight(24);
        JScrollPane tableScroll = new JScrollPane(inputTable);
        tableScroll.setPreferredSize(new Dimension(480, 150));
        processPanel.add(tableScroll, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton addBtn = new JButton("+ Add Process");
        JButton removeBtn = new JButton("- Remove");
        JButton tc1Btn = new JButton("Load Test Case 1");
        JButton tc2Btn = new JButton("Load Test Case 2");

        addBtn.addActionListener(e -> {
            int nextId = inputModel.getRowCount() + 1;
            inputModel.addRow(new Object[]{nextId, 0, 4, 1});
        });
        removeBtn.addActionListener(e -> {
            int row = inputTable.getSelectedRow();
            if (row >= 0) {
                inputModel.removeRow(row);
            }
        });
        tc1Btn.addActionListener(e -> loadTestCase1());
        tc2Btn.addActionListener(e -> loadTestCase2());

        btnRow.add(addBtn);
        btnRow.add(removeBtn);
        btnRow.add(tc1Btn);
        btnRow.add(tc2Btn);
        processPanel.add(btnRow, BorderLayout.SOUTH);

        // Right: Scheduler Configuration
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(new TitledBorder("Scheduler Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        algoBox = new JComboBox<>(new String[]{
            "Preemptive SJF",
            "Adaptive Container Scheduler (ACS)"
        });
        csSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 20, 1));
        qSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 50, 1));
        agingSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 50, 1));

        algoBox.addActionListener(e -> {
            boolean isACS = algoBox.getSelectedIndex() == 1;
            qSpinner.setEnabled(isACS);
            agingSpinner.setEnabled(isACS);
        });
        qSpinner.setEnabled(false);
        agingSpinner.setEnabled(false);

        gbc.gridx = 0;
        gbc.gridy = 0;
        configPanel.add(new JLabel("Algorithm:"), gbc);
        gbc.gridx = 1;
        configPanel.add(algoBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        configPanel.add(new JLabel("Context Switch (CS):"), gbc);
        gbc.gridx = 1;
        configPanel.add(csSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        configPanel.add(new JLabel("Time Quantum (Q):"), gbc);
        gbc.gridx = 1;
        configPanel.add(qSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        configPanel.add(new JLabel("Aging Interval:"), gbc);
        gbc.gridx = 1;
        configPanel.add(agingSpinner, gbc);

        JButton runBtn = new JButton("Run Simulation");
        runBtn.setFont(runBtn.getFont().deriveFont(Font.BOLD, 13f));
        runBtn.setForeground(new Color(20, 115, 55));
        runBtn.addActionListener(e -> runSimulation());

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        configPanel.add(runBtn, gbc);

        top.add(processPanel, BorderLayout.CENTER);
        top.add(configPanel, BorderLayout.EAST);
        return top;
    }

    private JPanel buildCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(5, 5));
        center.setBorder(new TitledBorder("Gantt Chart Timeline"));

        ganttPanel = new GanttPanel();
        JScrollPane ganttScroll = new JScrollPane(ganttPanel);
        ganttScroll.setPreferredSize(new Dimension(1040, 130));
        ganttScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ganttScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        center.add(ganttScroll, BorderLayout.CENTER);
        return center;
    }

    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new GridLayout(1, 2, 10, 0));
        bottom.setPreferredSize(new Dimension(1040, 280));

        // Left: Metrics Table
        JPanel metricsPanel = new JPanel(new BorderLayout(5, 5));
        metricsPanel.setBorder(new TitledBorder("Performance Metrics"));

        metricsModel = new DefaultTableModel(
                new String[]{"PID", "Arrival", "Burst", "Finish", "Turnaround", "Waiting", "Response"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable metricsTable = new JTable(metricsModel);
        metricsTable.setRowHeight(22);
        metricsPanel.add(new JScrollPane(metricsTable), BorderLayout.CENTER);

        avgLabel = new JLabel("Avg Waiting: -   |   Avg Turnaround: -   |   Avg Response: -");
        avgLabel.setFont(avgLabel.getFont().deriveFont(Font.BOLD));
        avgLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
        metricsPanel.add(avgLabel, BorderLayout.SOUTH);

        // Right: Tabbed View (Demotion Matrix Table + Text Log)
        rightTabs = new JTabbedPane();

        matrixModel = new DefaultTableModel(
                new String[]{"Time", "Running / State", "Tier 1 (RR Queue)", "Tier 2 (Priority Queue)"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable matrixTable = new JTable(matrixModel);
        matrixTable.setRowHeight(23);
        matrixTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        matrixTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        matrixTable.getColumnModel().getColumn(1).setPreferredWidth(145);
        matrixTable.getColumnModel().getColumn(2).setPreferredWidth(250);
        matrixTable.getColumnModel().getColumn(3).setPreferredWidth(270);

        JScrollPane matrixScroll = new JScrollPane(matrixTable);
        rightTabs.addTab("Heuristic Demotion Matrix", matrixScroll);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        rightTabs.addTab("Execution & Timeline Log", new JScrollPane(logArea));

        bottom.add(metricsPanel);
        bottom.add(rightTabs);
        return bottom;
    }

    private void loadTestCase1() {
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{1, 0, 4, 2});
        inputModel.addRow(new Object[]{2, 1, 7, 3});
        inputModel.addRow(new Object[]{3, 3, 2, 1});
        csSpinner.setValue(0);
        qSpinner.setValue(3);
        agingSpinner.setValue(4);
    }

    private void loadTestCase2() {
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{1, 0, 5, 4});
        inputModel.addRow(new Object[]{2, 1, 6, 2});
        inputModel.addRow(new Object[]{3, 4, 4, 3});
        csSpinner.setValue(1);
        qSpinner.setValue(3);
        agingSpinner.setValue(4);
    }

    private List<Process> readProcessesFromTable() {
        if (inputTable.isEditing()) {
            inputTable.getCellEditor().stopCellEditing();
        }
        List<Process> list = new ArrayList<>();
        for (int i = 0; i < inputModel.getRowCount(); i++) {
            int id = Integer.parseInt(inputModel.getValueAt(i, 0).toString());
            int arrival = Integer.parseInt(inputModel.getValueAt(i, 1).toString());
            int burst = Integer.parseInt(inputModel.getValueAt(i, 2).toString());
            int priority = Integer.parseInt(inputModel.getValueAt(i, 3).toString());
            list.add(new Process(id, arrival, burst, priority));
        }
        return list;
    }

    private void runSimulation() {
        try {
            List<Process> processes = readProcessesFromTable();
            if (processes.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Add at least one process.");
                return;
            }

            int cs = (int) csSpinner.getValue();
            int q = (int) qSpinner.getValue();
            int aging = (int) agingSpinner.getValue();

            List<SJFScheduler.Segment> timeline;
            double avgW, avgT, avgR;
            StringBuilder log = new StringBuilder();
            matrixModel.setRowCount(0);

            if (algoBox.getSelectedIndex() == 0) {
                SJFScheduler sjf = new SJFScheduler(processes, cs);
                sjf.run();
                timeline = sjf.getTimeline();
                avgW = sjf.getAvgWaitingTime();
                avgT = sjf.getAvgTurnaroundTime();
                avgR = sjf.getAvgResponseTime();

                log.append("=== Preemptive SJF (CS = ").append(cs).append(") ===\n");
                for (SJFScheduler.Segment s : timeline) {
                    log.append(s).append("\n");
                    for (int t = s.start; t < s.end; t++) {
                        matrixModel.addRow(new Object[]{t, s.label, "N/A (SJF)", "N/A (SJF)"});
                    }
                }
                rightTabs.setSelectedIndex(1);
            } else {
                timeline = runACSWithMatrix(processes, cs, q, aging, log);
                avgW = Metrics.calculateAverageWaitingTime(processes);
                avgT = Metrics.calculateAverageTurnaroundTime(processes);
                avgR = Metrics.calculateAverageResponseTime(processes);
                rightTabs.setSelectedIndex(0);
            }

            processes.sort(Comparator.comparingInt(p -> p.id));
            ganttPanel.setTimeline(timeline);

            metricsModel.setRowCount(0);
            for (Process p : processes) {
                metricsModel.addRow(new Object[]{
                    "P" + p.id, p.arrivalTime, p.burstTime,
                    p.completionTime, p.turnaroundTime, p.waitingTime, p.responseTime
                });
            }

            avgLabel.setText(String.format(
                    "Avg Waiting: %.2f   |   Avg Turnaround: %.2f   |   Avg Response: %.2f",
                    avgW, avgT, avgR
            ));
            logArea.setText(log.toString());
            logArea.setCaretPosition(0);

        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error running simulation: " + ex,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Executes ACS while recording the full per-timestep Demotion Matrix (Tier
     * 1 & Tier 2).
     */
    private List<SJFScheduler.Segment> runACSWithMatrix(
            List<Process> processes, int cs, int quantum, int agingInterval, StringBuilder log) {

        List<Process> allProcesses = new ArrayList<>(processes);
        allProcesses.sort(Comparator.comparingInt(p -> p.arrivalTime));

        Queue<Process> tier1 = new LinkedList<>();
        List<Process> tier2 = new ArrayList<>();
        List<SJFScheduler.Segment> segments = new ArrayList<>();
        List<String[]> matrixRows = new ArrayList<>();

        int time = 0;
        Process lastProcess = null;

        while (!allFinished(allProcesses)) {
            addArrivals(allProcesses, tier1, tier2, time);

            Process next = null;
            if (!tier1.isEmpty()) {
                next = tier1.peek();
            } else if (!tier2.isEmpty()) {
                next = selectTier2(tier2, time, agingInterval);
            }

            if (next == null) {
                matrixRows.add(new String[]{
                    String.valueOf(time), "IDLE", formatQueue(tier1), formatQueue(tier2)
                });
                addSegment(segments, time, time + 1, "IDLE");
                time++;
                lastProcess = null;
                continue;
            }

            // Context switch when switching between distinct processes
            if (lastProcess != null && lastProcess != next && cs > 0) {
                for (int c = 0; c < cs; c++) {
                    matrixRows.add(new String[]{
                        String.valueOf(time),
                        "CS (load P" + next.id + ")",
                        formatQueue(tier1),
                        formatQueue(tier2)
                    });
                    addSegment(segments, time, time + 1, "CS");
                    time++;
                    addArrivals(allProcesses, tier1, tier2, time);
                }
            }

            if (!tier1.isEmpty()) {
                // Tier 1: Round Robin
                Process p = tier1.poll();
                if (!p.started) {
                    p.responseTime = time - p.arrivalTime;
                    p.started = true;
                }

                int execTime = Math.min(quantum, p.remainingTime);
                for (int i = 0; i < execTime; i++) {
                    List<Process> t1Display = new ArrayList<>();
                    t1Display.add(p);
                    t1Display.addAll(tier1);

                    matrixRows.add(new String[]{
                        String.valueOf(time),
                        "P" + p.id + " (Tier 1)",
                        formatQueue(t1Display),
                        formatQueue(tier2)
                    });

                    addSegment(segments, time, time + 1, "P" + p.id);
                    time++;
                    p.remainingTime--;
                    p.lastProcessedTime = time;
                    addArrivals(allProcesses, tier1, tier2, time);
                }

                if (p.remainingTime == 0) {
                    p.completionTime = time;
                } else {
                    tier2.add(p);
                }
                lastProcess = p;

            } else if (!tier2.isEmpty()) {
                // Tier 2: Non-Preemptive Priority with Aging
                Process p = selectTier2(tier2, time, agingInterval);
                tier2.remove(p);

                while (p.remainingTime > 0) {
                    List<Process> t2Display = new ArrayList<>();
                    t2Display.add(p);
                    t2Display.addAll(tier2);

                    matrixRows.add(new String[]{
                        String.valueOf(time),
                        "P" + p.id + " (Tier 2)",
                        formatQueue(tier1),
                        formatQueue(t2Display)
                    });

                    addSegment(segments, time, time + 1, "P" + p.id);
                    time++;
                    p.remainingTime--;
                    p.lastProcessedTime = time;
                    addArrivals(allProcesses, tier1, tier2, time);
                }

                p.completionTime = time;
                lastProcess = p;
            }
        }

        // Final completion state row
        matrixRows.add(new String[]{
            String.valueOf(time), "Completed", "[Empty]", "[Empty]"
        });

        for (Process p : allProcesses) {
            Metrics.calculateAllMetrics(p);
        }

        log.append("=== Adaptive Container Scheduler (Q=").append(quantum)
                .append(", CS=").append(cs).append(", Aging=").append(agingInterval).append(") ===\n");
        for (SJFScheduler.Segment s : segments) {
            log.append(s).append("\n");
        }

        log.append("\n=== Heuristic Demotion Matrix (Per Time Step) ===\n");
        log.append(String.format("%-6s | %-16s | %-34s | %s%n", "Time", "Running/State", "Tier 1", "Tier 2"));
        log.append("-".repeat(92)).append("\n");

        for (String[] row : matrixRows) {
            matrixModel.addRow(row);
            log.append(String.format("%-6s | %-16s | %-34s | %s%n", row[0], row[1], row[2], row[3]));
        }

        return segments;
    }

    private void addArrivals(List<Process> all, Queue<Process> tier1, List<Process> tier2, int time) {
        for (Process p : all) {
            if (p.arrivalTime <= time && p.remainingTime > 0 && !p.started
                    && !tier1.contains(p) && !tier2.contains(p)) {
                tier1.add(p);
            }
        }
    }

    private boolean allFinished(List<Process> all) {
        for (Process p : all) {
            if (p.remainingTime > 0) {
                return false;
            }
        }
        return true;
    }

    private Process selectTier2(List<Process> tier2, int time, int agingInterval) {
        for (Process p : tier2) {
            int waited = time - p.lastProcessedTime;
            int agingAmount = waited / agingInterval;
            p.priority -= agingAmount;
            p.lastProcessedTime += agingAmount * agingInterval;
        }
        Process selected = null;
        for (Process p : tier2) {
            if (selected == null || p.priority < selected.priority) {
                selected = p;
            }
        }
        return selected;
    }

    private String formatQueue(Collection<Process> queue) {
        if (queue.isEmpty()) {
            return "[Empty]";
        }
        StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (Process p : queue) {
            if (i++ > 0) {
                sb.append(", ");
            }
            sb.append("P").append(p.id)
                    .append(" (rem: ").append(p.remainingTime)
                    .append(", prio: ").append(p.priority).append(")");
        }
        sb.append("]");
        return sb.toString();
    }

    private void addSegment(List<SJFScheduler.Segment> list, int start, int end, String label) {
        if (!list.isEmpty()) {
            SJFScheduler.Segment last = list.get(list.size() - 1);
            if (last.label.equals(label) && last.end == start && !label.equals("CS")) {
                last.end = end;
                return;
            }
        }
        list.add(new SJFScheduler.Segment(start, end, label));
    }

    // ---------- Custom Gantt Chart Renderer ----------
    private static class GanttPanel extends JPanel {

        private List<SJFScheduler.Segment> timeline = Collections.emptyList();
        private final Map<String, Color> colorMap = new HashMap<>();
        private final Color[] palette = {
            new Color(79, 129, 189),
            new Color(155, 187, 89),
            new Color(128, 100, 162),
            new Color(75, 172, 198),
            new Color(247, 150, 70),
            new Color(192, 80, 77)
        };

        public void setTimeline(List<SJFScheduler.Segment> timeline) {
            this.timeline = timeline;
            int totalUnits = timeline.isEmpty() ? 10 : timeline.get(timeline.size() - 1).end;
            int width = Math.max(980, totalUnits * 42 + 60);
            setPreferredSize(new Dimension(width, 100));
            revalidate();
            repaint();
        }

        private Color getColorFor(String label) {
            if (label.equals("CS")) {
                return new Color(220, 90, 90);
            }
            if (label.equals("IDLE")) {
                return new Color(190, 190, 190);
            }
            return colorMap.computeIfAbsent(label, k -> palette[colorMap.size() % palette.length]);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (timeline == null || timeline.isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int totalUnits = timeline.get(timeline.size() - 1).end;
            int leftMargin = 25;
            int availableWidth = getWidth() - 50;
            double unitWidth = Math.max(28.0, (double) availableWidth / Math.max(1, totalUnits));

            int boxY = 20;
            int boxH = 45;

            for (SJFScheduler.Segment seg : timeline) {
                int x1 = leftMargin + (int) Math.round(seg.start * unitWidth);
                int x2 = leftMargin + (int) Math.round(seg.end * unitWidth);
                int w = Math.max(18, x2 - x1);

                g2.setColor(getColorFor(seg.label));
                g2.fillRect(x1, boxY, w, boxH);

                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(x1, boxY, w, boxH);

                g2.setColor(seg.label.equals("IDLE") ? Color.DARK_GRAY : Color.WHITE);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int textX = x1 + (w - fm.stringWidth(seg.label)) / 2;
                int textY = boxY + (boxH + fm.getAscent()) / 2 - 2;
                g2.drawString(seg.label, textX, textY);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
                g2.drawString(String.valueOf(seg.start), x1 - 3, boxY + boxH + 16);
            }

            SJFScheduler.Segment last = timeline.get(timeline.size() - 1);
            int endX = leftMargin + (int) Math.round(last.end * unitWidth);
            g2.setColor(Color.BLACK);
            g2.drawString(String.valueOf(last.end), endX - 4, boxY + boxH + 16);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new SchedulerGUI().setVisible(true);
        });
    }
}
