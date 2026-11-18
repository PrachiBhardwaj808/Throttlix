package com.throttlix.algorithm.slidingwindow;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Stores the request timestamps that are still relevant for one client's
 * sliding window. Each client will have its own instance of this class.
 */
public class SlidingWindowState {

    // Requests are added in time order, so the oldest timestamp stays at the front.
    private final Deque<Long> requestTimestamps = new ArrayDeque<>();

    /**
     * Removes timestamps at or before the start of the active window.
     * The active window is (currentTimeMillis - windowDurationMillis, currentTimeMillis].
     */
    public synchronized void removeExpiredTimestamps(
            long currentTimeMillis,
            long windowDurationMillis) {
        long windowStartMillis = currentTimeMillis - windowDurationMillis;

        while (!requestTimestamps.isEmpty()
                && requestTimestamps.peekFirst() <= windowStartMillis) {
            requestTimestamps.removeFirst();
        }
    }

    /** Adds the timestamp of an accepted request. */
    public synchronized void addRequestTimestamp(long timestampMillis) {
        requestTimestamps.addLast(timestampMillis);
    }

    /** Returns how many request timestamps are currently stored. */
    public synchronized int getRequestCount() {
        return requestTimestamps.size();
    }

    /** Returns the oldest stored timestamp, or null when there are no requests. */
    public synchronized Long getOldestRequestTimestamp() {
        return requestTimestamps.peekFirst();
    }
}
