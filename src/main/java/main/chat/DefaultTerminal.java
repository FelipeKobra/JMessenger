package main.chat;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

public class DefaultTerminal implements CustomTerminal {
    public final Logger logger = Logger.getLogger(getClass().getName());
    private final Terminal terminal;
    private final LineReader lineReader;

    public DefaultTerminal() {
        Terminal tempTerminal = null;
        LineReader tempLineReader = null;
        try {
            tempTerminal = TerminalBuilder.terminal();
            tempLineReader = LineReaderBuilder.builder().terminal(tempTerminal).variable(LineReader.DISABLE_HISTORY, Boolean.TRUE).build();
        } catch (IOException e) {
            logger.warning("Error creating client terminal: " + e);
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

    @Override
    public void close() throws IOException {
        terminal.close();
    }
}
