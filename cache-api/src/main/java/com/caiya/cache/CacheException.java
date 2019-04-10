package com.caiya.cache;

/**
 * Cache Exception.
 */
public class CacheException extends RuntimeException {

    private static final long serialVersionUID = -6867165720650881604L;

    public CacheException(String msg) {
        super(msg);
    }

    public CacheException(Exception e) {
        super(e);
    }

    public CacheException(String msg, Exception e) {
        super(msg, e);
    }
}
