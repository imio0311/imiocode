package io.imiocode.team.backend;
public record ProcessResult(int exitCode,String output) { public boolean success(){return exitCode==0;} }
