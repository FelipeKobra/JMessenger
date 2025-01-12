package app.util;

import app.util.connection.ConnectionMessage;

public class Chat {
    private final String userPrompt;

    public Chat(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public void cleanLine() {
        System.out.print("\r\033[K");
    }

    public void prettyPrint(String str) {
        String division = "=".repeat(str.length());
        System.out.println("\n" + division);
        System.out.println(str);
        System.out.println(division + "\n");
    }

    public void showUserPrompt() {
        System.out.print(userPrompt + " ");
    }

    public void showBufferedMessage(String bufferedMessage) {
        showUserPrompt();
        System.out.print(bufferedMessage);
    }

    public void showConnectionMessage(final ConnectionMessage message) {
        cleanLine();
        System.out.println(message);
    }

    public String userPrompt() {
        return userPrompt;
    }

}
