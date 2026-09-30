package com.medroute.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NominatimRateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(NominatimRateLimiter.class);
    
    // Nominatim public API strict limit: max 1 request per second.
    private static final long MIN_DELAY_MS = 1000;
    
    private static final Object lock = new Object();
    private static long lastRequestTime = 0;

    /**
     * Blocks the current thread if necessary to ensure that at least
     * 1000ms have passed since the last request. Thread-safe.
     */
    public static void acquire() {
        synchronized (lock) {
            long now = System.currentTimeMillis();
            long timeSinceLast = now - lastRequestTime;
            
            if (timeSinceLast < MIN_DELAY_MS) {
                long sleepTime = MIN_DELAY_MS - timeSinceLast;
                try {
                    logger.debug("NominatimRateLimiter enforcing delay of {} ms", sleepTime);
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("NominatimRateLimiter interrupted", e);
                }
            }
            
            lastRequestTime = System.currentTimeMillis();
        }
    }
}
