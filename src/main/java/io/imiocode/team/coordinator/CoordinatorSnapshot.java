package io.imiocode.team.coordinator;
import io.imiocode.tool.ToolSelection;
public record CoordinatorSnapshot(boolean active,CoordinatorStage stage,ToolSelection selection) { }
