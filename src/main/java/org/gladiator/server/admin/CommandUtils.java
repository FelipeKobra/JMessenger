package org.gladiator.server.admin;

import java.util.regex.Pattern;

/**
 * Utility class for server administration commands.
 *
 * <p>Provides helpers for formatting available commands, parsing command strings and common
 * command-related patterns (for example {@link #BAN_PATTERN} for the `/ban` command). All members
 * are static; instantiation is prevented via a private constructor.
 */
public final class CommandUtils {

  public static final Pattern BAN_PATTERN =
      Pattern.compile("^/ban\\s+(\\S+)(?:\\s+(\\d+[smhdwSMHDW]*))?(?:\\s+(.+))?$");

  private CommandUtils() {
  }

  /**
   * Build a human-readable list of available server administration commands.
   *
   * <p>Iterates over all values of the {@code Command} enum and appends the formatted usage for
   * each command (via {@link #formatCommandUsage(Command)}), separating entries with a newline. The
   * resulting string starts with the header "Available Commands:" followed by a newline.
   *
   * @return a single string containing the header and one formatted usage line per command
   */
  public static String formatCommandList() {
    final StringBuilder builder = new StringBuilder("Available Commands:\n");
    for (final Command command : Command.values()) {
      builder.append(formatCommandUsage(command)).append("\n");
    }
    return builder.toString();
  }

  private static String formatCommandUsage(final Command command) {
    return command.getCommandString()
        + ": "
        + command.getDescription()
        + " | Usage: "
        + command.getInstruction();
  }

  /**
   * Resolve a {@link Command} from a raw command input string.
   *
   * <p>The input is expected to start with a leading slash (for example {@code "/ban user"}).
   * This method removes the leading {@code '/'} and then extracts the command token (the text up to
   * the first space, or the entire remaining string if there is no space). The extracted token is
   * compared case-insensitively against {@link Command#getCommandString()} for each enum value.
   *
   * <p>Examples:
   * - {@code "/kick alice"} -> checks {@code "kick"} - {@code "/list"} -> checks {@code "list"}
   *
   * @param commandString the raw command input (must start with {@code '/'} and not be
   *                      {@code null})
   * @return the matching {@link Command}, or {@code null} if no matching command is found
   * @throws NullPointerException      if {@code commandString} is {@code null}
   * @throws IndexOutOfBoundsException if {@code commandString} is empty
   */
  public static Command getCommandByString(final String commandString) {
    String commandWithoutBar = commandString.substring(1);
    commandWithoutBar =
        commandWithoutBar.contains(" ")
            ? commandWithoutBar.substring(0, commandWithoutBar.indexOf(" "))
            : commandWithoutBar;

    for (final Command command : Command.values()) {
      if (command.getCommandString().equalsIgnoreCase(commandWithoutBar)) {
        return command;
      }
    }
    return null;
  }
}
