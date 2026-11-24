import java.util.List;

public class Metrics {

    // Calculate Turnaround Time
    public static int calculateTurnaroundTime(Process p) {
        return p.completionTime - p.arrivalTime;
    }

    // Calculate Waiting Time
    public static int calculateWaitingTime(Process p) {
        return p.turnaroundTime - p.burstTime;
    }

    // Calculate Response Time
    public static int calculateResponseTime(Process p) {
        return p.responseTime;
    }

    // Calculate all metrics for one process
    public static void calculateAllMetrics(Process p) {

        p.turnaroundTime = calculateTurnaroundTime(p);

        p.waitingTime = calculateWaitingTime(p);
    }

    // Calculate Average Waiting Time
    public static double calculateAverageWaitingTime(
            List<Process> processes) {

        if (processes.isEmpty()) return 0;

        int total = 0;

        for (Process p : processes) {
            total += p.waitingTime;
        }

        return (double) total / processes.size();
    }

    // Calculate Average Turnaround Time
    public static double calculateAverageTurnaroundTime(
            List<Process> processes) {

        if (processes.isEmpty()) return 0;

        int total = 0;

        for (Process p : processes) {
            total += p.turnaroundTime;
        }

        return (double) total / processes.size();
    }

    // Calculate Average Response Time
    public static double calculateAverageResponseTime(
            List<Process> processes) {

        if (processes.isEmpty()) return 0;

        int total = 0;

        for (Process p : processes) {
            total += p.responseTime;
        }

        return (double) total / processes.size();
    }
}
