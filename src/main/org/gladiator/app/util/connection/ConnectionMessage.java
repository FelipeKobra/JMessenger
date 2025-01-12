package app.util.connection;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record ConnectionMessage(String senderName, String content) {
    public ConnectionMessage {
        Validate.notBlank(senderName);
        Objects.requireNonNull(content);
    }

    public static ConnectionMessage fromRawString(String message) {
        Validate.notBlank(message);
        Validate.matchesPattern(message, "([\\w\\s]*),(.*)");
        final var split = StringUtils.split(message, ",", 2);
        return new ConnectionMessage(split[0], split[1]);
    }

    public static String toRawString(String senderName, String content) {
        return senderName + "," + content;
    }

    @Override
    public String toString() {
        return senderName + ": " + content;
    }

    public String toRawString() {
        return senderName + "," + content;
    }
}
