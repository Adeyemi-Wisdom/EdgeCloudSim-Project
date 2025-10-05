package edu.boun.edgecloudsim.edge_client;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.core.SimSettings.NETWORK_DELAY_TYPES;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;

public class DefaultMobileDeviceManager extends MobileDeviceManager {
    private static final int BASE = 100000;
    private static final int REQUEST_RECEIVED_BY_CLOUD = BASE + 1;
    private static final int REQUEST_RECEIVED_BY_EDGE_DEVICE = BASE + 2;
    private static final int RESPONSE_RECEIVED_BY_MOBILE_DEVICE = BASE + 3;
    private int taskIdCounter = 0;

    public DefaultMobileDeviceManager() throws Exception {
    }

    @Override
    public void initialize() {
        // Nothing specific for now
    }

    @Override
    public UtilizationModel getCpuUtilizationModel() {
        return new CpuUtilizationModel_Custom();
    }

    protected void submitCloudlets() {
        // do nothing
    }

    protected void processCloudletReturn(SimEvent ev) {
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        Task task = (Task) ev.getData();

        SimLogger.getInstance().taskExecuted(task.getCloudletId());

        if (task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID) {
            double WanDelay = networkModel.getDownloadDelay(SimSettings.CLOUD_DATACENTER_ID, task.getMobileDeviceId(), task);
            if (WanDelay > 0) {
                Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(
                        task.getMobileDeviceId(), CloudSim.clock() + WanDelay);
                if (task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId()) {
                    networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
                    SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), WanDelay, NETWORK_DELAY_TYPES.WAN_DELAY);
                    schedule(getId(), WanDelay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
                } else {
                    SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
                }
            } else {
                SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), NETWORK_DELAY_TYPES.WAN_DELAY);
            }
        } else {
            double WlanDelay = networkModel.getDownloadDelay(task.getAssociatedHostId(), task.getMobileDeviceId(), task);
            if (WlanDelay > 0) {
                Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(
                        task.getMobileDeviceId(), CloudSim.clock() + WlanDelay);
                if (task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId()) {
                    networkModel.downloadStarted(currentLocation, SimSettings.GENERIC_EDGE_DEVICE_ID);
                    SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), WlanDelay, NETWORK_DELAY_TYPES.WLAN_DELAY);
                    schedule(getId(), WlanDelay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
                } else {
                    SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
                }
            } else {
                SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), NETWORK_DELAY_TYPES.WLAN_DELAY);
            }
        }
    }

    protected void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            SimLogger.printLine(getName() + ".processOtherEvent(): Error - event is null! Terminating simulation...");
            System.exit(1);
            return;
        }

        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();

        switch (ev.getTag()) {
            case REQUEST_RECEIVED_BY_CLOUD: {
                Task task = (Task) ev.getData();
                networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
                submitTaskToVm(task, 0, SimSettings.CLOUD_DATACENTER_ID);
                break;
            }
            case REQUEST_RECEIVED_BY_EDGE_DEVICE: {
                Task task = (Task) ev.getData();
                networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
                submitTaskToVm(task, 0, SimSettings.GENERIC_EDGE_DEVICE_ID);
                break;
            }
            case RESPONSE_RECEIVED_BY_MOBILE_DEVICE: {
                Task task = (Task) ev.getData();
                if (task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID)
                    networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
                else if (task.getAssociatedDatacenterId() != SimSettings.MOBILE_DATACENTER_ID)
                    networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
                SimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
                break;
            }
            default:
                SimLogger.printLine(getName() + ".processOtherEvent(): Error - unknown event! Terminating simulation...");
                System.exit(1);
        }
    }

    public void submitTask(TaskProperty edgeTask) {
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();

        // Create Task from TaskProperty
        Task task = createTask(edgeTask);

        Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(
                task.getMobileDeviceId(), CloudSim.clock());

        task.setSubmittedLocation(currentLocation);

        // Log task
        SimLogger.getInstance().addLog(
                task.getMobileDeviceId(),
                task.getCloudletId(),
                task.getTaskType(),
                (int) task.getCloudletLength(),
                (int) task.getCloudletFileSize(),
                (int) task.getCloudletOutputSize()
        );

        int nextHopId = SimManager.getInstance().getEdgeOrchestrator().getDeviceToOffload(task);
        System.out.println(
                "Task " + task.getCloudletId() +
                        " (criticality=" + edgeTask.getCriticality() +
                        ", deadline=" + edgeTask.getDeadline() +
                        ") assigned to device ID=" + nextHopId
        );

        if (nextHopId == SimSettings.CLOUD_DATACENTER_ID) {
            double WanDelay = networkModel.getUploadDelay(task.getMobileDeviceId(), nextHopId, task);
            if (WanDelay > 0) {
                networkModel.uploadStarted(currentLocation, nextHopId);
                SimLogger.getInstance().taskStarted(task.getCloudletId(), CloudSim.clock());
                SimLogger.getInstance().setUploadDelay(task.getCloudletId(), WanDelay, NETWORK_DELAY_TYPES.WAN_DELAY);
                schedule(getId(), WanDelay, REQUEST_RECEIVED_BY_CLOUD, task);
            } else {
                SimLogger.getInstance().rejectedDueToBandwidth(
                        task.getCloudletId(),
                        CloudSim.clock(),
                        SimSettings.VM_TYPES.CLOUD_VM.ordinal(),
                        NETWORK_DELAY_TYPES.WAN_DELAY);
            }
        } else if (nextHopId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
            double WlanDelay = networkModel.getUploadDelay(task.getMobileDeviceId(), nextHopId, task);
            if (WlanDelay > 0) {
                networkModel.uploadStarted(currentLocation, nextHopId);
                schedule(getId(), WlanDelay, REQUEST_RECEIVED_BY_EDGE_DEVICE, task);
                SimLogger.getInstance().taskStarted(task.getCloudletId(), CloudSim.clock());
                SimLogger.getInstance().setUploadDelay(task.getCloudletId(), WlanDelay, NETWORK_DELAY_TYPES.WLAN_DELAY);
            } else {
                SimLogger.getInstance().rejectedDueToBandwidth(
                        task.getCloudletId(),
                        CloudSim.clock(),
                        SimSettings.VM_TYPES.EDGE_VM.ordinal(),
                        NETWORK_DELAY_TYPES.WLAN_DELAY);
            }
        } else {
            SimLogger.printLine("Unknown nextHopId! Terminating simulation...");
            System.exit(1);
        }
    }

    private void submitTaskToVm(Task task, double delay, int datacenterId) {
        Vm selectedVM = SimManager.getInstance().getEdgeOrchestrator().getVmToOffload(task, datacenterId);

        int vmType = (datacenterId == SimSettings.CLOUD_DATACENTER_ID)
                ? SimSettings.VM_TYPES.CLOUD_VM.ordinal()
                : SimSettings.VM_TYPES.EDGE_VM.ordinal();

        if (selectedVM != null) {
            task.setAssociatedDatacenterId(
                    (datacenterId == SimSettings.CLOUD_DATACENTER_ID)
                            ? SimSettings.CLOUD_DATACENTER_ID
                            : selectedVM.getHost().getDatacenter().getId()
            );
            task.setAssociatedHostId(selectedVM.getHost().getId());
            task.setAssociatedVmId(selectedVM.getId());

            getCloudletList().add(task);
            bindCloudletToVm(task.getCloudletId(), selectedVM.getId());

            schedule(getVmsToDatacentersMap().get(task.getVmId()), delay, CloudSimTags.CLOUDLET_SUBMIT, task);

            SimLogger.getInstance().taskAssigned(
                    task.getCloudletId(),
                    selectedVM.getHost().getDatacenter().getId(),
                    selectedVM.getHost().getId(),
                    selectedVM.getId(),
                    vmType
            );
        } else {
            SimLogger.getInstance().rejectedDueToVMCapacity(task.getCloudletId(), CloudSim.clock(), vmType);
        }
    }

  private Task createTask(TaskProperty edgeTask) {
    UtilizationModel utilizationModelCPU = getCpuUtilizationModel();
    UtilizationModel utilizationModel = new UtilizationModelFull();

    Task task = new Task(
            edgeTask.getMobileDeviceId(),
            ++taskIdCounter,
            edgeTask.getLength(),
            edgeTask.getPesNumber(),
            edgeTask.getInputFileSize(),
            edgeTask.getOutputFileSize(),
            utilizationModelCPU,
            utilizationModel,
            utilizationModel
    );

    task.setUserId(this.getId());
    task.setTaskType(edgeTask.getTaskType());

    //  Copy criticality and deadline
    task.setCriticality(edgeTask.getCriticality());
    task.setDeadline(edgeTask.getDeadline());

    if (utilizationModelCPU instanceof CpuUtilizationModel_Custom) {
        ((CpuUtilizationModel_Custom) utilizationModelCPU).setTask(task);
    }

    return task;
  }

}

