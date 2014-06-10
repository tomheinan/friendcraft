FriendCraft
===========

Welcome to **FriendCraft**, a universal friends list plugin for Minecraft[^minecraft].

FriendCraft helps you keep track of your friends in Minecraft. You can add players to your friends list to see their status, get notifications when they join a server, and keep in touch with unified messaging. FriendCraft uses a powerful, real-time centralized network, so you can access your list - and your friends - from any server that has this plugin installed.

---

For Players
-----------

Getting started with FriendCraft is quick and easy! To check if it's available on your server, type `/fc`. If you get a message describing how to use the command, great! You're good to go. If not, ask your server administrator to install FriendCraft. It's a [Bukkit][1] plugin and it's available [here][2].

#### Managing Your Friends List

Use the `/fc ` command to manage your friends list:

- `/fc add <player>` adds a player to your list. Replace `<player>` with the name of the player you wish to add (capitalization doesn't matter).
- `/fc remove <player>` removes the given player from your list.
- `/fc list` prints the names of all the players currently on your list.
- `/fc info <player>` gives you detailed information about a particular player.

#### Settings

- `/fc sidebar <on/off>` turns the sidebar on or off. The sidebar will only display if you have added at least one friend.
- `/fc private <on/off>` turns private mode on or off. Private mode hides your status, so only the people on your friends list will know when you're online[^private mode 1][^private mode 2].

#### Messaging

You can message any player[^private mode 3] with the `/msg` command. If that player is online and playing on a FriendCraft-enabled server, they will receive your message immediately. Otherwise, the message will be queued and delivered the next time they sign on.

- `/msg <player> <message>` will send a message to the given player.
- `/r <message>` will send a message to the last person who messaged you.

For Server Admins
-----------------

#### System Requirements

TODO

#### Installation

TODO

#### Configuration

TODO

---

Legal
-----

TODO

  [^minecraft]: Minecraft ®/™ & © 2009-2014 Mojang / Notch

  [^private mode 1]: Private mode does not change your visibility on the local server - anyone logged in to the same server as you will still see you in the player list.

  [^private mode 2]: Private mode also prevents people you don't follow from sending you unsolicited messages, although if you message someone who isn't on your list, they will be able to reply directly to you with `/r`.

  [^private mode 3]: Players with private mode enabled will not receive messages from you unless you are in their friends list.

  [1]: http://bukkit.org/
  [2]: https://github.com/tomheinan/friendcraft/releases
