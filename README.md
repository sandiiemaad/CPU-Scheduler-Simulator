# 📌 CPU Scheduler Simulator

A **Java-based CPU Scheduling Simulator** that implements and tests multiple **CPU scheduling algorithms** commonly studied in **Operating Systems** courses.

The simulator reads **JSON test cases**, executes different scheduling algorithms, and validates results such as **execution order**, **waiting time**, and **turnaround time**.

---

## Supported Scheduling Algorithms

### 1️⃣ Preemptive Shortest Job First (SJF)
- Selects the process with the **shortest remaining burst time**
- Supports **context switching**
- Fully preemptive

### 2️⃣ Round Robin (RR)
- Uses a fixed **time quantum**
- Handles **context switching**
- Accurately tracks waiting time across multiple executions

### 3️⃣ Priority Scheduling (Preemptive)
- Lower numeric value = **higher priority**
- Supports:
  - Context switching
  - **Aging mechanism** to prevent starvation

### 4️⃣ AG Scheduler (Advanced / Hybrid)
A hybrid scheduling algorithm that combines:
- FCFS
- Priority Scheduling
- Preemptive SJF

Features:
- Dynamic quantum adjustment
- Priority preemption
- SJF-based preemption
- Quantum history tracking for each process

---

## 🧠 Key Features

-  Multiple scheduling algorithms
-  Preemptive execution
-  Context switch handling
-  Aging to prevent starvation
-  JSON-based test cases
-  Automatic validation of results
-  Average Waiting Time & Turnaround Time calculation
-  Clear execution order tracking

---

## 📂 Project Structure
CPU-Scheduler-Simulator/
│
├── src/
│   └── CPUSchedulers/
│
├── test_cases/
│   ├── Other_Schedulers/
│   │   ├── test_1.json
│   │   ├── test_2.json
│   │   ├── test_3.json
│   │   ├── test_4.json
│   │   ├── test_5.json
│   │   └── test_6.json
│   │
│   └── AG/
│       ├── AG_test1.json
│       ├── AG_test2.json
│       ├── AG_test3.json
│       ├── AG_test4.json
│       ├── AG_test5.json
│       └── AG_test6.json
│
└── README.md
