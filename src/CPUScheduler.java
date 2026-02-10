import java.util.*;
import java.nio.file.*;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

class Process {
    String name;

    @SerializedName("arrival")
    int arrivalTime;

    @SerializedName("burst")
    int burstTime;

    int remainingTime;
    int priority;

    int completionTime;
    int tempArrival ;

    public Process(String name, int arrivalTime, int burstTime, int priority) {
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.priority = priority;
    }

    public Process copy() {
        return new Process(name, arrivalTime, burstTime, priority);
    }
}

class ProcessResult {
    String name;
    int waitingTime;
    int turnaroundTime;

    ProcessResult(String name, int WT, int TT) {
        this.name = name;
        this.waitingTime = WT;
        this.turnaroundTime = TT;
    }
}

class SchedulerResult {
    List<String> executionOrder = new ArrayList<>();
    List<ProcessResult> processResults = new ArrayList<>();
    double averageWaitingTime;
    double averageTurnaroundTime;
}


// ================================
// Preemptive SJF
// ================================
class SJFScheduler {

    public SchedulerResult run(List<Process> processes, int contextSwitch, int quantum, int agingInterval) {

        List<Process> ps = new ArrayList<>();
        for (Process p : processes) ps.add(p.copy());

        SchedulerResult result = new SchedulerResult();

        int time = 0;
        int completed = 0;
        Process last = null;

        while (completed < ps.size()) {

            Process current = null;
            int minRemaining = Integer.MAX_VALUE;

            for (Process p : ps) {
                if (p.remainingTime > 0 && p.arrivalTime <= time) {
                    if (p.remainingTime < minRemaining) {
                        minRemaining = p.remainingTime;
                        current = p;
                    }
                }
            }

            if (current == null) {
                time++;
                continue;
            }

            if (last != null && last != current) {
                time += contextSwitch;
            }

            if (result.executionOrder.isEmpty() ||
                    !result.executionOrder.get(result.executionOrder.size() - 1).equals(current.name)) {
                result.executionOrder.add(current.name);
            }

            current.remainingTime--;
            time++;

            if (current.remainingTime == 0) {
                completed++;
                current.completionTime = time;
            }

            last = current;
        }

        // Fill results using corrected waiting times
        fillResults(ps, result);
        return result;
    }

    void fillResults(List<Process> ps, SchedulerResult r) {
        int totalWT = 0, totalTAT = 0;

        for (Process p : ps) {
            int tat = p.completionTime - p.arrivalTime;
            int wt = tat - p.burstTime;

            r.processResults.add(new ProcessResult(p.name, wt, tat));
            totalWT += wt;
            totalTAT += tat;
        }

        r.averageWaitingTime = Math.round(((double) totalWT / ps.size()) * 100.0) / 100.0;

        r.averageTurnaroundTime = Math.round(((double) totalTAT / ps.size()) * 100.0) / 100.0;

    }
}

// ================================
// Round Robin
// ================================
class RRScheduler {

    public SchedulerResult run(List<Process> processes, int contextSwitch, int quantum, int agingInterval) {

        List<Process> ps = new ArrayList<>();
        for (Process p : processes) ps.add(p.copy());

        SchedulerResult result = new SchedulerResult();

        Queue<Process> queue = new LinkedList<>();
        int time = 0;
        int completed = 0;

        Map<String, Integer> totalWaitingTime = new HashMap<>();
        Map<String, Integer> lastFinishTime = new HashMap<>();
        for (Process p : ps) {
            totalWaitingTime.put(p.name, 0);
            lastFinishTime.put(p.name, p.arrivalTime);
        }

        for (Process p : ps) {
            if (p.arrivalTime == 0) queue.add(p);
        }

        while (completed < ps.size()) {

            if (queue.isEmpty()) {
                time++;
                for (Process p : ps) {
                    if (p.remainingTime > 0 && p.arrivalTime == time) {
                        queue.add(p);
                    }
                }
                continue;
            }

            Process cur = queue.poll();

            int wait = time - lastFinishTime.get(cur.name);
            if (wait > 0) totalWaitingTime.put(cur.name, totalWaitingTime.get(cur.name) + wait);

            if (result.executionOrder.isEmpty() ||
                    !result.executionOrder.get(result.executionOrder.size() - 1).equals(cur.name)) {
                result.executionOrder.add(cur.name);
            }

            int run = Math.min(quantum, cur.remainingTime);
            int start = time;

            cur.remainingTime -= run;
            time += run;

            for (Process p : ps) {
                if (p.remainingTime > 0 &&
                        p.arrivalTime > start &&
                        p.arrivalTime <= time) {
                    queue.add(p);
                }
            }

            if (cur.remainingTime == 0) {
                completed++;
                cur.completionTime = time;
            } else {
                queue.add(cur);
            }

            // Update last finish time
            lastFinishTime.put(cur.name, time);

            // context switch
            for (int i = 0; i < contextSwitch; i++) {
                time++;
                for (Process p : ps) {
                    if (p.remainingTime > 0 && p.arrivalTime == time) {
                        queue.add(p);
                    }
                }
            }
        }

        // Fill results using corrected waiting times
        fillResults(ps, result, totalWaitingTime);
        return result;
    }

    void fillResults(List<Process> ps, SchedulerResult r, Map<String, Integer> totalWaitingTime) {
        int totalWT = 0, totalTAT = 0;

        for (Process p : ps) {
            int wt = totalWaitingTime.get(p.name);
            int tat = p.completionTime - p.arrivalTime;

            r.processResults.add(new ProcessResult(p.name, wt, tat));
            totalWT += wt;
            totalTAT += tat;
        }

        r.averageWaitingTime = Math.round(((double) totalWT / ps.size()) * 100.0) / 100.0;

        r.averageTurnaroundTime = Math.round(((double) totalTAT / ps.size()) * 100.0) / 100.0;

    }
}

class PriorityScheduler {

    public SchedulerResult run(
            List<Process> input,
            int contextSwitch,
            int quantum,
            int agingInterval
    ) {

        List<Process> processes = new ArrayList<>();
        for (Process p : input) {
            Process np = p.copy();
            np.remainingTime = np.burstTime;
            np.tempArrival = np.arrivalTime;
            processes.add(np);
        }

        SchedulerResult result = new SchedulerResult();

        PriorityQueue<Process> readyQueue = new PriorityQueue<>(
                (p1, p2) -> {
                    if (p1.priority != p2.priority)
                        return Integer.compare(p1.priority, p2.priority);
                    if (p1.arrivalTime != p2.arrivalTime)
                        return Integer.compare(p1.arrivalTime, p2.arrivalTime);
                    return p1.name.compareTo(p2.name);
                }
        );

        processes.sort(Comparator.comparingInt(p -> p.arrivalTime));

        int time = processes.get(0).arrivalTime;
        int i = 0;

        // initial arrivals
        while (i < processes.size() && processes.get(i).arrivalTime == time) {
            readyQueue.add(processes.get(i));
            i++;
        }

        String lastProcess = "";
        String lastExecuted = "";

        while (!readyQueue.isEmpty() || i < processes.size()) {

            Process current = null;
            String currentName = "Null";

            if (!readyQueue.isEmpty()) {
                current = readyQueue.poll();
                currentName = current.name;

                if (!current.name.equals(lastExecuted)) {
                    result.executionOrder.add(current.name);
                    lastExecuted = current.name;
                }
            }

            // ================= Context Switch =================
            if (!lastProcess.isEmpty()
                    && !lastProcess.equals(currentName)
                    && !lastProcess.equals("Null")) {

                if (current != null)
                    readyQueue.add(current);

                for (int c = 0; c < contextSwitch; c++) {
                    time++;
                    applyAging(readyQueue, time, agingInterval);

                    while (i < processes.size()
                            && processes.get(i).arrivalTime == time) {
                        readyQueue.add(processes.get(i));
                        i++;
                    }
                }

                lastProcess = currentName;
                continue;
            }

            lastProcess = currentName;

            //Execute 1 time unit
            time++;

            if (current != null)
                current.remainingTime--;

            applyAging(readyQueue, time, agingInterval);

            while (i < processes.size()
                    && processes.get(i).arrivalTime == time) {
                readyQueue.add(processes.get(i));
                i++;
            }

            if (current == null)
                continue;

            if (current.remainingTime > 0) {
                current.tempArrival = time;
                readyQueue.add(current);
            } else {
                current.completionTime = time;
            }
        }

        fillResults(processes, result);
        return result;
    }

    // Aging
    private void applyAging(
            PriorityQueue<Process> queue,
            int time,
            int agingInterval
    ) {
        if (agingInterval <= 0) return;

        List<Process> temp = new ArrayList<>(queue);
        queue.clear();

        for (Process p : temp) {
            if ((time - p.tempArrival) % agingInterval == 0) {
                p.priority = Math.max(1, p.priority - 1);
            }
            queue.add(p);
        }
    }

    // Fill results using corrected waiting times
    void fillResults(List<Process> ps, SchedulerResult r) {
        int totalWT = 0, totalTAT = 0;

        for (Process p : ps) {
            int tat = p.completionTime - p.arrivalTime;
            int wt = tat - p.burstTime;

            r.processResults.add(new ProcessResult(p.name, wt, tat));
            totalWT += wt;
            totalTAT += tat;
        }

        r.averageWaitingTime = Math.round(((double) totalWT / ps.size()) * 100.0) / 100.0;
        r.averageTurnaroundTime = Math.round(((double) totalTAT / ps.size()) * 100.0) / 100.0;

    }
}




// ================================
// AG Scheduler Classes
// ================================
class AGSchedulerProcess {
    String name;
    int arrivalTime;
    int burstTime;
    int remainingTime;
    int priority;
    int quantum;
    int completionTime;

    List<Integer> quantumHistory = new ArrayList<>();

    AGSchedulerProcess(String name, int arrival, int burst, int priority, int quantum) {
        this.name = name;
        this.arrivalTime = arrival;
        this.burstTime = burst;
        this.remainingTime = burst;
        this.priority = priority;
        this.quantum = quantum;
        quantumHistory.add(quantum);
    }
}

enum StopReason {
    NONE,
    PRIORITY_PREEMPT,
    SJF_PREEMPT
}

class AGSchedulerCore {
    int currentTime;
    ArrayList<AGSchedulerProcess> processes;
    ArrayList<AGSchedulerProcess> readyQueue;
    ArrayList<AGSchedulerProcess> allProcesses;
    ArrayList<String> executionOrder;
    StopReason lastStopReason;

    AGSchedulerCore(List<AGSchedulerProcess> processList) {
        currentTime = 0;
        processes = new ArrayList<>(processList);
        readyQueue = new ArrayList<>();
        allProcesses = new ArrayList<>(processList);
        executionOrder = new ArrayList<>();
        lastStopReason = StopReason.NONE;
    }

    // Move processes that have arrived to the ready queue
    void moveArrivedProcesses() {
        Iterator<AGSchedulerProcess> it = processes.iterator();
        while (it.hasNext()) {
            AGSchedulerProcess p = it.next();
            if (p.arrivalTime <= currentTime) {
                readyQueue.add(p);
                it.remove();
            }
        }
    }

    // Select the next process to run based on scheduling logic
    AGSchedulerProcess pickNextProcess() {
        if (readyQueue.isEmpty())
            return null;

        if (lastStopReason == StopReason.NONE)
            return readyQueue.remove(0);

        // Pick highest priority process if last stop was priority preemption
        if (lastStopReason == StopReason.PRIORITY_PREEMPT) {
            AGSchedulerProcess best = readyQueue.get(0);
            for (AGSchedulerProcess p : readyQueue)
                if (p.priority < best.priority)
                    best = p;
            readyQueue.remove(best);
            return best;
        }

        // Pick shortest remaining time if last stop was SJF preemption
        AGSchedulerProcess shortest = readyQueue.get(0);
        for (AGSchedulerProcess p : readyQueue)
            if (p.remainingTime < shortest.remainingTime)
                shortest = p;

        readyQueue.remove(shortest);
        return shortest;
    }

    void run() {
        while (!readyQueue.isEmpty() || !processes.isEmpty()) {

            moveArrivedProcesses();

            if (readyQueue.isEmpty()) {
                currentTime++;
                continue;
            }

            AGSchedulerProcess current = pickNextProcess();
            executionOrder.add(current.name);
            lastStopReason = StopReason.NONE;

            int quantum = current.quantum;
            int q25 = (int) Math.ceil(quantum * 0.25);
            int q50 = (int) Math.ceil(quantum * 0.5);
            int used = 0;

            // Run first 25% of quantum (Non preemptive FCFE)
            int run = Math.min(q25, current.remainingTime);
            current.remainingTime -= run;
            currentTime += run;
            used += run;
            moveArrivedProcesses();

            if (current.remainingTime == 0) {
                current.quantum = 0;
                current.quantumHistory.add(0);
                current.completionTime = currentTime;
                continue;
            }

            // Run the next 25% of quantum (Non preemptive Priority)
            while (used < q50 && current.remainingTime > 0) {
                AGSchedulerProcess hp = null;
                for (AGSchedulerProcess p : readyQueue)
                    if (hp == null || p.priority < hp.priority)
                        hp = p;

                // Preempt if a higher priority process exists
                if (hp != null && hp.priority < current.priority) {
                    int rem = quantum - used;
                    current.quantum = quantum + (int) Math.ceil(rem / 2.0);
                    current.quantumHistory.add(current.quantum);
                    readyQueue.add(current);
                    lastStopReason = StopReason.PRIORITY_PREEMPT;
                    break;
                }

                run = Math.min(q25, current.remainingTime);
                current.remainingTime -= run;
                currentTime += run;
                used += run;
                moveArrivedProcesses();
            }

            if (current.remainingTime == 0) {
                current.quantum = 0;
                current.quantumHistory.add(0);
                current.completionTime = currentTime;
                continue;
            }

            if (lastStopReason == StopReason.PRIORITY_PREEMPT)
                continue;

            // Run remaining quantum with SJF preemptive
            while (used < quantum && current.remainingTime > 0) {
                AGSchedulerProcess sj = null;
                for (AGSchedulerProcess p : readyQueue)
                    if (sj == null || p.remainingTime < sj.remainingTime)
                        sj = p;

                if (sj != null && sj.remainingTime < current.remainingTime) {
                    int rem = quantum - used;
                    current.quantum = quantum + rem;
                    current.quantumHistory.add(current.quantum);
                    readyQueue.add(current);
                    lastStopReason = StopReason.SJF_PREEMPT;
                    break;
                }

                current.remainingTime--;
                currentTime++;
                used++;
                moveArrivedProcesses();
            }

            if (current.remainingTime == 0) {
                current.quantum = 0;
                current.quantumHistory.add(0);
                current.completionTime = currentTime;
            } else if (lastStopReason == StopReason.NONE) {
                current.quantum = quantum + 2;
                current.quantumHistory.add(current.quantum);
                readyQueue.add(current);
            }
        }
    }
}

// JSON Models for AG
class AGTestCase {
    AGInput input;
    AGExpectedOutput expectedOutput;
}

class AGInput {
    List<AGSchedulerProcessJSON> processes;
}

class AGSchedulerProcessJSON {
    String name;
    int arrival;
    int burst;
    int priority;
    int quantum;
}

class AGExpectedOutput {
    List<String> executionOrder;
    List<ProcessResult> processResults;
}
//---------------------------------------------------------------

// Test Runner for SJF / RR /Priority
class TestCase {
    String name;
    TestInput input;
    ExpectedOutput expectedOutput;
}

class TestInput {
    int contextSwitch;
    int rrQuantum;
    int agingInterval;
    List<Process> processes;
}

class ExpectedOutput {
    SchedulerResult SJF;
    SchedulerResult RR;
    SchedulerResult Priority;
}

//For the other schedule ( SJF / RR / Priority)
class TestRunner {

    static void runTest(String path) throws Exception {

        Gson gson = new Gson();
        TestCase tc = gson.fromJson(Files.readString(Paths.get(path)), TestCase.class);

        System.out.println("\n=== Running Test: " + tc.name + " ===");

        SJFScheduler sjf = new SJFScheduler();
        RRScheduler rr = new RRScheduler();
        PriorityScheduler Priority = new PriorityScheduler();

        SchedulerResult sjfActual = sjf.run(
                tc.input.processes,
                tc.input.contextSwitch,
                tc.input.rrQuantum,
                tc.input.agingInterval
        );

        SchedulerResult rrActual = rr.run(
                tc.input.processes,
                tc.input.contextSwitch,
                tc.input.rrQuantum,
                tc.input.agingInterval
        );

        SchedulerResult prioActual = Priority.run(
                tc.input.processes,
                tc.input.contextSwitch,
                tc.input.rrQuantum,
                tc.input.agingInterval
        );

        // ===================== SJF =====================
        List<String> sjfFailures = new ArrayList<>();

        if (!sjfActual.executionOrder.equals(tc.expectedOutput.SJF.executionOrder)) {
            sjfFailures.add("[Execution Order] does not match\nExpected = "
                    + tc.expectedOutput.SJF.executionOrder
                    + "\nActual = " + sjfActual.executionOrder);
        }

        for (int i = 0; i < sjfActual.processResults.size(); i++) {
            ProcessResult actual = sjfActual.processResults.get(i);
            ProcessResult expected = tc.expectedOutput.SJF.processResults.get(i);

            if (actual.waitingTime != expected.waitingTime) {
                sjfFailures.add("[Waiting Time] does not match for " + actual.name
                        + "\nExpected = " + expected.waitingTime
                        + "\nActual = " + actual.waitingTime);
            }

            if (actual.turnaroundTime != expected.turnaroundTime) {
                sjfFailures.add("[Turnaround Time] does not match for " + actual.name
                        + "\nExpected = " + expected.turnaroundTime
                        + "\nActual = " + actual.turnaroundTime);
            }
        }

        if (!sjfFailures.isEmpty()) {
            System.out.println("[SJF] : FAILED");
            for (String f : sjfFailures) System.out.println(f);
        } else {
            System.out.println("[SJF] : PASSED");
        }

        System.out.println("Execution Order: " + sjfActual.executionOrder);
        System.out.println("Process Results:");
        for (ProcessResult pr : sjfActual.processResults) {
            System.out.println(pr.name + " | Waiting Time = " + pr.waitingTime
                    + " | Turnaround Time = " + pr.turnaroundTime);
        }
        System.out.println("Average Waiting Time = " + sjfActual.averageWaitingTime);
        System.out.println("Average Turnaround Time = " + sjfActual.averageTurnaroundTime);

        System.out.println("--------------------------------------------------");

        // ===================== RR =====================
        List<String> rrFailures = new ArrayList<>();

        if (!rrActual.executionOrder.equals(tc.expectedOutput.RR.executionOrder)) {
            rrFailures.add("[Execution Order] does not match\nExpected = "
                    + tc.expectedOutput.RR.executionOrder
                    + "\nActual = " + rrActual.executionOrder);
        }

        for (int i = 0; i < rrActual.processResults.size(); i++) {
            ProcessResult actual = rrActual.processResults.get(i);
            ProcessResult expected = tc.expectedOutput.RR.processResults.get(i);

            if (actual.waitingTime != expected.waitingTime) {
                rrFailures.add("[Waiting Time] does not match for " + actual.name
                        + "\nExpected = " + expected.waitingTime
                        + "\nActual = " + actual.waitingTime);
            }

            if (actual.turnaroundTime != expected.turnaroundTime) {
                rrFailures.add("[Turnaround Time] does not match for " + actual.name
                        + "\nExpected = " + expected.turnaroundTime
                        + "\nActual = " + actual.turnaroundTime);
            }
        }

        if (!rrFailures.isEmpty()) {
            System.out.println("[RR] : FAILED");
            for (String f : rrFailures) System.out.println(f);
        } else {
            System.out.println("[RR] : PASSED");
        }

        System.out.println("Execution Order: " + rrActual.executionOrder);
        System.out.println("Process Results:");
        for (ProcessResult pr : rrActual.processResults) {
            System.out.println(pr.name + " | Waiting Time = " + pr.waitingTime
                    + " | Turnaround Time = " + pr.turnaroundTime);
        }
        System.out.println("Average Waiting Time = " + rrActual.averageWaitingTime);
        System.out.println("Average Turnaround Time = " + rrActual.averageTurnaroundTime);
        System.out.println("--------------------------------------------------");


        // ===================== Priority =====================

        List<String> prioFailures = new ArrayList<>();

        // Check Execution Order
        if (!prioActual.executionOrder.equals(tc.expectedOutput.Priority.executionOrder)) {
            prioFailures.add("[Execution Order] does not match\nExpected = "
                    + tc.expectedOutput.Priority.executionOrder
                    + "\nActual = " + prioActual.executionOrder);
        }

        for (int i = 0; i < prioActual.processResults.size(); i++) {
            ProcessResult actual = prioActual.processResults.get(i);
            ProcessResult expected = tc.expectedOutput.Priority.processResults.get(i);

            if (actual.waitingTime != expected.waitingTime) {
                prioFailures.add("[Waiting Time] does not match for " + actual.name
                        + "\nExpected = " + expected.waitingTime
                        + "\nActual = " + actual.waitingTime);
            }

            if (actual.turnaroundTime != expected.turnaroundTime) {
                prioFailures.add("[Turnaround Time] does not match for " + actual.name
                        + "\nExpected = " + expected.turnaroundTime
                        + "\nActual = " + actual.turnaroundTime);
            }
        }

        if (!prioFailures.isEmpty()) {
            System.out.println("[Priority] : FAILED");
            for (String f : prioFailures) System.out.println(f);
        } else {
            System.out.println("[Priority] : PASSED");
        }

        System.out.println("Execution Order: " + prioActual.executionOrder);
        System.out.println("Process Results:");
        for (ProcessResult pr : prioActual.processResults) {
            System.out.println(pr.name + " | Waiting Time = " + pr.waitingTime
                    + " | Turnaround Time = " + pr.turnaroundTime);
        }
        System.out.println("Average Waiting Time = " + prioActual.averageWaitingTime);
        System.out.println("Average Turnaround Time = " + prioActual.averageTurnaroundTime);


    }
}



class AGTestRunner {

    static int counter = 1;

    public static void runAGTest(String path) throws Exception {

        Gson gson = new Gson();
        AGTestCase tcAG = gson.fromJson(Files.readString(Paths.get(path)), AGTestCase.class);

        // Create AG processes from JSON
        List<AGSchedulerProcess> agProcesses = new ArrayList<>();
        for (AGSchedulerProcessJSON p : tcAG.input.processes) {
            agProcesses.add(new AGSchedulerProcess(
                    p.name, p.arrival, p.burst, p.priority, p.quantum));
        }

        AGSchedulerCore ag = new AGSchedulerCore(agProcesses);
        ag.run();

        System.out.println("\n=== Running AG Test: " + counter++ + " ===");

        List<String> agFailures = new ArrayList<>();

        // Check Execution Order
        if (!ag.executionOrder.equals(tcAG.expectedOutput.executionOrder)) {
            agFailures.add("[Execution Order] does not match\nExpected = "
                    + tcAG.expectedOutput.executionOrder
                    + "\nActual = " + ag.executionOrder);
        }

        // Check Waiting Time / Turnaround Time
        if (tcAG.expectedOutput.processResults != null) {
            for (int i = 0; i < ag.allProcesses.size(); i++) {
                AGSchedulerProcess p = ag.allProcesses.get(i);
                int tat = p.completionTime - p.arrivalTime;
                int wt = tat - p.burstTime;
                ProcessResult expected = tcAG.expectedOutput.processResults.get(i);

                if (wt != expected.waitingTime) {
                    agFailures.add("[Waiting Time] does not match for " + p.name
                            + "\nExpected = " + expected.waitingTime
                            + "\nActual = " + wt);
                }

                if (tat != expected.turnaroundTime) {
                    agFailures.add("[Turnaround Time] does not match for " + p.name
                            + "\nExpected = " + expected.turnaroundTime
                            + "\nActual = " + tat);
                }
            }
        }

        if (!agFailures.isEmpty()) {
            System.out.println("[AG] : FAILED");
            for (String f : agFailures) System.out.println(f);
        } else {
            System.out.println("[AG] : PASSED");
        }

        System.out.println("Execution Order: " + ag.executionOrder);
        System.out.println("Process Results:");

        double totalWT = 0;
        double totalTAT = 0;

        for (AGSchedulerProcess p : ag.allProcesses) {
            int tat = p.completionTime - p.arrivalTime;
            int wt = tat - p.burstTime;

            totalWT += wt;
            totalTAT += tat;

            System.out.println(p.name +
                    " | Waiting Time = " + wt +
                    " | Turnaround Time = " + tat +
                    " | Quantum History: " + p.quantumHistory);
        }

        double avgWT = Math.round(((double) totalWT / ag.allProcesses.size()) * 100.0) / 100.0;
        double avgTAT = Math.round(((double) totalTAT / ag.allProcesses.size()) * 100.0) / 100.0;

        System.out.println("Average Waiting Time = " + avgWT);
        System.out.println("Average Turnaround Time = " + avgTAT);

        System.out.println("----------------------------------------------------");
    }

    public static void runMultipleAGTests(String[] paths) throws Exception {
        for (String path : paths) {
            runAGTest(path);
        }
    }
}

// ================================
// Main
// ================================
public class CPUScheduler {
    public static void main(String[] args) throws Exception {

        // Run SJF / RR / Priority Tests
        TestRunner.runTest( "test_cases/Other_Schedulers/test_1.json");
        System.out.println("====================================================================");
        TestRunner.runTest( "test_cases/Other_Schedulers/test_2.json");
        System.out.println("====================================================================");
        TestRunner.runTest( "test_cases/Other_Schedulers/test_3.json");
        System.out.println("====================================================================");
        TestRunner.runTest( "test_cases/Other_Schedulers/test_4.json");
        System.out.println("====================================================================");
        TestRunner.runTest( "test_cases/Other_Schedulers/test_5.json");
        System.out.println("====================================================================");
        TestRunner.runTest( "test_cases/Other_Schedulers/test_6.json");
        System.out.println("====================================================================");

        System.out.println("\n\t\t\t\t\t\tAG SCHEDULE TESTS");

        // Run AG Scheduler separately
        String[] agPaths = new String[] {
                "test_cases/AG/AG_test1.json",
                "test_cases/AG/AG_test2.json",
                "test_cases/AG/AG_test3.json",
                "test_cases/AG/AG_test4.json",
                "test_cases/AG/AG_test5.json",
                "test_cases/AG/AG_test6.json"
        };

        AGTestRunner.runMultipleAGTests(agPaths);


    }
}