package org.gladiator.util.chat;

import java.io.IOException;
import java.util.Objects;
import org.gladiator.util.connection.message.ConnectionMessageType;
import org.gladiator.util.connection.message.model.Message;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for handling chat-related operations.
 */
public final class ChatUtils implements AutoCloseable {

  public static final String USER_INTERRUPT_MESSAGE = "User stopped the console reading,"
      + " probably by pressing CTRL + C";
  private static final Logger LOGGER = LoggerFactory.getLogger(ChatUtils.class);
  private final String userPrompt;
  private final Terminal terminal;
  private final LineReader lineReader;

  private ChatUtils(final String userPrompt, final Terminal terminal, final LineReader lineReader) {
    this.userPrompt = userPrompt;
    this.terminal = terminal;
    this.lineReader = lineReader;
  }

  /**
   * Creates a new {@link ChatUtils} instance with the specified user prompt. Note that there can
   * only be one instance of {@link Terminal} in the entire application. Therefore, if you create a
   * {@link ChatUtils} instance, you must close it before creating another one to avoid conflicts.
   *
   * @param userPrompt The prompt to display to the user.
   * @return A new {@link ChatUtils} instance.
   */
  public static ChatUtils create(final String userPrompt) {
    Terminal terminal = null;
    LineReader lineReader = null;
    try {
      terminal = TerminalBuilder.terminal();
      lineReader = LineReaderBuilder.builder().terminal(terminal)
          .variable(LineReader.DISABLE_HISTORY, true).build();
    } catch (final IOException e) {
      LOGGER.error("Error creating client terminal: {}", e.getMessage(), e);
    }

    Objects.requireNonNull(terminal);
    Objects.requireNonNull(lineReader);
    terminal.enterRawMode();
    terminal.echo(true);

    return new ChatUtils(userPrompt, terminal, lineReader);
  }

  /**
   * Displays a new chat received message on the screen.
   *
   * @param message The message to display.
   */
  public void showNewMessage(final Message message) {
    final String messageContent = message.toString();
    final ConnectionMessageType messageType = message.getType();

    cleanLine();

    if (ConnectionMessageType.SIMPLE != messageType) {
      displayBanner(messageContent);
    } else {
      displayOnScreen(messageContent);
    }

    showBufferedUserPrompt();
  }


  /**
   * Reads user input from the console using the default prompt.
   *
   * @return The user input.
   */
  public String getUserInput() throws UserInterruptException {
    return lineReader.readLine(userPrompt + " ");
  }

  /**
   * Reads user input from the console using the specified prompt.
   *
   * @param prompt The prompt to display to the user.
   * @return The user input.
   */
  public String getUserInput(final String prompt) {
    return lineReader.readLine(prompt);
  }

  /**
   * Displays a message on the screen. This method should be used as the default for displaying
   * messages on the console instead of using {@code System.out.println()}. It utilizes the
   * configured {@link Terminal} with custom properties and behaviors to ensure consistent output
   * formatting.
   *
   * @param msg The message to display.
   */
  public void displayOnScreen(final String msg) {
    terminal.writer().println(msg);
  }

  /**
   * Asks the user for an option with a default value.
   *
   * @param optionName    The name of the option.
   * @param defaultOption The default value for the option.
   * @return The user input.
   */
  public String askUserOption(final String optionName, final String defaultOption) {
    String userInput = getUserInput(
        "Type the " + optionName + " (" + defaultOption + "): ");

    if (userInput.isBlank()) {
      userInput = defaultOption;
    }

    return userInput;
  }

  /**
   * Asks the user for an option with a default value and a maximum option length. Note that the
   * maximum length is only displayed in the prompt and is not enforced programmatically.
   *
   * @param optionName      The name of the option.
   * @param defaultOption   The default value for the option.
   * @param maxOptionLength The maximum length of the option.
   * @return The user input.
   */
  public String askUserOption(final String optionName, final String defaultOption,
      final int maxOptionLength) {
    return askUserOption("Type the " + optionName + " [max size: " + maxOptionLength + "]",
        defaultOption);
  }


  /**
   * Prints a string with a decorative border.
   *
   * @param str The string to print.
   */
  public void displayBanner(final String str) {
    final String division = "=".repeat(str.length());

    displayOnScreen(System.lineSeparator() + division);
    displayOnScreen(str);
    displayOnScreen(division + System.lineSeparator());
  }

  /**
   * Displays the buffered user prompt on the screen.
   */
  private void showBufferedUserPrompt() {
    print(userPrompt + " " + lineReader.getBuffer().toString());
  }

  private void cleanLine() {
    print("\r\033[K");
  }

  private void print(final String str) {
    terminal.writer().print(str);
  }

  @Override
  public void close() {
    try {
      terminal.close();
    } catch (final IOException e) {
      LOGGER.error("Error closing terminal: {}", e.getMessage(), e);
    }
  }
}
