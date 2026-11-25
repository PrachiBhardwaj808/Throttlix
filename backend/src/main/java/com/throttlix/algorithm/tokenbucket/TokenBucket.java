package com.throttlix.algorithm.tokenbucket;

public class TokenBucket {

    private final int maxTokens;
    private final int refillTokens;
    private final long refillDurationMillis;
    private double currentTokens;
    private long lastRefillTime;

    public TokenBucket(int maxTokens, int refillTokens, long refillDurationMillis, long initialTimeMillis) {
        this.maxTokens = maxTokens;
        this.refillTokens = refillTokens;
        this.refillDurationMillis = refillDurationMillis;
        this.currentTokens = maxTokens;
        this.lastRefillTime = initialTimeMillis;
    }

    public synchronized void refill(long currentTimeMillis) {
        if (currentTimeMillis <= lastRefillTime) {
            return;
        }

        long timePassed = currentTimeMillis - lastRefillTime;
        double tokensToAdd = (timePassed * (double) refillTokens) / refillDurationMillis;

        if (tokensToAdd > 0) {
            currentTokens = Math.min((double) maxTokens, currentTokens + tokensToAdd);
            lastRefillTime = currentTimeMillis;
        }
    }

    public synchronized boolean tryConsume(long currentTimeMillis) {
        refill(currentTimeMillis);

        if (currentTokens >= 1.0) {
            currentTokens = currentTokens - 1.0;
            return true;
        }

        return false;
    }

    public synchronized double getAvailableTokens() {
        return currentTokens;
    }

    public synchronized double getAvailableTokens(long currentTimeMillis) {
        refill(currentTimeMillis);
        return currentTokens;
    }

    public synchronized long calculateRetryAfterMillis(long currentTimeMillis) {
        refill(currentTimeMillis);

        if (currentTokens >= 1.0) {
            return 0L;
        }

        double tokensNeeded = 1.0 - currentTokens;
        long timeNeeded = (long) Math.ceil((tokensNeeded * refillDurationMillis) / refillTokens);

        return Math.max(1L, timeNeeded);
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public int getCapacity() {
        return maxTokens;
    }

    public int getRefillTokens() {
        return refillTokens;
    }

    public long getRefillDurationMillis() {
        return refillDurationMillis;
    }

    public long getLastRefillTime() {
        return lastRefillTime;
    }

    public long getLastRefillTimeMillis() {
        return lastRefillTime;
    }
}
