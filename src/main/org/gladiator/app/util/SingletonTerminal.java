package app.util;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public class SingletonTerminal implements AutoCloseable {
    public static final SingletonTerminal TERMINAL = new SingletonTerminal();
    public static final Logger LOGGER = LoggerFactory.getLogger(SingletonTerminal.class);

    private final Terminal terminal;
    private final LineReader lineReader;

    private SingletonTerminal() {
        Terminal tempTerminal = null;
        LineReader tempLineReader = null;
        try {
            tempTerminal = TerminalBuilder.terminal();
            tempLineReader = LineReaderBuilder.builder().terminal(tempTerminal).variable(LineReader.DISABLE_HISTORY, true).build();
        } catch (IOException e) {
            LOGGER.error("Error creating client terminal: {}", e.getMessage(), e);
        }

        Objects.requireNonNull(tempTerminal);
        tempTerminal.enterRawMode();
        tempTerminal.echo(true);
        this.terminal = tempTerminal;
        this.lineReader = tempLineReader;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public LineReader getLineReader() {
        return lineReader;
    }

    public void close() {
        try {
            terminal.close();
        } catch (IOException e) {
            LOGGER.error("Error closing terminal: {}", e.getMessage(), e);
        }
    }
}
