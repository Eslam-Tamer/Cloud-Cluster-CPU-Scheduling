import java.util.ArrayList;
import java.util.List;

public class SJFScheduler {

    /** One entry in the execution timeline (for output / GUI). */
    public static class Segment {
        public final int start;
        public int end;
        public final String label; // "P1", "CS", "IDLE"

        public Segment(int start, int end, String label) {
            this.start = start;
            this.end = end;
            this.label = label;
        }

        @Override
        public String toString() {
            return "[" + start + " - " + end + "] " + label;
        }
    }

    private final List<Process> processes;
    private final int contextSwitch;
    private final List<Segment> timeline = new ArrayList<>();

    private double avgWaitingTime;
    private double avgTurnaroundTime;
    private double avgResponseTime;

    public SJFScheduler(List<Process> processes, int contextSwitch) {
        this.processes = processes;
        this.contextSwitch = contextSwitch;
    }

    public void run() {
        int n = processes.size();
        int time = 0;
        int done = 0;
        Process current = null; // process that ran in the previous time unit (if unfinished)
        Process last = null;    // last process that used the CPU

        while (done < n) {
            // 1) Find the shortest-remaining process among arrived ones
            Process best = null;
            for (Process p : processes) {
                if (p.arrivalTime <= time && p.remainingTime > 0) {
                    if (best == null
                            || p.remainingTime < best.remainingTime
                            || (p.remainingTime == best.remainingTime && p.arrivalTime < best.arrivalTime)
                            || (p.remainingTime == best.remainingTime && p.arrivalTime == best.arrivalTime && p.id < best.id)) {
                        best = p;
                    }
                }
            }

            // 2) Nobody ready -> CPU idle
            if (best == null) {
                addSegment(time, time + 1, "IDLE");
                time++;
                current = null;
                continue;
            }

            // 3) Keep the current process unless someone is strictly shorter
            if (current != null && current.remainingTime > 0
                    && current.remainingTime <= best.remainingTime) {
                best = current;
            }

            // 4) Context switch when moving to a different process
            if (last != null && last != best && contextSwitch > 0) {
                addSegment(time, time + contextSwitch, "CS");
                time += contextSwitch;
            }

            // 5) Run the chosen process for 1 time unit
            if (!best.started) {
                best.started = true;
                best.responseTime = time - best.arrivalTime;
            }
            addSegment(time, time + 1, "P" + best.id);
            best.remainingTime--;
            time++;
            last = best;
            current = best;

            // 6) Completion
            if (best.remainingTime == 0) {
                best.completionTime = time;
                best.turnaroundTime = best.completionTime - best.arrivalTime;
                best.waitingTime = best.turnaroundTime - best.burstTime;
                done++;
                current = null;
            }
        }

        computeAverages();
    }

    /** Adds a segment, merging it with the previous one if it has the same label. */
    private void addSegment(int start, int end, String label) {
        if (!timeline.isEmpty()) {
            Segment lastSeg = timeline.get(timeline.size() - 1);
            if (lastSeg.label.equals(label) && lastSeg.end == start && !label.equals("CS")) {
                lastSeg.end = end;
                return;
            }
        }
        timeline.add(new Segment(start, end, label));
    }

    private void computeAverages() {
        int n = processes.size();
        double w = 0, t = 0, r = 0;
        for (Process p : processes) {
            w += p.waitingTime;
            t += p.turnaroundTime;
            r += p.responseTime;
        }
        avgWaitingTime = w / n;
        avgTurnaroundTime = t / n;
        avgResponseTime = r / n;
    }

    // ---------- Getters (for teammates: metrics/output + GUI) ----------
    public List<Segment> getTimeline() { return timeline; }
    public double getAvgWaitingTime() { return avgWaitingTime; }
    public double getAvgTurnaroundTime() { return avgTurnaroundTime; }
    public double getAvgResponseTime() { return avgResponseTime; }


}