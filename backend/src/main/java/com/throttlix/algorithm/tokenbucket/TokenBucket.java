package com.throttlix.algorithm.tokenbucket;

public class TokenBucket {

    private final int capacity;
    private final int refillTokens;
    private final long refillDurationMillis;
    private double availableTokens;
    private long lastRefillTimeMillis;

    public TokenBucket(int capacity, int refillTokens, long refillDurationMillis, long initialTimeMillis) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillDurationMillis = refillDurationMillis;
        this.availableTokens = capacity;
        this.lastRefillTimeMillis = initialTimeMillis;
    }

    public synchronized void refill(long currentTimeMillis) {
        if (currentTimeMillis <= lastRefillTimeMillis) {
            return;
        }

        long elapsedTime = currentTimeMillis - lastRefillTimeMillis;
        double tokensToAdd = (elapsedTime * (double) refillTokens) / refillDurationMillis;

        if (tokensToAdd > 0) {
            availableTokens = Math.min((double) capacity, availableTokens + tokensToAdd);
            lastRefillTimeMillis = currentTimeMillis;
        }
    }

    public synchronized boolean tryConsume(long currentTimeMillis) {
        refill(currentTimeMillis);

        if (availableTokens >= 1.0) {
            availableTokens = availableTokens - 1.0;
            return true;
        }

        return false;
    }

    public synchronized double getAvailableTokens(long currentTimeMillis) {
        refill(currentTimeMillis);
        return availableTokens;
    }

    public synchronized long calculateRetryAfterMillis(long currentTimeMillis) {
        refill(currentTimeMillis);

        if (availableTokens >= 1.0) {
            return 0L;
        }

        double tokensNeeded = 1.0 - availableTokens;
        long timeNeeded = (long) Math.ceil((tokensNeeded * refillDurationMillis) / refillTokens);

        return Math.max(1L, timeNeeded);
    }

    public int getCapacity() {
        return capacity;
    }

    public int getRefillTokens() {
        return refillTokens;
    }

    public long getRefillDurationMillis() {
        return refillDurationMillis;
    }
}
