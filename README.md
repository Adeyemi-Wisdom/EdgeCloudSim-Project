<<<<<<< HEAD
# EdgeCloudSim

EdgeCloudSim provides a simulation environment specific to Edge Computing scenarios where it is possible to conduct experiments that considers both computational and networking resources. EdgeCloudSim is based on CloudSim but adds considerable functionality so that it can be efficiently used for Edge Computing scenarios. EdgeCloudSim is an open source tool and any contributions are welcome. If you want to contribute EdgeCloudSim, please check below feature list and the [contributing guidelines](/CONTRIBUTING.md). If you want to use EdgeCloudSim in your research work, please cite our paper [[3]](https://onlinelibrary.wiley.com/doi/abs/10.1002/ett.3493).

## Discussion Forum

The discussion forum for EdgeCloudSim can be found [here](https://groups.google.com/forum/#!forum/edgecloudsim).
We hope to meet with all interested parties in this forum.
Please feel free to join and let us discuss issues, share ideas related to EdgeCloudSim all together.

## YouTube Channel

The YouTube channel of EdgeCloudSim can be found [here](https://www.youtube.com/channel/UC2gnXTWHHN6h4bk1D5gpcIA).
You can find some videos presenting our works and tutorials on this channel.
Click [here](https://youtu.be/SmQgRANWUts) to watch the video with brief information about EdgeCloudSim.

## Needed Features

* Task migration among the Edge or Cloud VMs
* Energy consumption model for the mobile and edge devices as well as the cloud datacenters
* Adding probabilistic network failure model by considering the congestion or other parameters such as the distance between mobile device and the WiFi access point.
* Visual tool for displaying the network topology

# EdgeCloudSim: An Environment for Performance Evaluation of Edge Computing Systems

EdgeCloudSim provides a modular architecture to provide support for a variety of crucial functionalities such as network modeling specific to WLAN and WAN, device mobility model, realistic and tunable load generator. As depicted in Figure 2, the current EdgeCloudSim version has five main modules available: Core Simulation, Networking, Load Generator, Mobility and Edge Orchestrator. To ease fast prototyping efforts, each module contains a default implementation that can be easily extended.

<p align="center">
  <img src="/doc/images/edgecloudsim_diagram.png" width="55%">
  <p align="center">
    Figure 1: Relationship between EdgeCloudSim modules.
  </p>
</p>

## Mobility Module
The mobility module manages the location of edge devices and clients. Since CloudSim focuses on the conventional cloud computing principles, the mobility is not considered in the framework. In our design, each mobile device has x and y coordinates which are updated according to the dynamically managed hash table. By default, we provide a nomadic mobility model, but different mobility models can be implemented by extending abstract MobilityModel class.

<p align="center">
  <img src="/doc/images/mobility_module.png" width="55%">
</p>

## Load Generator Module
The load generator module is responsible for generating tasks for the given configuration. By default, the tasks are generated according to a Poisson distribution via active/idle task generation pattern. If other task generation patterns are required, abstract LoadGeneratorModel class should be extended.

<p align="center">
  <img src="/doc/images/task_generator_module.png" width="50%">
</p>

## Networking Module
The networking module particularly handles the transmission delay in the WLAN and WAN by considering both upload and download data. The default implementation of the networking module is based on a single server queue model. Users of EdgeCloudSim can incorporate their own network behavior models by extending abstract NetworkModel class.

<p align="center">
  <img src="/doc/images/network_module.png" width="55%">
</p>

## Edge Orchestrator Module
The edge orchestrator module is the decision maker of the system. It uses the information collected from the other modules to decide how and where to handle incoming client requests. In the first version, we simply use a probabilistic approach to decide where to handle incoming tasks, but more realistic edge orchestrator can be added by extending abstract EdgeOrchestrator class.

<p align="center">
  <img src="/doc/images/edge_orchestrator_module.png" width="65%">
</p>

## Core Simulation Module
The core simulation module is responsible for loading and running the Edge Computing scenarios from the configuration files. In addition, it offers a logging mechanism to save the simulation results into the files. The results are saved in comma-separated value (CSV) data format by default, but it can be changed to any format.

## Extensibility
EdgeCloudSim uses a factory pattern making easier to integrate new models mentioned above. As shown in Figure 2, EdgeCloudsim requires a scenario factory class which knows the creation logic of the abstract modules. If you want to use different mobility, load generator, networking and edge orchestrator module, you can use your own scenario factory which provides the concrete implementation of your custom modules.

<p align="center">
  <img src="/doc/images/class_diagram.png" width="100%">
  <p align="center">
    Figure 2: Class Diagram of Important Modules
  </p>
</p>

## Ease of Use
At the beginning of our study, we observed that too many parameters are used in the simulations and managing these parameters programmatically is difficult.
As a solution, we propose to use configuration files to manage the parameters.
EdgeCloudSim reads parameters dynamically from the following files:
- **config.properties:** Simulation settings are managed in configuration file
- **applications.xml:** Application properties are stored in xml file
- **edge_devices.xml:** Edge devices (datacenters, hosts, VMs etc.) are defined in xml file

<p align="center">
  <img src="/doc/images/ease_of_use.png" width="60%">
</p>

## Compilation and Running
To compile sample application, *compile.sh* script which is located in *scripts/sample_application* folder can be used. You can rewrite similar script for your own application by modifying the arguments of javac command in way to declare the java file which includes your main method. Please note that this script can run on Linux based systems, including Mac OS. You can also use your favorite IDE (eclipse, netbeans etc.) to compile your project.

In order to run multiple sample_application scenarios in parallel, you can use *run_scenarios.sh* script which is located in *scripts/sample_application* folder. To run your own application, modify the java command in *runner.sh* script in a way to declare the java class which includes your main method. The details of using this script is explained in [this](/wiki/How-to-run-EdgeCloudSim-application-in-parallel) wiki page.

You can also monitor each process via the output files located under *scripts/sample_application/output/date* folder. For example:
```
./run_scenarios.sh 8 10
tail -f output/date/ite_1.log
```

## Analyzing the Results
At the end of each iteration, simulation results will be compressed in the *output/date/ite_n.tgz* files. When you extract these tgz files, you will see lots of log file in csv format. You can find matlab files which can plot graphics by using these files under *scripts/sample_application/matlab* folder. You can also write other scripts (e.g. python scripts) with the same manner of matlab plotter files.

## Example Output of EdgeCloudSim
You can plot lots of graphics by using the result of EdgeCloudSim. Some examples are given below:

![Alt text](/doc/images/result1.png?raw=true) ![Alt text](/doc/images/result2.png?raw=true)

![Alt text](/doc/images/result4.png?raw=true) ![Alt text](/doc/images/result5.png?raw=true)

![Alt text](/doc/images/result6.png?raw=true) ![Alt text](/doc/images/result3.png?raw=true)

![Alt text](/doc/images/result7.png?raw=true) ![Alt text](/doc/images/result8.png?raw=true)

## Publications
**[1]** C. Sonmez, A. Ozgovde and C. Ersoy, "[EdgeCloudSim: An environment for performance evaluation of Edge Computing systems](http://ieeexplore.ieee.org/document/7946405/)," *2017 Second International Conference on Fog and Mobile Edge Computing (FMEC)*, Valencia, 2017, pp. 39-44.

**[2]** C. Sonmez, A. Ozgovde and C. Ersoy, "[Performance evaluation of single-tier and two-tier cloudlet assisted applications](http://ieeexplore.ieee.org/document/7962674/)," *2017 IEEE International Conference on Communications Workshops (ICC Workshops)*, Paris, 2017, pp. 302-307.

**[3]** Sonmez C, Ozgovde A, Ersoy C. "[EdgeCloudSim: An environment for performance evaluation of Edge Computing systems](https://onlinelibrary.wiley.com/doi/abs/10.1002/ett.3493)," *Transactions on Emerging Telecommunications Technologies*, 2018;e3493.

**[4]** C. Sonmez, A. Ozgovde and C. Ersoy, "[Fuzzy Workload Orchestration for Edge Computing](https://ieeexplore.ieee.org/abstract/document/8651335/)," in *IEEE Transactions on Network and Service Management*, vol. 16, no. 2, pp. 769-782, June 2019.

**[5]** C. Sonmez, A. Ozgovde and C. Ersoy, "[Machine Learning-Based Workload Orchestrator for Vehicular Edge Computing](https://ieeexplore.ieee.org/abstract/document/9208723/)," in *IEEE Transactions on Intelligent Transportation Systems*, doi: 10.1109/TITS.2020.3024233.
=======
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
 2efa4b7a21aa8ed78e63b4cfedce3e4da258678b
