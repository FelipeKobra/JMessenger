package org.gladiator.server.admin;

import java.util.regex.Pattern;

public final class CommandUtils {

  public static final Pattern BAN_PATTERN =
      Pattern.compile("^/ban\\s+(\\S+)(?:\\s+(\\d+[smhdwSMHDW]*))?(?:\\s+(.+))?$");

  private CommandUtils() {
  }

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
