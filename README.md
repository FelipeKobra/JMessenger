<!--suppress HtmlDeprecatedAttribute -->
<p align="center"><img src="src/main/resources/images/command.png" alt="command"></p>
<h1 align="center">JMessenger</h1>
<p align="center">JMessenger is a terminal-based chat application that allows you to communicate seamlessly with
others over the internet. It supports both client and server functionalities, making it easy to set
up and use. Additionally, the communication is encrypted, meaning that messages are sent in a secure
and protected manner, ensuring the privacy and confidentiality of conversations.</p>

## Table of Contents

* [User Guide](#user-guide)
    + [Installation](#installation)
    + [Usage](#usage)
    + [UPnP](#upnp)
* [Developer Guide](#developer-guide)
    + [Requirements](#requirements)
    + [Building](#building)
    + [Building Natives](#building-natives)
    + [Contributing](#contributing)
* [License](#license)

## User Guide

### Installation

**Note:** This installation step is only necessary if you want to run the JAR file instead of the
native executable.
To run the JAR file, you need to have Java 21 or higher installed on your system. Here are the steps
to install Java 21 on different operating systems:

* Windows:
    + Download the Java 21 installer from the official Oracle website.
    + Run the installer and follow the installation instructions.
* macOS:
    + Download the Java 21 installer from the official Oracle website.
    + Run the installer and follow the installation instructions.
* Linux:
    + Open a terminal and run the command sudo apt-get install openjdk-21-jdk (for Ubuntu-based
      systems) or sudo yum install java-21-openjdk (for RPM-based systems).
    + Follow the installation instructions.

### Usage

#### Windows Executable

Download the executable (.exe) from
the [Releases](https://github.com/FelipeKobra/JavaTerminalChat/releases) section. Use the following
commands in your console:

* For the Chat Server:

```bash
./Server.exe
```

* For the Chat Client:

```bash
./Client.exe
```

#### JAR

Download the JAR (.jar) from
the [Releases](https://github.com/FelipeKobra/JavaTerminalChat/releases) section. Use the following
commands in your console:

* For the Chat Server:

```bash
java -jar Server.jar
```

* For the Chat Client:

```bash
java -jar Client.jar
```

### UPnP

**Universal Plug and Play (UPnP)** is a networking protocol that allows devices to automatically
discover and configure themselves on a network. It simplifies the discovery and management of
devices on the network, making it easy and secure to access and control them.

By default, UPnP is enabled in this application, eliminating the need to manually forward ports on
your router, as long as it is also enabled on your router. This feature enables the application to
automatically configure the router for seamless communication.

---

## Developer Guide

### Requirements

* JDK 21 or higher
* GraalVM JDK 21 or Higher
* Powershell

### Building

To build this project, run the following command in the root folder:

```bash
.\mvnw clean install
```

### Building Natives

#### Development

To create a fast build for development with low optimization, run the following command:

```bash
.\mvnw -Pnative-dev clean package
```

This will generate a native application with minimal optimization, suitable for development and
testing purposes.

#### Production

To create a slow build, but optimized application, run the following command:

```bash
.\mvnw -Pnative-prod clean package
```

### Contributing

If you would like to contribute to JMessenger, please fork the repository and submit a pull request.
For major changes, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the MIT License - see
the [LICENSE](https://github.com/FelipeKobra/JMessenger?tab=MIT-1-ov-file) file for details.