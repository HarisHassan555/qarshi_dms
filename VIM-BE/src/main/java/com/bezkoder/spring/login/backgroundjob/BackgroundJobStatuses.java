package com.bezkoder.spring.login.backgroundjob;

public final class BackgroundJobStatuses {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";
    public static final String CANCELLED = "CANCELLED";

    private BackgroundJobStatuses() {
    }
}
