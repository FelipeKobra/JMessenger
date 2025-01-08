# Installation
1. Open the project folder with your terminal
3. Execute `./mvnw install`

# Executing

## Executable
Run the executable (`.exe`) , that is on the [Releases](https://github.com/FelipeKobra/JavaTerminalChat/releases) section, on your console `./ChatClient` or `./CharServer`

## Source Code

### Explanation
There are two main classes:
1. ClientMain (`main.client.ClientMain`)
2. ServerMain (`main.server.ServerMain`)

First, you need to execute the `ServerMain`, so it can redistribute and send the messages to other clients, then you can execute the `ClientMain`, to communicate with others in the same chat

### Examples
``` bash
java -cp "C:\Users\{yourUser}\IdeaProjects\JavaConsoleChat\target\classes;C:\Users\{yourUser}\.m2\repository\org\apache\commons\commons-lang3\3.17.0\commons-lang3-3.17.0.jar;C:\Users\{yourUser}\.m2\repository\org\fusesource\jansi\jansi\2.4.1\jansi-2.4.1.jar;C:\Users\{yourUser}\.m2\repository\org\jline\jline\3.24.1\jline-3.24.1.jar" main.client.ClientMain
```

``` bash
java -cp "C:\Users\{yourUser}\IdeaProjects\JavaConsoleChat\target\classes;C:\Users\{yourUser}\.m2\repository\org\apache\commons\commons-lang3\3.17.0\commons-lang3-3.17.0.jar;C:\Users\{yourUser}\.m2\repository\org\fusesource\jansi\jansi\2.4.1\jansi-2.4.1.jar;C:\Users\{yourUser}\.m2\repository\org\jline\jline\3.24.1\jline-3.24.1.jar" main.server.ServerMain
```

# Using
1. Have a `ChatServer` running
2. Open a `ChatClient` and add the required IP of the computer running the server when prompted, if it is running locally, you can just leave it empty or enter `localhost`

# Requirements for Developers
JDK 21 or higher
