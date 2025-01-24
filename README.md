<!--suppress HtmlDeprecatedAttribute -->
<p align="center"><img src="src/main/resources/images/command.png" alt="command"></p>
<h1 align="center">JMessenger</h1>

**JMessenger** is a terminal-based chatUtils application that allows you to communicate seamlessly
with others over
the internet. It supports both client and server functionalities, making it easy to set up and use.

## Table of Contents

- [Notes](#notes)
- [Usage](#usage)
    - [Windows Executable](#windows-executable)
    - [JAR](#jar)
- [Developers](#developers)
    - [Requirements](#requirements)
- [Installation](#installation)
- [Building](#building)
    - [Project](#project)
    - [JAR Artifacts](#jar-artifacts)
- [Building Natives](#building-natives)
    - [Development](#development)
    - [Production](#production)
    - [PGO](#pgo-profile-guided-optimization)
- [Contributing](#contributing)
- [License](#license)

## Notes

- You don't need to forward ports on your router if UPnP (Universal Plug and Play) is enabled. This
  allows the
  application to automatically configure the router for communication.

## Usage

### Windows Executable

Download the executable (`.exe`) from
the [Releases](https://github.com/FelipeKobra/JavaTerminalChat/releases) section.
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

Download the JAR (`.jar`) from
the [Releases](https://github.com/FelipeKobra/JavaTerminalChat/releases) section. Use the
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

1. **ClientMain** (`app.client.ClientMain`): Handles client-side operations, allowing users to send
   and receive
   messages.
2. **ServerMain** (`app.server.ServerMain`): Manages server-side operations, redistributing messages
   to connected
   clients.

#### Example

1. Start the `ChatServer` by running the `ServerMain` class.
2. Open a new terminal and start a `ChatClient` instance. When prompted, enter the IP address of the
   computer running
   the server. If the server is running locally, you can leave it empty or enter `localhost`.

---

## Developers

### Requirements

- JDK 21 or higher
- GraalVM JDK 21 or Higher
- Powershell

## Installation

1. Open the project folder with your terminal.
2. Execute the following command to install dependencies:

```bash
   ./mvnw install
```

## Building

### Project

To build this project, run the following command in the root folder:

   ```bash
    .\mvnw clean install
   ```

### JAR Artifacts

To build the artifacts, run this command in the root folder:

   ```bash
    .\mvnw clean package
   ```

## Building Natives

### Development

To create a fast build for development with low optimization, run the following command:

   ```bash
    .\mvnw -Pnative-dev clean package
   ```

This will generate a native application with minimal optimization, suitable for development and
testing purposes.

### Production

To create a slow build, but optimized application, run the following command:

   ```bash
    .\mvnw -Pnative-prod clean package
   ```

### PGO (Profile-Guided Optimization)

To create PGO executables for even better performance on production, follow these steps:

**1 - Create the PGO Instruments**

   ```bash
    .\mvnw -pnative-pgo-build clean package
   ```

This will generate the PGO instruments in the `src/assets/pgo`
directory
and their respective folders.

**2 - Execute the PGO Instruments**

1. Run the PGO instruments.
2. Interact with the application to generate profiling data.
3. Close the application.

**3 - Create the production file with PGO**

Run the following command to create the production files with PGO:

   ```bash
    .\mvnw -pnative-prod-pgo clean package
   ```

## Contributing

If you would like to contribute to JMessenger, please fork the repository and submit a pull request.
For major changes,
please open an issue first to discuss what you would like to change.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
