package main.chat;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import java.io.IOException;

public interface CustomTerminal extends AutoCloseable {
    Terminal getTerminal();

    LineReader getLineReader();

    void close() throws IOException;
}
