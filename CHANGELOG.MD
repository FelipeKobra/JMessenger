# Change Log

## [0.0.4](https://github.com/FelipeKobra/JMessenger/tree/v0.0.4) - To Release

### Enhancements

-- Commit 2 --

- Add support for native image profiles
- Add Profile-Guided Optimization (PGO) support for native images
- Modify PortMapper logic to require developers to close ports they open
- Change META-INF to the maven default place under resources

-- [Commit 1](https://github.com/FelipeKobra/JMessenger/commit/b48f120356808012cdbb39e72e248073aa213fd0) --

- More organized README for developers
- Unique instance for ChatUtils on client instead of creating one for each instance
- Remove Server PortMapper property and create it outside

## [0.0.3](https://github.com/FelipeKobra/JMessenger/tree/v0.0.3) - January 17, 2025

### Enhancements

-- [Commit 2](https://github.com/FelipeKobra/JMessenger/commit/0a6223b066b3e8e8074578609740c209ffda69ab) --

- Add UTF-8 support natively on terminal
- Code structure enhancements
- Use exception as cancellation mechanism
- Add more user-friendly warnings
- Add Google Code Style
- Add JavaDocs
- Faster resource closing on Server and Client classes

-- [Commit 1](https://github.com/FelipeKobra/JMessenger/commit/1260371d40a75ea7e6108d8e7c3683d85e746ee1) --

- Use GraalVM instead of Launch4j for creating Windows native images. Launch4j made necessary for
  the user to have JRE
- Added JLine in bundles for better compatibility with the native image build
- Change Deprecated JLine Jansi to JNI Version

### Bug Fixes

-- [Commit 1](https://github.com/FelipeKobra/JMessenger/commit/1260371d40a75ea7e6108d8e7c3683d85e746ee1) --

- Fixed showing terminal buffered messages a lot of times on closing the server
- No more Exceptions when exiting of the application via `CTRL + C`

-- [Commit 2](https://github.com/FelipeKobra/JMessenger/commit/0a6223b066b3e8e8074578609740c209ffda69ab) --

- Fix wrong maven folder structure

## [0.0.2](https://github.com/FelipeKobra/JMessenger/tree/v0.0.2) - January 12, 2025

### Enhancements

- Added `CHANGELOG.md` and `TODO.md` files for better project management.
- Corrected folder structures for improved organization.
- Updated server and client designs, including renaming for clarity.
- Implemented a more robust logging system for easier debugging.
- Reorganized default prompt options for better usability.
- Improved performance of reconnection attempts on the client side.
- Introduced a Singleton pattern for the Terminal instance to ensure a single point of access.
- **Added UPnP support** for automatic port forwarding.

## [0.0.1 - RELEASE](https://github.com/FelipeKobra/JMessenger/tree/v0.0.1) - January 7, 2025

- Created the initial repository.
