package com.caiya.cache;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * The result wrapper of Redis Command: SCAN.
 * <p>
 *
 * @param <T> type of redis key
 * @author wangnan
 * @since 1.1
 */
public class ScanResult<T> {

    private static final String CHARSET = "UTF-8";

    private byte[] cursor;
    private List<T> results;

    public ScanResult(String cursor, List<T> results) {
        this(encode(cursor), results);
    }

    public ScanResult(byte[] cursor, List<T> results) {
        this.cursor = cursor;
        this.results = results;
    }

    public String getStringCursor() {
        return decode(cursor);
    }

    public byte[] getCursorAsBytes() {
        return cursor;
    }

    public List<T> getResult() {
        return results;
    }

    public static byte[] encode(final String str) {
        try {
            if (str == null) {
                throw new CacheException("value cannot be null");
            }
            return str.getBytes(CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new CacheException(e);
        }
    }

    public static String decode(final byte[] data) {
        try {
            return new String(data, CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new CacheException(e);
        }
    }
}
