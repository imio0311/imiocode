package io.imiocode.team.task;
public enum TeamTaskStatus { PENDING, RUNNING, BLOCKED, COMPLETED, FAILED, STOPPED;
    public boolean terminal(){return this==COMPLETED||this==FAILED||this==STOPPED;}
}
