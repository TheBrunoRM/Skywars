# Skywars

A Minecraft plugin that adds the Skywars minigame.
Compatible with Bukkit and Spigot, from versions 1.8 upwards.

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/786de08d9dfa4332bc1e15e8f4373bd6)](https://www.codacy.com/gh/TheBrunoRM/Skywars/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=TheBrunoRM/Skywars&amp;utm_campaign=Badge_Grade)
[![GitHub last commit](https://img.shields.io/github/last-commit/TheBrunoRM/Skywars.svg)](https://github.com/TheBrunoRM/Skywars/commits/master)
[![version](https://img.shields.io/github/release/TheBrunoRM/Skywars.svg?colorB=7418f5)](https://github.com/TheBrunoRM/Skywars/releases/latest)
[![view on SpigotMC](https://img.shields.io/badge/view%20on-spigotmc-orange.svg)](https://www.spigotmc.org/resources/98709)

# Table of Contents
1. [How to install](#how-to-install)
2. [How to use](#how-to-use)
   * [How to play](#how-to-play)
   * [Creating an arena](#creating-an-arena)
   * [Creating signs](#creating-signs)
3. [Current features](#current-features)
4. [To do](#to-do)
5. [Compatibility issues](#compatibility-issues)

## How to install

To install the plugin, drag the .jar file to your server's **plugins** folder.\
The plugin does not need any other dependencies as it should work by itself.\
You can start or reload the server after installing the plugin.

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
  * Maps
  * Kits
  * Easy map setup menu
  * Custom language file (not 100% of the messages, still working on it)
  * Configuration options
  * Chest refills
  * 1.13+ compatible (still working on it, but it kinda works)
  * Vault support (economy)

## To do
  * More game events
  * More configuration options
  * Party options (?)
  * Team games
  * Custom cases (schematic)
  * Ability to buy kits (inside and outside a game)
  * Ability to change case (inside and outside a game)
  * Game options (time, weather, chests, etc)
  * Code optimization
  * Plugin API events and methods

## Compatibility issues
  * **Schematic files**\
    The plugin currently works with both .schematic and .schem file types,\
    but there are some problems with some block types and metadata, like with chests.
    If you find any way to improve it, you're welcome to help!
  * **Actionbar messages on Bukkit**\
    do not work because of Bukkit limitations on message sending.\
    If you find a way to send an actionbar message on Bukkit, please open an issue or a pull request.

Contact me in Discord: BrunoRM#7316
