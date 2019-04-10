package com.caiya.cache;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Expiration holds a value with its associated {@link TimeUnit}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 1.7
 */
public class Expiration {

    private long expirationTime;
    private TimeUnit timeUnit;

    /**
     * Creates new {@link Expiration}.
     *
     * @param expirationTime can be {@literal null}. Defaulted to {@link TimeUnit#SECONDS}
     * @param timeUnit       TimeUnit
     */
    protected Expiration(long expirationTime, TimeUnit timeUnit) {

        this.expirationTime = expirationTime;
        this.timeUnit = timeUnit != null ? timeUnit : TimeUnit.SECONDS;
    }

    /**
     * Get the expiration time converted into {@link TimeUnit#MILLISECONDS}.
     *
     * @return expiration time in millis
     */
    public long getExpirationTimeInMilliseconds() {
        return getConverted(TimeUnit.MILLISECONDS);
    }

    /**
     * Get the expiration time converted into {@link TimeUnit#SECONDS}.
     *
     * @return expiration time in seconds
     */
    public long getExpirationTimeInSeconds() {
        return getConverted(TimeUnit.SECONDS);
    }

    /**
     * Get the expiration time.
     *
     * @return expiration time
     */
    public long getExpirationTime() {
        return expirationTime;
    }

    /**
     * Get the time unit for the expiration time.
     *
     * @return TimeUnit
     */
    public TimeUnit getTimeUnit() {
        return this.timeUnit;
    }

    /**
     * Get the expiration time converted into the desired {@code targetTimeUnit}.
     *
     * @param targetTimeUnit must not {@literal null}.
     * @return convert result time
     * @throws IllegalArgumentException
     */
    public long getConverted(TimeUnit targetTimeUnit) {
        if (targetTimeUnit == null)
            throw new IllegalArgumentException("TargetTimeUnit must not be null!");

        return targetTimeUnit.convert(expirationTime, timeUnit);
    }

    /**
     * Creates new {@link Expiration} with {@link TimeUnit#SECONDS}.
     *
     * @param expirationTime expiration time
     * @return this object
     */
    public static Expiration seconds(long expirationTime) {
        return new Expiration(expirationTime, TimeUnit.SECONDS);
    }

    /**
     * Creates new {@link Expiration} with {@link TimeUnit#MILLISECONDS}.
     *
     * @param expirationTime expiration time
     * @return this object
     */
    public static Expiration milliseconds(long expirationTime) {
        return new Expiration(expirationTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates new {@link Expiration} with the provided {@link TimeUnit}. Greater units than {@link TimeUnit#SECONDS} are
     * converted to {@link TimeUnit#SECONDS}. Units smaller than {@link TimeUnit#MILLISECONDS} are converted to
     * {@link TimeUnit#MILLISECONDS} and can lose precision since {@link TimeUnit#MILLISECONDS} is the smallest granularity
     * supported by Redis.
     *
     * @param expirationTime expiration time
     * @param timeUnit       can be {@literal null}. Defaulted to {@link TimeUnit#SECONDS}
     * @return this object
     */
    public static Expiration from(long expirationTime, TimeUnit timeUnit) {

        if (Objects.equals(timeUnit, TimeUnit.MICROSECONDS)
                || Objects.equals(timeUnit, TimeUnit.NANOSECONDS)
                || Objects.equals(timeUnit, TimeUnit.MILLISECONDS)) {
            return new Expiration(timeUnit.toMillis(expirationTime), TimeUnit.MILLISECONDS);
        }

        if (timeUnit != null) {
            return new Expiration(timeUnit.toSeconds(expirationTime), TimeUnit.SECONDS);
        }

        return new Expiration(expirationTime, TimeUnit.SECONDS);
    }

    /**
     * Convert to redis command argument according to this TimeUnit
     *
     * @return redis command argument
     */
    public String toSetCommandExPxArgument() {
        if (Objects.equals((TimeUnit.SECONDS), this.timeUnit)) {
            return "ex";
        }
        if (Objects.equals((TimeUnit.MILLISECONDS), this.timeUnit)) {
            return "px";
        }

        throw new IllegalArgumentException("Unsupported TimeUnit");
    }

    /**
     * Creates new persistent {@link Expiration}.
     *
     * @return this object
     */
    public static Expiration persistent() {
        return new Expiration(-1, TimeUnit.SECONDS);
    }

    /**
     * @return {@literal true} if {@link Expiration} is set to persistent.
     */
    public boolean isPersistent() {
        return expirationTime == -1;
    }
}
