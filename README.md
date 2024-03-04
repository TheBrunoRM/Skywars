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
Add maps into the "worlds" folder; the plugin will\
load the worlds and create the configuration files for their maps.

To disable the default lobby scoreboard,\
set the value of **lobby** in the **scoreboard.yml** file to **false**.

If the plugin doesn't have a **main lobby** set,\
players will be teleported to the **last location** they were at.\
If it is set, then they will be teleported to it.

### How to play

Type **/sw play** to open the arenas menu and click an arena to join.\
Type **/sw start** to start the game countdown.\
Type **/sw forcestart** to start the game immediately.

### Creating an arena

To create an arena, type **/sw create <arena>**\
After creating the arena, type **/sw config <arena>** to open the configuration menu\
You can configure most of the values of the arena on the configuration menu,\
and for the ones you can't find, you can refer to the arena's configuration file.\
Make sure to select a world folder.\
To load an world folder, put it inside the **worlds** folder inside the plugin folder\
You can set the arena's world through the configuration menu

### Creating signs

To create a sign, make it like this:
* 2nd line: [SkyWars]
* 3rd line: the arena name.

\* The casing of the signs is not relevant; there cannot be two arenas with the same name but different casing.

![Sign image](https://cdn.discordapp.com/attachments/835594221456064544/876946375110189146/unknown.png)

To remove a sign, sneak (shift) while on creative mode and left click.

## Plugin hooks
The plugin will hook to other plugins to add extra functionality.
### Economy
#### Current economy features:
  * Coins
#### Currently implemented economy plugin APIS ([Skywars.java](https://github.com/TheBrunoRM/Skywars/blob/master/src/main/java/me/brunorm/skywars/Skywars.java#L250)):
  * Vault
### Holograms
#### Current hologram features:
  * Holograms above chests to indicate refill time.

#### Currently implemented hologram plugins APIs ([HologramController.java](https://github.com/TheBrunoRM/Skywars/blob/master/src/main/java/me/brunorm/skywars/holograms/HologramController.java)):
  * HolographicDisplays
  * DecentHolograms

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
    The plugin no longer works with schematic files.\
    I've decided to drop support for these type of files\
    for various reasons like compatibility.\
    I may consider implementing a new system for this.\
    For now, the plugin works with world files.
  * **Actionbar messages on Bukkit**\
    do not work because of Bukkit limitations on message sending.\
    If you find a way to send an actionbar message on Bukkit, please open an issue or a pull request.

Contact me in Discord: brunorm
