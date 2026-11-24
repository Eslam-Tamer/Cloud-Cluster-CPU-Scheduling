// ---------- Quick test ----------
public static void main(String[] args) {
    List<Process> list = new ArrayList<>();
    list.add(new Process(1, 0, 4, 2));
    list.add(new Process(2, 1, 7, 3));
    list.add(new Process(3, 3, 2, 1));

    int cs = 1;
    SJFScheduler sjf = new SJFScheduler(list, cs);
    sjf.run();

    System.out.println("=== Preemptive SJF (CS = " + cs + ") ===");
    System.out.println("Timeline:");
    for (SJFScheduler.Segment s : sjf.getTimeline()) System.out.println("  " + s);

    System.out.println("\nProcess  Arrival  Burst  Finish  Turnaround  Waiting  Response");
    for (Process p : list) {
        System.out.printf("P%-7d %-8d %-6d %-7d %-11d %-8d %d%n",
                p.id, p.arrivalTime, p.burstTime, p.completionTime,
                p.turnaroundTime, p.waitingTime, p.responseTime);
    }
    System.out.printf("%nAverage Waiting Time    : %.2f%n", sjf.getAvgWaitingTime());
    System.out.printf("Average Turnaround Time : %.2f%n", sjf.getAvgTurnaroundTime());
    System.out.printf("Average Response Time   : %.2f%n", sjf.getAvgResponseTime());
}