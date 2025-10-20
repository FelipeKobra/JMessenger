package org.gladiator.server.admin;

/**
 * Represents administrative commands available on the server.
 *
 * <p>Each enum constant encapsulates:
 * <ul>
 *   <li>the command string used by administrators (e.g. {@code "kick"})</li>
 *   <li>a short human-readable description of the command</li>
 *   <li>usage instructions showing expected arguments</li>
 * </ul>
 *
 * <p>Supported commands: {@link #KICK}, {@link #BAN}, {@link #UNBAN},
 * {@link #QUIT}, {@link #HELP}, {@link #SHOW_USERS}, {@link #SHOW_BANS}.</p>
 */
public enum Command {
  KICK("kick", "Kick a user from the server", "/kick <username>"),
  BAN(
      "ban",
      "Ban a user from the server",
      "/ban <username> [duration e.g 1d2h30m (optional)] [reason (optional)]"),
  UNBAN("unban", "Unban a user from the server", "/unban <username>"),
  QUIT("quit", "Shut down the server", "/quit"),
  HELP("help", "Display this help message", "/help"),
  SHOW_USERS("show_users", "Show all connected users", "/show_users"),
  SHOW_BANS("show_bans", "Show all banned users", "/show_bans");

  private final String commandString;
  private final String description;
  private final String instruction;

  Command(final String commandString, final String description, final String instruction) {
    this.commandString = commandString;
    this.description = description;
    this.instruction = instruction;
  }

  public String getCommandString() {
    return commandString;
  }

  public String getDescription() {
    return description;
  }

  public String getInstruction() {
    return instruction;
  }
}
