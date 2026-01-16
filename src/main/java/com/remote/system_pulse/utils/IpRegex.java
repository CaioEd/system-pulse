package com.remote.system_pulse.utils;

public final class IpRegex { // "final" prevents the class from being extended

    // private constructor prevents instantiation
    private IpRegex() {
        throw new UnsupportedOperationException("Esta é uma classe de constantes e não deve ser instanciada.");
    }

    public static final String IP_PATTERN = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";
}