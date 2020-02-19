package utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Validator {

    public static void checkIfNull(Object object, String reason) {
        if (object == null) {
            log.error(reason);
            throw new ConfigMissingException();
        }
    }

    public static void fail(String reason) {
        checkIfNull(null, reason);
    }

    private static class ConfigMissingException extends RuntimeException {
        public ConfigMissingException() {
            super("ConfigMissingException: Some config item is missing.");
        }
    }
}
