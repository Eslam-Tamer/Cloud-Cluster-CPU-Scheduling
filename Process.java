public class Process {
    // Process information
    int id;
    int arrivalTime;
    int burstTime;
    int remainingTime;
    int priority;

    // Scheduling metrics
    int completionTime;
    int turnaroundTime;
    int waitingTime;
    int responseTime;

    boolean started;

    public Process(int id, int arrivalTime, int burstTime, int priority) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.priority = priority;

        this.completionTime = 0;
        this.turnaroundTime = 0;
        this.waitingTime = 0;
        this.responseTime = -1;

        this.started = false;
    }
}