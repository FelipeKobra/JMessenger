package org.gladiator.util.chat;

import java.io.IOException;
import java.util.Locale;
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

/** Utility class for handling chat-related operations. */
public final class ChatUtils implements AutoCloseable {

  public static final String USER_INTERRUPT_MESSAGE =
      "User stopped the console reading," + " probably by pressing CTRL + C";
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
      lineReader =
          LineReaderBuilder.builder()
              .terminal(terminal)
              .variable(LineReader.DISABLE_HISTORY, true)
              .build();
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
    return lineReader.readLine(prompt).trim();
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
   * @param optionName The name of the option.
   * @param defaultOption The default value for the option.
   * @return The user input.
   */
  public String askUserOption(final String optionName, final String defaultOption) {
    String userInput = getUserInput("Type the " + optionName + " (" + defaultOption + "): ");

    if (userInput.isBlank()) {
      userInput = defaultOption;
    }

    return userInput;
  }

  /**
   * Asks the user for an option with a default value and a maximum option length. Note that the
   * maximum length is only displayed in the prompt and is not enforced programmatically.
   *
   * @param optionName The name of the option.
   * @param defaultOption The default value for the option.
   * @param maxOptionLength The maximum length of the option.
   * @return The user input.
   */
  public String askUserOption(
      final String optionName, final String defaultOption, final int maxOptionLength) {
    final String userOption =
        askUserOption(optionName + " [max size: " + maxOptionLength + "]", defaultOption);

    if (userOption.length() > maxOptionLength) {
      final String truncatedOption = userOption.substring(0, maxOptionLength);
      displayOnScreen(
          "Input has more than "
              + maxOptionLength
              + " characters and will be truncated to: \""
              + truncatedOption
              + "\"");
      return truncatedOption;
    }

    return userOption;
  }

  /**
   * Prompts the user for a binary (yes/no) option.
   *
   * <p>This method was created to get "yes or no" options from the user. It shows the provided
   * prompt along with a default-choice hint ("Y/n" when {@code defaultOption} is {@code true}, or
   * "y/N" when {@code false}). The user's input is read, converted to upper case using {@link
   * Locale#ROOT}, and considered affirmative only if it equals {@code "Y"}.
   *
   * @param optionPrompt the prompt message shown to the user
   * @param defaultOption the default boolean choice; {@code true} means default is "Y"
   * @return {@code true} if the user answered 'Y' (case-insensitive), {@code false} otherwise
   */
  public boolean askUserBinaryOption(final String optionPrompt, final boolean defaultOption) {
    final String defaultOptionTip;

    if (defaultOption) {
      defaultOptionTip = "Y/n";
    } else {
      defaultOptionTip = "y/N";
    }

    final String userInput = getUserInput(optionPrompt + " " + defaultOptionTip + ": ");
    final String upperUserInput = userInput.toUpperCase(Locale.ROOT);

    return "Y".equals(upperUserInput);
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

  /** Displays the buffered user prompt on the screen. */
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
