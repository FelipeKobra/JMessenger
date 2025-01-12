<p align="center"><img src="src/main/resources/images/command.png" alt="command"></p>
<h1 align="center">JMessenger</h1>

**JMessenger** is a terminal-based chat application that allows you to communicate seamlessly with others over a
network. It supports both client and server functionalities, making it easy to set up and use.

## Table of Contents

- [Notes](#notes)
- [Installation](#installation)
- [Usage](#usage)
    - [Windows Executable](#windows-executable)
    - [JAR](#jar)
- [Building](#building)
    - [Project](#project)
    - [Artifacts](#artifacts)
- [Requirements for Developers](#requirements-for-developers)
- [Contributing](#contributing)
- [License](#license)

## Notes

- You don't need to forward ports on your router if UPnP (Universal Plug and Play) is enabled. This allows the
  application to automatically configure the router for communication.

## Installation

1. Open the project folder with your terminal.
2. Execute the following command to install dependencies:

```bash
   ./mvnw install
```

## Usage

### Windows Executable

Download the executable (`.exe`) from the [Releases](https://github.com/FelipeKobra/JavaTerminalChat/releases) section.
Use the following commands in your console:

- For the Chat Server:
  ```bash
    ./Server.exe
  ```
- For the Chat Client:
    ```bash
    ./Client.exe
    ```

### JAR

Download the JAR (`.jar`) from the [Releases](https://github.com/FelipeKobra/JavaTerminalChat/releases) section. Use the
following commands in your console:

- For the Chat Server:
    ```bash
    java -jar Server.jar
    ```
- For the Chat Client:
    ```bash
  java -jar Client.jar
    ```

### Source Code

#### Explanation

There are two main application classes:

1. **ClientMain** (`app.client.ClientMain`): Handles client-side operations, allowing users to send and receive
   messages.
2. **ServerMain** (`app.server.ServerMain`): Manages server-side operations, redistributing messages to connected
   clients.

#### Example

1. Start the `ChatServer` by running the `ServerMain` class.
2. Open a new terminal and start a `ChatClient` instance. When prompted, enter the IP address of the computer running
   the server. If the server is running locally, you can leave it empty or enter `localhost`.

## Building

### Project

To build this project, run the following command in the root folder:

   ```bash
    .\mvnw clean install
   ```

### Artifacts

To build the artifacts, run this command in the root folder:

   ```bash
    .\mvnw clean package
   ```

## Requirements for Developers

- JDK 21 or higher

## Contributing

If you would like to contribute to JMessenger, please fork the repository and submit a pull request. For major changes,
please open an issue first to discuss what you would like to change.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
