/*
 * Title:        EdgeCloudSim - Basic Edge Orchestrator implementation
 * 
 * Description: 
 * BasicEdgeOrchestrator implements basic algorithms which are
 * first/next/best/worst/random fit algorithms while assigning
 * requests to the edge devices.
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.edge_orchestrator;

import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.Cloudlet;

import edu.boun.edgecloudsim.cloud_server.CloudVM;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimUtils;

public class BasicEdgeOrchestrator extends EdgeOrchestrator {
    private int numberOfHost; //used by load balancer
    private int lastSelectedHostIndex; //used by load balancer
    private int[] lastSelectedVmIndexes; //used by each host individually

    // threshold for deadline-based decision (seconds) — tune as needed
    private static final double DEADLINE_THRESHOLD = 10.0;

    public BasicEdgeOrchestrator(String _policy, String _simScenario) {
        super(_policy, _simScenario);
    }

    @Override
    public void initialize() {
        numberOfHost = SimSettings.getInstance().getNumOfEdgeHosts();

        lastSelectedHostIndex = -1;
        lastSelectedVmIndexes = new int[numberOfHost];
        for (int i = 0; i < numberOfHost; i++)
            lastSelectedVmIndexes[i] = -1;
    }

    /**
     * Decide whether to offload to EDGE or CLOUD.
     * MUST return either SimSettings.GENERIC_EDGE_DEVICE_ID or SimSettings.CLOUD_DATACENTER_ID
     */
    @Override
    public int getDeviceToOffload(Task task) {
        double length = task.getCloudletLength();
        long inputSize = task.getCloudletFileSize();
        long outputSize = task.getCloudletOutputSize();

        // Default decision: cloud
        int deviceId = SimSettings.CLOUD_DATACENTER_ID;

        // Priority: critical tasks go to edge (low latency)
        if (task.getCriticality() == 1) {
            deviceId = SimSettings.GENERIC_EDGE_DEVICE_ID;
        }
        // Deadline-based: tight deadlines prefer edge
        else if (task.getDeadline() <= DEADLINE_THRESHOLD) {
            deviceId = SimSettings.GENERIC_EDGE_DEVICE_ID;
        }
        // Otherwise go to cloud (fallback)
        else {
            deviceId = SimSettings.CLOUD_DATACENTER_ID;
        }

        // Debug log: show decision and task attributes
        System.out.println("Strategy used for task -> Length: " + length +
                ", Input: " + inputSize +
                ", Output: " + outputSize);
        System.out.println("Decision: criticality=" + task.getCriticality()
                + ", deadline=" + task.getDeadline()
                + " -> deviceId=" + deviceId
                + (deviceId == SimSettings.GENERIC_EDGE_DEVICE_ID ? " (EDGE)" : " (CLOUD)"));

        return deviceId;
    }

    @Override
    public Vm getVmToOffload(Task task, int deviceId) {
        Vm selectedVM = null;

        if (deviceId == SimSettings.CLOUD_DATACENTER_ID) {
            // Select VM on cloud devices via Least Loaded algorithm!
            double selectedVmCapacity = 0; // start with min value
            List<Host> list = SimManager.getInstance().getCloudServerManager().getDatacenter().getHostList();
            for (int hostIndex = 0; hostIndex < list.size(); hostIndex++) {
                List<CloudVM> vmArray = SimManager.getInstance().getCloudServerManager().getVmList(hostIndex);
                for (int vmIndex = 0; vmIndex < vmArray.size(); vmIndex++) {
                    double requiredCapacity = ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu())
                            .predictUtilization(vmArray.get(vmIndex).getVmType());
                    double targetVmCapacity = (double) 100 - vmArray.get(vmIndex).getCloudletScheduler()
                            .getTotalUtilizationOfCpu(CloudSim.clock());
                    if (requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity) {
                        selectedVM = vmArray.get(vmIndex);
                        selectedVmCapacity = targetVmCapacity;
                    }
                }
            }
        } else if (simScenario.equals("TWO_TIER_WITH_EO"))
            selectedVM = selectVmOnLoadBalancer(task);
        else
            selectedVM = selectVmOnHost(task);

        return selectedVM;
    }

    public EdgeVM selectVmOnHost(Task task) {
        EdgeVM selectedVM = null;

        Location deviceLocation = SimManager.getInstance().getMobilityModel()
                .getLocation(task.getMobileDeviceId(), CloudSim.clock());
        // in the scenario, serving wlan ID is equal to the host id
        int relatedHostId = deviceLocation.getServingWlanId();
        List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);

        if (policy.equalsIgnoreCase("RANDOM_FIT")) {
            int randomIndex = SimUtils.getRandomNumber(0, vmArray.size() - 1);
            double requiredCapacity = ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu())
                    .predictUtilization(vmArray.get(randomIndex).getVmType());
            double targetVmCapacity = (double) 100 - vmArray.get(randomIndex).getCloudletScheduler()
                    .getTotalUtilizationOfCpu(CloudSim.clock());
            if (requiredCapacity <= targetVmCapacity)
                selectedVM = vmArray.get(randomIndex);
        } else if (policy.equalsIgnoreCase("WORST_FIT")) {
            double selectedVmCapacity = 0; // start with min value
            for (int vmIndex = 0; vmIndex < vmArray.size(); vmIndex++) {
                double requiredCapacity = ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu())
                        .predictUtilization(vmArray.get(vmIndex).getVmType());
                double targetVmCapacity = (double) 100 - vmArray.get(vmIndex).getCloudletScheduler()
                        .getTotalUtilizationOfCpu(CloudSim.clock());
                if (requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity) {
                    selectedVM = vmArray.get(vmIndex);
                    selectedVmCapacity = targetVmCapacity;
                }
            }
        } else if (policy.equalsIgnoreCase("BEST_FIT")) {
            double selectedVmCapacity = 101; // start with max value
            for (int vmIndex = 0; vmIndex < vmArray.size(); vmIndex++) {
                double requiredCapacity = ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu())
                        .predictUtilization(vmArray.get(vmIndex).getVmType());
                double targetVmCapacity = (double) 100 - vmArray.get(vmIndex).getCloudletScheduler()
                        .getTotalUtilizationOfCpu(CloudSim.clock());
                if (requiredCapacity <= targetVmCapacity && targetVmCapacity < selectedVmCapacity) {
                    selectedVM = vmArray.get(vmIndex);
                    selectedVmCapacity = targetVmCapacity;
                }
            }
        } else if (policy.equalsIgnoreCase("FIRST_FIT")) {
            for (int vmIndex = 0; vmIndex < vmArray.size(); vmIndex++) {
                double requiredCapacity = ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu())
                        .predictUtilization(vmArray.get(vmIndex).getVmType());
                double targetVmCapacity = (double) 100 - vmArray.get(vmIndex).getCloudletScheduler()
                        .getTotalUtilizationOfCpu(CloudSim.clock());
                if (requiredCapacity <= targetVmCapacity) {
                    selectedVM = vmArray.get(vmIndex);
                    break;
                }
            }
        } else if (policy.equalsIgnoreCase("NEXT_FIT")) {
            int tries = 0;
            while (tries < vmArray.size()) {
                lastSelectedVmIndexes[relatedHostId] = (lastSelectedVmIndexes[relatedHostId] + 1) % vmArray.size();
                double requiredCapacity = ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu())
                        .predictUtilization(vmArray.get(lastSelectedVmIndexes[relatedHostId]).getVmType());
                double targetVmCapacity = (double) 100 - vmArray
                        .get(lastSelectedVmIndexes[relatedHostId]).getCloudletScheduler()
                        .getTotalUtilizationOfCpu(CloudSim.clock());
                if (requiredCapacity <= targetVmCapacity) {
                    selectedVM = vmArray.get(lastSelectedVmIndexes[relatedHostId]);
                    break;
                }
                tries++;
            }
        }

        return selectedVM;
    }

    public EdgeVM selectVmOnLoadBalancer(Task task) {
        EdgeVM selectedVM = null;

        if (policy.equalsIgnoreCase("RANDOM_FIT")) {
            int randomHostIndex = SimUtils.getRandomNumber(0, numberOfHost - 1);
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(randomHostIndex);
            int randomIndex = SimUtils.getRandomNumber(0, vmArray.size() - 1);

            double requiredCapacity = ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu())
                    .predictUtilization(vmArray.get(randomIndex).getVmType());
            double targetVmCapacity = (double) 100 - vmArray.get(randomIndex).getCloudletScheduler()
                    .getTotalUtilizationOfCpu(CloudSim.clock());
            if (requiredCapacity <= targetVmCapacity)
                selectedVM = vmArray.get(randomIndex);
        } else if (policy.equalsIgnoreCase("WORST_FIT")) {
            double selectedVmCapacity = 0; // start with min value
            for (int hostIndex = 0; hostIndex < numberOfHost; hostIndex++) {
                List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
                for (int vmIndex = 0; vmIndex < vmArray.size(); vmIndex++) {
                    double requiredCapacity = ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu())
                            .predictUtilization(vmArray.get(vmIndex).getVmType());
                    double targetVmCapacity = (double) 100 - vmArray.get(vmIndex).getCloudletScheduler()
                            .getTotalUtilizationOfCpu(CloudSim.clock());
                    if (requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity) {
                        selectedVM = vmArray.get(vmIndex);
                        selectedVmCapacity = targetVmCapacity;
                    }
                }
            }
        } else if (policy.equalsIgnoreCase("BEST_FIT")) {
            double selectedVmCapacity = 101; // start with max value
            for (int hostIndex = 0; hostIndex < numberOfHost; hostIndex++) {
                List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
                for (int vmIndex = 0; vmIndex < vmArray.size(); vmIndex++) {
                    double requiredCapacity = ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu())
                            .predictUtilization(vmArray.get(vmIndex).getVmType());
                    double targetVmCapacity = (double) 100 - vmArray.get(vmIndex).getCloudletScheduler()
                            .getTotalUtilizationOfCpu(CloudSim.clock());
                    if (requiredCapacity <= targetVmCapacity && targetVmCapacity < selectedVmCapacity) {
                        selectedVM = vmArray.get(vmIndex);
                        selectedVmCapacity = targetVmCapacity;
                    }
                }
            }
        } else if (policy.equalsIgnoreCase("FIRST_FIT")) {
            for (int hostIndex = 0; hostIndex < numberOfHost; hostIndex++) {
                List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
                for (int vmIndex = 0; vmIndex < vmArray.size(); vmIndex++) {
                    double requiredCapacity = ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu())
                            .predictUtilization(vmArray.get(vmIndex).getVmType());
                    double targetVmCapacity = (double) 100 - vmArray.get(vmIndex).getCloudletScheduler()
                            .getTotalUtilizationOfCpu(CloudSim.clock());
                    if (requiredCapacity <= targetVmCapacity) {
                        selectedVM = vmArray.get(vmIndex);
                        break;
                    }
                }
            }
        } else if (policy.equalsIgnoreCase("NEXT_FIT")) {
            int hostCheckCounter = 0;
            while (selectedVM == null && hostCheckCounter < numberOfHost) {
                int tries = 0;
                lastSelectedHostIndex = (lastSelectedHostIndex + 1) % numberOfHost;

                List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(lastSelectedHostIndex);
                while (tries < vmArray.size()) {
                    lastSelectedVmIndexes[lastSelectedHostIndex] = (lastSelectedVmIndexes[lastSelectedHostIndex] + 1)
                            % vmArray.size();
                    double requiredCapacity = ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu())
                            .predictUtilization(vmArray.get(lastSelectedVmIndexes[lastSelectedHostIndex]).getVmType());
                    double targetVmCapacity = (double) 100 - vmArray
                            .get(lastSelectedVmIndexes[lastSelectedHostIndex]).getCloudletScheduler()
                            .getTotalUtilizationOfCpu(CloudSim.clock());
                    if (requiredCapacity <= targetVmCapacity) {
                        selectedVM = vmArray.get(lastSelectedVmIndexes[lastSelectedHostIndex]);
                        break;
                    }
                    tries++;
                }
                hostCheckCounter++;
            }
        }

        return selectedVM;
    }

    @Override
    public void processEvent(SimEvent arg0) {
        
    }

    @Override
    public void shutdownEntity() {
        
    }

    @Override
    public void startEntity() {
        
    }
}

