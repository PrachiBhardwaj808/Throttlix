package com.throttlix.algorithm.tokenbucket;

public class TokenBucketState extends TokenBucket {

    public TokenBucketState(int capacity, int refillTokens, long refillDurationMillis, long initialTimeMillis) {
        super(capacity, refillTokens, refillDurationMillis, initialTimeMillis);
    }
}
