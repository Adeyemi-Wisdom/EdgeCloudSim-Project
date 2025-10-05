package edu.boun.edgecloudsim.utils;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import edu.boun.edgecloudsim.core.SimSettings;

public class TaskProperty {
    private double startTime;
    private long length, inputFileSize, outputFileSize;
    private int taskType;
    private int pesNumber;
    private int mobileDeviceId;

    // === New fields for LSCT strategy ===
    private double deadline;     // in seconds (task completion requirement)
    private int criticality;     // e.g., 0 = normal, 1 = critical
    private int priority;        // relative importance (higher = more urgent)

    // Constructor with full parameters (no random assignment)
    public TaskProperty(double _startTime, int _mobileDeviceId, int _taskType, int _pesNumber, long _length, long _inputFileSize, long _outputFileSize) {
        startTime = _startTime;
        mobileDeviceId = _mobileDeviceId;
        taskType = _taskType;
        pesNumber = _pesNumber;
        length = _length;
        inputFileSize = _inputFileSize;
        outputFileSize = _outputFileSize;
        priority = 0;
    }

    // Constructor used in IdleActiveLoadGenerator (no random assignment)
    public TaskProperty(int _mobileDeviceId, int _taskType, double _startTime, ExponentialDistribution[][] expRngList) {
        mobileDeviceId = _mobileDeviceId;
        startTime = _startTime;
        taskType = _taskType;

        inputFileSize = (long)expRngList[_taskType][0].sample();
        outputFileSize = (long)expRngList[_taskType][1].sample();
        length = (long)expRngList[_taskType][2].sample();
        pesNumber = (int)SimSettings.getInstance().getTaskLookUpTable()[_taskType][8];

        priority = 0;
    }

    // Constructor with ExponentialDistribution array (optional)
    public TaskProperty(int mobileDeviceId, double startTime, ExponentialDistribution[] expRngList) {
        this.mobileDeviceId = mobileDeviceId;
        this.startTime = startTime;
        taskType = 0;
        inputFileSize = (long)expRngList[0].sample();
        outputFileSize = (long)expRngList[1].sample();
        length = (long) expRngList[2].sample();
        pesNumber = (int)SimSettings.getInstance().getTaskLookUpTable()[0][8];
        priority = 0;
    }

    // === Getters ===
    public double getStartTime(){ return startTime; }
    public long getLength(){ return length; }
    public long getInputFileSize(){ return inputFileSize; }
    public long getOutputFileSize(){ return outputFileSize; }
    public int getTaskType(){ return taskType; }
    public int getPesNumber(){ return pesNumber; }
    public int getMobileDeviceId(){ return mobileDeviceId; }
    public double getDeadline() { return deadline; }
    public int getCriticality() { return criticality; }
    public int getPriority() { return priority; }

    // === Setters ===
    public void setDeadline(double deadline) { this.deadline = deadline; }
    public void setCriticality(int criticality) { this.criticality = criticality; }
    public void setPriority(int priority) { this.priority = priority; }
}

