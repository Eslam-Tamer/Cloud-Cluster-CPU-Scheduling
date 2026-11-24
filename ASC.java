
import java.util.*;

public class ASC {

    static int QUANTUM;
    static int AGING_INTERVAL;
    static int CONTEXT_SWITCH;

    static int time = 0;

    static Queue<Process> tier1 = new LinkedList<>();
    static List<Process> tier2 = new ArrayList<>();
    static List<Process> allProcesses = new ArrayList<>();

    static StringBuilder ganttChart = new StringBuilder();

    // Previously executed process
    static Process lastProcess = null;

    // Add newly arrived processes to Tier 1
    static void addArrivals() {

        for (Process p : allProcesses) {

            if (p.arrivalTime <= time &&
                    p.remainingTime > 0 &&
                    !p.started &&
                    !tier1.contains(p) &&
                    !tier2.contains(p)) {

                tier1.add(p);
            }
        }
    }

    // Check if all processes are finished
    static boolean allFinished() {

        for (Process p : allProcesses) {
            if (p.remainingTime > 0)
                return false;
        }

        return true;
    }

    // Execute Round Robin (Tier 1)
    static void executeRR() {

        Process p = tier1.poll();

        if (p == null) return;

        int start = time;

        int executionTime = Math.min(QUANTUM, p.remainingTime);

        if (!p.started) {
            p.responseTime = time - p.arrivalTime;
            p.started = true;
        }

        for (int i = 0; i < executionTime; i++) {

            time++;
            p.remainingTime--;

            p.lastProcessedTime = time;


            addArrivals();
        }

        ganttChart.append("P").append(p.id)
                .append(" [").append(start)
                .append("-").append(time).append("] ");

        if (p.remainingTime == 0) {

            p.completionTime = time;

        } else {
            tier2.add(p);
        }
        lastProcess = p;
    }

    // Execute non-preemptive priority scheduling (Tier 2)
    static void executePriority() {

        Process p = selectTier2();

        if (p == null) return;

        tier2.remove(p);

        int start = time;

        while (p.remainingTime > 0) {

            time++;
            p.remainingTime--;

            p.lastProcessedTime = time;


            addArrivals();
        }

        ganttChart.append("P").append(p.id)
                .append(" [").append(start)
                .append("-").append(time).append("] ");

        p.completionTime = time;

        lastProcess = p;
    }

    // Select highest-priority process
    static Process selectTier2() {

        applyAging();

        Process selected = null;

        for (Process p : tier2) {

            if (selected == null ||
                    p.priority < selected.priority) {

                selected = p;
            }
        }

        return selected;
    }

    // Apply aging to Tier 2
    static void applyAging() {

        for (Process p : tier2) {

            int waitingTime = time - p.lastProcessedTime;

            int agingAmount = waitingTime / AGING_INTERVAL;

            p.priority -= agingAmount;

            p.lastProcessedTime += agingAmount * AGING_INTERVAL;
        }
    }

    // Get the next process according to tier precedence
    static Process getNextProcess() {

        if (!tier1.isEmpty()) {
            return tier1.peek();
        }
        if (!tier2.isEmpty()) {
            return selectTier2();
        }
        return null;
    }

    // Calculate scheduling metrics
    static void calculateResults() {

        System.out.println("\nPID\tCT\tTAT\tWT\tRT");

        for (Process p : allProcesses) {

            Metrics.calculateAllMetrics(p);

            System.out.println(
                    "P" + p.id + "\t" +
                            p.completionTime + "\t" +
                            p.turnaroundTime + "\t" +
                            p.waitingTime + "\t" +
                            p.responseTime
            );
        }

        System.out.println("\nAverage WT: " +
                Metrics.calculateAverageWaitingTime(allProcesses));

        System.out.println("Average TAT: " +
                Metrics.calculateAverageTurnaroundTime(allProcesses));

        System.out.println("Average RT: " +
                Metrics.calculateAverageResponseTime(allProcesses));
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        // Scheduling configuration
        System.out.print("Enter Time Quantum: ");
        QUANTUM = sc.nextInt();

        System.out.print("Enter Aging Interval: ");
        AGING_INTERVAL = sc.nextInt();

        System.out.print("Enter Context Switch Time: ");
        CONTEXT_SWITCH = sc.nextInt();

        System.out.print("Enter number of processes: ");
        int n = sc.nextInt();

        System.out.println("Enter Arrival Time, Burst Time, Priority:");

        for (int i = 1; i <= n; i++) {

            int arrival = sc.nextInt();
            int burst = sc.nextInt();
            int priority = sc.nextInt();

            allProcesses.add(
                    new Process(i, arrival, burst, priority)
            );
        }

        allProcesses.sort(
                Comparator.comparingInt(p -> p.arrivalTime)
        );

        while (!allFinished()) {

            addArrivals();

            Process next = getNextProcess();


            if (next == null) {

                time++;
                lastProcess = null;

                continue;
            }

            if (lastProcess != null && lastProcess != next) {

                int csStart = time;

                time += CONTEXT_SWITCH;

                ganttChart.append("CS [")
                        .append(csStart)
                        .append("-")
                        .append(time)
                        .append("] ");
                addArrivals();
            }

            if (!tier1.isEmpty()) {

                executeRR();

            } else if (!tier2.isEmpty()) {

                executePriority();
            }
        }

        System.out.println("\nGantt Chart:");
        System.out.println(ganttChart);

        calculateResults();

        sc.close();
    }
}
