package com.throttlix.algorithm.slidingwindow;

import java.util.ArrayDeque;
import java.util.Deque;

public class SlidingWindowState {

    private final Deque<Long> requestTimestamps = new ArrayDeque<>();

    public synchronized void removeExpiredTimestamps(long currentTimeMillis, long windowDurationMillis) {
        long windowStart = currentTimeMillis - windowDurationMillis;
        while (!requestTimestamps.isEmpty() && requestTimestamps.peekFirst() <= windowStart) {
            requestTimestamps.removeFirst();
        }
    }

    public synchronized void addRequestTimestamp(long timestampMillis) {
        requestTimestamps.addLast(timestampMillis);
    }

    public synchronized int getRequestCount() {
        return requestTimestamps.size();
    }

    public synchronized Long getOldestRequestTimestamp() {
        return requestTimestamps.peekFirst();
    }

    public synchronized Long getLatestRequestTimestamp() {
        return requestTimestamps.peekLast();
    }
}
