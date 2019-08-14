package com.caiya.cache;

/**
 * Options of Redis Command: SET.
 *
 * @author wangnan
 * @since 1.0
 */
public enum SetOption {

    /**
     * Do not set any additional command argument
     */
    UPSERT(null),

    /**
     * {@code NX}
     */
    SET_IF_ABSENT("nx"),

    /**
     * {@code XX}
     */
    SET_IF_PRESENT("xx");

    /**
     * redis set option name
     */
    private final String name;

    SetOption(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Do not set any additional command argument.
     *
     * @return NULL enum class
     */
    public static SetOption upsert() {
        return UPSERT;
    }

    /**
     * {@code XX}
     *
     * @return XX enum class
     */
    public static SetOption ifPresent() {
        return SET_IF_PRESENT;
    }

    /**
     * {@code NX}
     *
     * @return NX enum class
     */
    public static SetOption ifAbsent() {
        return SET_IF_ABSENT;
    }

}
