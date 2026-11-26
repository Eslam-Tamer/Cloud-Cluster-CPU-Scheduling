import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static class QueueEntry {
        final int id;
        final int remaining;
        final int priority;

        QueueEntry(int id, int remaining, int priority) {
            this.id = id;
            this.remaining = remaining;
            this.priority = priority;
        }
    }

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
            if (row >= 0) inputModel.removeRow(row);
        });
        tc1Btn.addActionListener(e -> loadTestCase1());
        tc2Btn.addActionListener(e -> loadTestCase2());

        btnRow.add(addBtn);
        btnRow.add(removeBtn);
        btnRow.add(tc1Btn);
        btnRow.add(tc2Btn);
        processPanel.add(btnRow, BorderLayout.SOUTH);

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

        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(new JLabel("Algorithm:"), gbc);
        gbc.gridx = 1;
        configPanel.add(algoBox, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        configPanel.add(new JLabel("Context Switch (CS):"), gbc);
        gbc.gridx = 1;
        configPanel.add(csSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        configPanel.add(new JLabel("Time Quantum (Q):"), gbc);
        gbc.gridx = 1;
        configPanel.add(qSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        configPanel.add(new JLabel("Aging Interval:"), gbc);
        gbc.gridx = 1;
        configPanel.add(agingSpinner, gbc);

        JButton runBtn = new JButton("Run Simulation");
        runBtn.setFont(runBtn.getFont().deriveFont(Font.BOLD, 13f));
        runBtn.setForeground(new Color(20, 115, 55));
        runBtn.addActionListener(e -> runSimulation());

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
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

        rightTabs.addTab("Heuristic Demotion Matrix", new JScrollPane(matrixTable));

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
                timeline = runUsingASC(processes, cs, q, aging, log);
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

    /** Drives ASC.java and pulls ASC.tier1, ASC.tier2, and ASC.ganttChart directly. */
    private List<SJFScheduler.Segment> runUsingASC(
            List<Process> processes, int cs, int q, int aging, StringBuilder log) {

        ASC.QUANTUM = q;
        ASC.AGING_INTERVAL = aging;
        ASC.CONTEXT_SWITCH = cs;
        ASC.time = 0;
        ASC.tier1.clear();
        ASC.tier2.clear();
        ASC.allProcesses.clear();
        ASC.ganttChart.setLength(0);
        ASC.lastProcess = null;

        ASC.allProcesses.addAll(processes);
        ASC.allProcesses.sort(Comparator.comparingInt(p -> p.arrivalTime));

        List<String[]> matrixRows = new ArrayList<>();

        while (!ASC.allFinished()) {
            ASC.addArrivals();
            Process next = ASC.getNextProcess();

            if (next == null) {
                matrixRows.add(new String[]{
                        String.valueOf(ASC.time), "IDLE", formatLiveQueue(ASC.tier1), formatLiveQueue(ASC.tier2)
                });
                ASC.ganttChart.append("IDLE [").append(ASC.time).append("-").append(ASC.time + 1).append("] ");
                ASC.time++;
                ASC.lastProcess = null;
                continue;
            }

            if (ASC.lastProcess != null && ASC.lastProcess != next && ASC.CONTEXT_SWITCH > 0) {
                int csStart = ASC.time;
                for (int c = 0; c < ASC.CONTEXT_SWITCH; c++) {
                    matrixRows.add(new String[]{
                            String.valueOf(ASC.time),
                            "CS (load P" + next.id + ")",
                            formatLiveQueue(ASC.tier1),
                            formatLiveQueue(ASC.tier2)
                    });
                    ASC.time++;
                    ASC.addArrivals();
                }
                ASC.ganttChart.append("CS [").append(csStart).append("-").append(ASC.time).append("] ");
            }

            if (!ASC.tier1.isEmpty()) {
                int stepStart = ASC.time;
                List<Process> t1Before = new ArrayList<>(ASC.tier1);
                List<Process> t2Before = new ArrayList<>(ASC.tier2);
                int startRem = next.remainingTime;

                ASC.executeRR();

                int stepEnd = ASC.time;
                for (int t = stepStart; t < stepEnd; t++) {
                    int elapsed = t - stepStart;
                    List<QueueEntry> t1Entries = new ArrayList<>();
                    for (Process p : t1Before) {
                        int rem = (p == next) ? (startRem - elapsed) : p.remainingTime;
                        t1Entries.add(new QueueEntry(p.id, rem, p.priority));
                    }
                    for (Process p : ASC.allProcesses) {
                        if (p.arrivalTime > stepStart && p.arrivalTime <= t && p != next) {
                            boolean exists = false;
                            for (Process existing : t1Before) {
                                if (existing.id == p.id) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                t1Entries.add(new QueueEntry(p.id, p.remainingTime, p.priority));
                            }
                        }
                    }

                    List<QueueEntry> t2Entries = new ArrayList<>();
                    for (Process p : t2Before) {
                        t2Entries.add(new QueueEntry(p.id, p.remainingTime, p.priority));
                    }

                    matrixRows.add(new String[]{
                            String.valueOf(t),
                            "P" + next.id + " (Tier 1)",
                            formatEntries(t1Entries),
                            formatEntries(t2Entries)
                    });
                }
            } else if (!ASC.tier2.isEmpty()) {
                int stepStart = ASC.time;
                List<Process> t2Before = new ArrayList<>(ASC.tier2);
                int startRem = next.remainingTime;

                ASC.executePriority();

                int stepEnd = ASC.time;
                for (int t = stepStart; t < stepEnd; t++) {
                    int elapsed = t - stepStart;
                    List<QueueEntry> t1Entries = new ArrayList<>();
                    for (Process p : ASC.allProcesses) {
                        if (p.arrivalTime > stepStart && p.arrivalTime <= t) {
                            t1Entries.add(new QueueEntry(p.id, p.remainingTime, p.priority));
                        }
                    }

                    List<QueueEntry> t2Entries = new ArrayList<>();
                    t2Entries.add(new QueueEntry(next.id, startRem - elapsed, next.priority));
                    for (Process p : t2Before) {
                        if (p != next) {
                            t2Entries.add(new QueueEntry(p.id, p.remainingTime, p.priority));
                        }
                    }

                    matrixRows.add(new String[]{
                            String.valueOf(t),
                            "P" + next.id + " (Tier 2)",
                            formatEntries(t1Entries),
                            formatEntries(t2Entries)
                    });
                }
            }
        }

        matrixRows.add(new String[]{
                String.valueOf(ASC.time), "Completed", "[Empty]", "[Empty]"
        });

        for (Process p : ASC.allProcesses) {
            Metrics.calculateAllMetrics(p);
        }

        List<SJFScheduler.Segment> segments = parseGanttString(ASC.ganttChart.toString());

        log.append("=== Adaptive Container Scheduler (Q=").append(q)
           .append(", CS=").append(cs).append(", Aging=").append(aging).append(") ===\n");
        for (SJFScheduler.Segment s : segments) {
            log.append(s).append("\n");
        }

        log.append("\n=== Heuristic Demotion Matrix (From ASC) ===\n");
        log.append(String.format("%-6s | %-16s | %-34s | %s%n", "Time", "Running/State", "Tier 1", "Tier 2"));
        log.append("-".repeat(92)).append("\n");

        for (String[] row : matrixRows) {
            matrixModel.addRow(row);
            log.append(String.format("%-6s | %-16s | %-34s | %s%n", row[0], row[1], row[2], row[3]));
        }

        return segments;
    }

    private String formatLiveQueue(Collection<Process> queue) {
        if (queue.isEmpty()) return "[Empty]";
        List<QueueEntry> entries = new ArrayList<>();
        for (Process p : queue) {
            entries.add(new QueueEntry(p.id, p.remainingTime, p.priority));
        }
        return formatEntries(entries);
    }

    private String formatEntries(List<QueueEntry> entries) {
        if (entries.isEmpty()) return "[Empty]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(", ");
            QueueEntry e = entries.get(i);
            sb.append("P").append(e.id)
              .append(" (rem: ").append(e.remaining)
              .append(", prio: ").append(e.priority).append(")");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<SJFScheduler.Segment> parseGanttString(String gantt) {
        List<SJFScheduler.Segment> segments = new ArrayList<>();
        Matcher m = Pattern.compile("(\\S+)\\s*\\[(\\d+)-(\\d+)]").matcher(gantt);
        while (m.find()) {
            String label = m.group(1);
            int start = Integer.parseInt(m.group(2));
            int end = Integer.parseInt(m.group(3));
            if (end > start) {
                if (!segments.isEmpty() && segments.get(segments.size() - 1).label.equals(label)
                        && segments.get(segments.size() - 1).end == start && !label.equals("CS")) {
                    segments.get(segments.size() - 1).end = end;
                } else {
                    segments.add(new SJFScheduler.Segment(start, end, label));
                }
            }
        }
        return segments;
    }

    // ---------- Gantt Chart Renderer ----------
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
            if (label.equals("CS")) return new Color(220, 90, 90);
            if (label.equals("IDLE")) return new Color(190, 190, 190);
            return colorMap.computeIfAbsent(label, k -> palette[colorMap.size() % palette.length]);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (timeline == null || timeline.isEmpty()) return;

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
            } catch (Exception ignored) {}
            new SchedulerGUI().setVisible(true);
        });
    }
}
