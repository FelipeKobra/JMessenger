# Changelog

## [v0.10.0](https://github.com/FelipeKobra/JMessenger/compare/v0.10.0...master) (2025-02-23)

### Breaking changes

-  add cryptography to messages ([6aaaf0a97b99843](https://github.com/FelipeKobra/JMessenger/commit/6aaaf0a97b99843))
-  add check for invalid messages on ConnectionMessageFactory ([8e5a5db8e855183](https://github.com/FelipeKobra/JMessenger/commit/8e5a5db8e855183))

### Features

-  add cryptography to messages ([6aaaf0a97b99843](https://github.com/FelipeKobra/JMessenger/commit/6aaaf0a97b99843))
-  check the message types on server side to not allow fake messages from clients ([7588f7c252e4082](https://github.com/FelipeKobra/JMessenger/commit/7588f7c252e4082))


## [v0.8.0](https://github.com/FelipeKobra/JMessenger/compare/v0.8.0...master) (2025-02-20)

### Breaking changes

-  remove not necessary method on ChatUtils.java (showNewMessage with String parameter) ([31e60aed5758c62](https://github.com/FelipeKobra/JMessenger/commit/31e60aed5758c62))
-  Notify other users when a new one connects ([e20f1b9e4b8df85](https://github.com/FelipeKobra/JMessenger/commit/e20f1b9e4b8df85))
-  change next_release_version.ps1 script name and place ([9cbdb6449ee0cee](https://github.com/FelipeKobra/JMessenger/commit/9cbdb6449ee0cee))

### Features

-  Notify other users when a user disconnects ([232f1e23dcddf54](https://github.com/FelipeKobra/JMessenger/commit/232f1e23dcddf54))
-  Notify other users when a new one connects ([e20f1b9e4b8df85](https://github.com/FelipeKobra/JMessenger/commit/e20f1b9e4b8df85))

### Bug Fixes

-  fix EI_EXPOSE_REP2 on Connection.java checking the nullity of some params ([307c4eb3b16b3cc](https://github.com/FelipeKobra/JMessenger/commit/307c4eb3b16b3cc))

## [v0.5.8](https://github.com/FelipeKobra/JMessenger/compare/v0.5.8...master) (2025-02-16)


### Features

-  create InputValidator ([793e3551889dd59](https://github.com/FelipeKobra/JMessenger/commit/793e3551889dd59))
-  add max user inputs max size ([946e139b6771b2c](https://github.com/FelipeKobra/JMessenger/commit/946e139b6771b2c))
-  add better and default options to ask for user inputs ([aafd8d7d6b44a31](https://github.com/FelipeKobra/JMessenger/commit/aafd8d7d6b44a31))

### Bug Fixes

-  blank messages now are not sent to the other users ([e419a381fb925d5](https://github.com/FelipeKobra/JMessenger/commit/e419a381fb925d5))
-  fix user input model on ChatUtils ([bb6e493f4a6f224](https://github.com/FelipeKobra/JMessenger/commit/bb6e493f4a6f224))
-  fix Regex error when non ascii character on client name ([a53054c175a9cdf](https://github.com/FelipeKobra/JMessenger/commit/a53054c175a9cdf))
-  fix UPnP port not opening ([56cf45f634eab89](https://github.com/FelipeKobra/JMessenger/commit/56cf45f634eab89))

## [v0.5.1](https://github.com/FelipeKobra/JMessenger/compare/v0.5.1...master) (2025-02-16)

### Breaking changes

-  remove not necessary method ([210e3aa7cfce651](https://github.com/FelipeKobra/JMessenger/commit/210e3aa7cfce651))


### Bug Fixes

-  fix client not connecting ([47569b48cbd9604](https://github.com/FelipeKobra/JMessenger/commit/47569b48cbd9604))
-  add better warning to user when server address not found ([d7629e1b0eddb21](https://github.com/FelipeKobra/JMessenger/commit/d7629e1b0eddb21))
-  More specific check on breaking changes ([f7e178fc644da16](https://github.com/FelipeKobra/JMessenger/commit/f7e178fc644da16))
-  Fix next release script to only check commits since last tag ([1bdd095dfce75be](https://github.com/FelipeKobra/JMessenger/commit/1bdd095dfce75be))

## [v0.4.0](https://github.com/FelipeKobra/JMessenger/compare/v0.4.0...master) (2025-02-14)

### Breaking changes

-  Remove not necessary PGO native images ([798d1ba1a4c2f16](https://github.com/FelipeKobra/JMessenger/commit/798d1ba1a4c2f16))

### Features

-  Add development parameter on next release script ([c4eea630d22429c](https://github.com/FelipeKobra/JMessenger/commit/c4eea630d22429c))

### Bug Fixes

-  Next release script now gives correct breaking change version ([d826a33e4229306](https://github.com/FelipeKobra/JMessenger/commit/d826a33e4229306))
-  Remove not necessary PGO native images ([798d1ba1a4c2f16](https://github.com/FelipeKobra/JMessenger/commit/798d1ba1a4c2f16))

## [v0.1.1](https://github.com/FelipeKobra/JMessenger/compare/v0.1.1...master) (2025-02-13)


### Features

-  Add next release version calculator ([6de5d148db4fef7](https://github.com/FelipeKobra/JMessenger/commit/6de5d148db4fef7))

### Bug Fixes

-  Correct version counting ([acf2bf35d2f692f](https://github.com/FelipeKobra/JMessenger/commit/acf2bf35d2f692f))
-  Change release rules ([f58cdb943a0e99c](https://github.com/FelipeKobra/JMessenger/commit/f58cdb943a0e99c))
-  Correct CHANGELOG links ([8dd54705af27857](https://github.com/FelipeKobra/JMessenger/commit/8dd54705af27857))
-  Assert nullable objects ([c0a17f679b24dac](https://github.com/FelipeKobra/JMessenger/commit/c0a17f679b24dac))
-  remove modifiable external dependencies ([ac47a45fa092c52](https://github.com/FelipeKobra/JMessenger/commit/ac47a45fa092c52))

## [v0.0.4](https://github.com/FelipeKobra/JMessenger/compare/v0.0.4...master) (2025-01-24)




## [v0.0.3](https://github.com/FelipeKobra/JMessenger/compare/v0.0.3...master) (2025-01-17)




## [v0.0.2](https://github.com/FelipeKobra/JMessenger/compare/v0.0.2...master) (2025-01-12)




## [v0.0.1](https://github.com/FelipeKobra/JMessenger/compare/v0.0.1...master) (2025-01-08)




