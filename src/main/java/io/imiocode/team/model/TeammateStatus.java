package io.imiocode.team.model;
public enum TeammateStatus {
    STARTING, RUNNING, IDLE, STOPPING, STOPPED, FAILED;
    public boolean active() { return this == STARTING || this == RUNNING || this == STOPPING; }
}
