package edu.boun.edgecloudsim.task_generator;

import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class IdleActiveLoadGenerator extends LoadGeneratorModel {
    private int taskTypeOfDevices[];
    private Random rngRandom = new Random(); // single Random object

    public IdleActiveLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
        super(_numberOfMobileDevices, _simulationTime, _simScenario);
    }

    @Override
    public void initializeModel() {
        taskList = new ArrayList<>();

        ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance().getTaskLookUpTable().length][3];

        // Initialize ExponentialDistribution arrays for each task type
        for (int i = 0; i < SimSettings.getInstance().getTaskLookUpTable().length; i++) {
            if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0) continue;

            expRngList[i][0] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][5]); // Input size
            expRngList[i][1] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][6]); // Output size
            expRngList[i][2] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][7]); // Length
        }

        taskTypeOfDevices = new int[numberOfMobileDevices];

        for (int i = 0; i < numberOfMobileDevices; i++) {
            // Random task type selection
            int randomTaskType = -1;
            double taskTypeSelector = SimUtils.getRandomDoubleNumber(0, 100);
            double taskTypePercentage = 0;
            for (int j = 0; j < SimSettings.getInstance().getTaskLookUpTable().length; j++) {
                taskTypePercentage += SimSettings.getInstance().getTaskLookUpTable()[j][0];
                if (taskTypeSelector <= taskTypePercentage) {
                    randomTaskType = j;
                    break;
                }
            }
            if (randomTaskType == -1) {
                SimLogger.printLine("Impossible occurred! no random task type!");
                continue;
            }

            taskTypeOfDevices[i] = randomTaskType;

            double poissonMean = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][2];
            double activePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][3];
            double idlePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][4];
            double activePeriodStartTime = SimUtils.getRandomDoubleNumber(
                    SimSettings.CLIENT_ACTIVITY_START_TIME,
                    SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod
            );
            double virtualTime = activePeriodStartTime;

            ExponentialDistribution rng = new ExponentialDistribution(poissonMean);

            while (virtualTime < simulationTime) {
                double interval = rng.sample();
                if (interval <= 0) continue;

                virtualTime += interval;

                if (virtualTime > activePeriodStartTime + activePeriod) {
                    activePeriodStartTime += activePeriod + idlePeriod;
                    virtualTime = activePeriodStartTime;
                    continue;
                }

                // Create TaskProperty with random length/input/output
                TaskProperty taskProp = new TaskProperty(
                        virtualTime,
                        i,
                        randomTaskType,
                        (int) SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][8],
                        (long) expRngList[randomTaskType][2].sample(),
                        (long) expRngList[randomTaskType][0].sample(),
                        (long) expRngList[randomTaskType][1].sample()
                );

                //  Truly random deadline & criticality
                double deadline = 5.0 + rngRandom.nextDouble() * 10.0;
                taskProp.setDeadline(deadline);
                taskProp.setCriticality((deadline < 10.0) ? 1 : 0);

                // Log
                SimLogger.getInstance().addLog(
                        i,
                        taskList.size(),
                        randomTaskType,
                        (int) taskProp.getLength(),
                        (int) taskProp.getInputFileSize(),
                        (int) taskProp.getOutputFileSize()
                );

                taskList.add(taskProp);
            }
        }
    }

    @Override
    public int getTaskTypeOfDevice(int deviceId) {
        return taskTypeOfDevices[deviceId];
    }
}

