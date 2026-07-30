package io.imiocode.permission.sandbox;

import io.imiocode.permission.PermissionRequest;

import java.nio.file.Path;

/** 文件工具在调度前和写入前使用的工作区沙箱。 */
public interface PathSandbox {
    SandboxResult inspect(PermissionRequest request);

    Path revalidateWritable(Path target);
}
