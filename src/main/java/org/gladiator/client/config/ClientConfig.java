package org.gladiator.client.config;

/**
 * Configuration class for the client. This class holds the client's name, server address, and port
 * number.
 *
 * @param name          the name of the client
 * @param serverAddress the address of the server to connect to
 * @param port          the port number to connect to
 */
public record ClientConfig(String name, String serverAddress, int port) {

}
