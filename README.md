# gChat [![Build Status](https://img.shields.io/github/workflow/status/N3FS/gChat/Deploy%20to%20Bintray)](https://github.com/N3FS/gChat/releases)
A simple global chat plugin for Velocity, based on [lucko/gChat](https://github.com/lucko/gChat).

* **Downloads** - <https://github.com/N3FS/gChat/releases>

## Features
* Global chat channel throughout your network
* Placeholders to customize chat formatting
* Multiple formats selected using weight
* API for adding additional placeholders, or listening to chat events

## Placeholders
### Standard Placeholders
These placeholders are provided by gChat.

* `{username}` or `{name}` - the players username
* `{server_name}` - the name of the players current server
* `{uuid}` - the players unique id
* `{display_name}` - the players display name
* `{message}` - the chat message being sent by the player.

### Plugin Hooks
gChat also provides hooks for the following plugins.

* [**LuckPerms**](https://github.com/lucko/LuckPerms) - gChat will hook with LuckPerms if it's installed on the proxy, and allows you to use any of the placeholders listed [here](https://github.com/lucko/LuckPerms/wiki/PlaceholderAPI).

## Configuration
```yaml
# gChat Configuration

# If a player doesn't have permission for any of the formats, should the chat message be passed through to the
# backend server or just ignored.
passthrough: true

# if permissions are required to send/receive chat messages
require-permission:
  # if true, players will need to have the "gchat.send" permission to send messages
  send: false

  # the message to send if a player doesn't have permission to chat
  # will only be sent if the 'passthrough' option below is false
  send-fail: "&cYou do not have permission to use the chat!"

  # if true, players will need to have the "gchat.receive" permission to receive messages
  receive: false

  # if a player doesn't have permission to send a message, should it be passed through to the backend server?
  passthrough: true

# Defines the formats to be used.
formats:

  # a default format for everyone on the server
  everyone:

    # the priority of the format. higher number = higher priority.
    # if a user has permission for multiple formats, the one with the highest priority will be used.
    priority: 100

    # if this format requires a permission.
    # permissions are in the format: "gchat.format.<format name>"
    # for example: "gchat.format.everyone"
    check-permission: true

    # the actual format for the chat message
    format: "{luckperms_prefix}{name}{luckperms_suffix}{message}"

    format-extra:

      # the format for the message tooltip
      hover: ""

      # what should happen when the message is clicked?
      click:

        # type: can be either "none", "suggest_command", "run_command" or "open_url"
        type: "none"

        # the value to suggest or run.
        value: ""

  # another format without all the comments!
  staff:
    priority: 150
    check-permission: true
    format: "* {luckperms_prefix}{name}{luckperms_suffix}&c: &b&l{message}"
    format-extra:
      hover: |-
        &e{name} is a staff member!

        &6Feel free to message them any time, by
        &6clicking this message!
      click:
        type: suggest_command
        value: "/msg {name} "
```

## Building

Clone the repository, ensure you have a recent JDK (8 or newer) installed, then run `./gradlew build` (or `./gradlew.bat build` on Windows).
