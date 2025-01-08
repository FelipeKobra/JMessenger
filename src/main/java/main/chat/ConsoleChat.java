package main.chat;

import main.connection.DesignMessage;

public record ConsoleChat(String userPrompt) implements Chat {

    public static void cleanLine() {
        System.out.print("\r\033[K");
    }

    public void showUserPrompt() {
        System.out.print(userPrompt + " ");
    }

    public void showBufferedMessage(String bufferedMessage) {
        showUserPrompt();
        System.out.print(bufferedMessage);
    }

    public void showConnectionMessage(final DesignMessage message) {
        cleanLine();
        System.out.println(message);
    }


}
