# Skywars

For Minecraft servers from version 1.8 to 1.12 (I plan to add support to more versions in the future)

[spigotmc-link]: https://www.spigotmc.org/resources/98709/
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/786de08d9dfa4332bc1e15e8f4373bd6)](https://www.codacy.com/gh/TheBrunoRM/Skywars/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=TheBrunoRM/Skywars&amp;utm_campaign=Badge_Grade)
[![GitHub last commit](https://img.shields.io/github/last-commit/TheBrunoRM/Skywars.svg)](https://github.com/TheBrunoRM/Skywars/commits/master)
[![version](https://img.shields.io/github/release/TheBrunoRM/Skywars.svg?colorB=7418f5)](https://github.com/TheBrunoRM/Skywars/releases/latest)
[![view on SpigotMC](https://img.shields.io/badge/view%20on-spigotmc-orange.svg)][spigotmc-link]

# Table of Contents
1. [How to install](#how-to-install)
2. [How to use](#how-to-use)
   * [How to play](#how-to-play)
   * [Creating an arena](#creating-an-arena)
   * [Creating signs](#creating-signs)
4. [Current features](#current-features)
5. [To do](#to-do)
6. [Compatibility issues](#compatibility-issues)

## How to install

To install the plugin, drag the .jar file to your server's **plugins** folder.\
The plugin does not need any other dependencies as it should work by itself.\
You can start the server after installing the plugin.

## How to use

Type **/sw help** to see the available commands.\
The plugin comes with a default arena and kit,\
so you should be able to play right after installing the plugin.

To disable the default lobby scoreboard,\
set the value of **lobby** in the **scoreboard.yml** file to **false**.

If the plugin doesn't have a **main lobby** set,\
players will be teleported to the last location they were at.\
If it is set, then they will be teleported to it.

### How to play

Type **/sw play** to open the arenas menu and click an arena to join.\
Type **/sw start** to start the game countdown.\
Type **/sw forcestart** to start the game immediately.

### Creating an arena

To create an arena, type **/sw create <arena>**\
After creating the arena, type **/sw config <arena>** to open the configuration menu\
You can configurate most of the values of the arena on the configuration menu\
Make sure to select an **schematic** file\
To load an schematic file, put it inside the **schematics** folder inside the plugin folder\
You can set the arena's schematic through the configuration menu

### Creating signs

To create a sign, make it like this:

![Sign image](https://cdn.discordapp.com/attachments/835594221456064544/876946375110189146/unknown.png)

## Current features
  - Maps
  - Kits
  - Easy map setup menu
  - Custom language file (not 100% of the messages, still working on it)
  - Configuration options
  - Chest refills

## To do
  - More game events
  - More configuration options
  - Party options (?)
  - Team games
  - Code optimization
  - 1.13+ compatible
  - Coins (?)

## Compatibility issues
  * **1.13+**\
    The plugin works in 1.13+, but the schematics do not work.\
    Schematic files from versions lower than 1.13 use block IDs\
    Instead, schematic files from 1.13+ use block palettes with block names.\
    I will have to remake the Schematic handler code (I will probably do at some point)\
    but for now, the schematics do not work in 1.13+.
