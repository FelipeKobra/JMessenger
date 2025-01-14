# Change Log

## [0.0.3](https://github.com/FelipeKobra/JMessenger/tree/v0.0.3) - To Release

### Enhancements

- Use GraalVM instead of Launch4j for creating Windows native images. Launch4j made necessary for the user to have JRE
- Added JLine in bundles for better compatibility with the native image build
- Change Deprecated JLine Jansi to JNI Version

### Bug Fixes

- Fixed showing terminal buffered messages a lot of times on closing the server
- No more Exceptions when exiting of the application via `CTRL + C`

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
