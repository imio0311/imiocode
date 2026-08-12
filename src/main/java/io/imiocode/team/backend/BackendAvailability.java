package io.imiocode.team.backend;
public record BackendAvailability(boolean available,String diagnostic) {
    public BackendAvailability { diagnostic=diagnostic==null?"":diagnostic; }
    public static BackendAvailability yes(){return new BackendAvailability(true,"");}
    public static BackendAvailability no(String reason){return new BackendAvailability(false,reason);}
}
