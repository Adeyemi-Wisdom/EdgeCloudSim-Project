 EdgeCloudSim – Enhanced Orchestration Strategy

 Overview

This project extends EdgeCloudSim, a mobile edge computing (MEC) simulator, with a new orchestration strategy that considers task criticality and deadline awareness.

It ensures that high-priority, time-sensitive tasks are executed efficiently across a two-tier edge–cloud architecture, leading to higher success rates, lower latency, and balanced VM loads.

 Problem Statement

Default EdgeCloudSim allocates tasks without considering:

* Task criticality (importance)
* Deadline constraints (urgency)
* VM load balancing

As a result:

* High-criticality tasks may fail or lag.
* Edge VMs are underutilized or overloaded.

Goal: Design an Enhanced Orchestrator (EO) that intelligently assigns tasks based on criticality, deadline, and VM availability, optimizing latency, load, and energy efficiency.


Enhanced Orchestration Strategy

Key Features

* Tasks have **criticality (0 or 1)** and **deadline** attributes.

* Orchestrator evaluates:

  * Criticality weight
  * Deadline slack
  * VM load
  * Network latency

* Tasks are assigned to most suitable VM dynamically.

 Architecture

1. **Edge Tier** → Handles high-criticality or low-latency tasks
2. **Cloud Tier** → Handles low-criticality or latency-tolerant tasks

Implementation Details

| File                                   | Purpose                                        |
| -------------------------------------- | ---------------------------------------------- |
| `Task.java`                            | Added `criticality` and `deadline` attributes  |
| `LimitedRandomTaskGenerator.java`      | Generates tasks with criticality and deadlines |
| `EnhancedOrchestrator.java`            | Implements EO scheduling logic                 |
| `SimulationManager.java`               | Registers EO orchestrator                      |
| `SimLogger.java`                       | Logs task success, latency, and VM load        |
| `applications.xml`, `edge_devices.xml` | Updated device tiers and application configs   |
| `runner` script                        | Executes simulations for EO strategy           |


Simplified Pseudocode

for each task {
    score = w1*(1/latency) + w2*(1/VM_load) + w3*(criticality) - w4*(deadline_risk)
    assign task to VM with max score
}


Running the Simulation

Step 1 – Locate the Simulator Folder

After cloning the repository, open your terminal and navigate to the simulator’s directory.
For example:


cd ~/Desktop/EdgeCloudSim

Step 2 – Move to the Sample Application Folder

cd scripts/sample_app1

Step 3 – Grant Execution Permission

Before running the simulator, make the shell scripts executable:

chmod +x compile.sh
chmod +x runner.sh

Step 4 – Compile the Simulator

Run the following command to compile all necessary Java files:

./compile.sh

Step 5 – Run the Simulation

Once compilation is complete, execute the simulation using:

./runner.sh result default_config edge_devices.xml applications.xml 1


This will start the experiment using the defined configuration files.

Step 6 – View Simulation Results

After the simulation completes, results are stored automatically under:

scripts/sample_app1/results/default_config/

You’ll find output files such as:


SIMRESULT_TWO_TIER_WITH_EO_NEXT_FIT_100DEVICES_ALL_APPS_GENERIC.log

These log files contain the data used to analyze metrics like task success ratio, latency, VM load, and energy performance.

 Results

* Task success ratio increased
* Average service and processing times reduced
* VM load balanced
* Slight network delay increase due to extra coordination

> Demonstrates that criticality- and deadline-aware orchestration improves MEC performance.

Credits

* Base Simulator: [EdgeCloudSim by BOUN](https://github.com/CagataySonmez/EdgeCloudSim)
