package main.chat;

import main.connection.DesignMessage;

public interface Chat {

    void showUserPrompt();

    void showConnectionMessage(DesignMessage message);

    String userPrompt();

    void showBufferedMessage(String bufferedMessage);
}
