# Skywars

For Minecraft servers from version 1.8 to 1.12 (I plan to add support to more versions in the future)

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
  - maps
  - kits
  - easy map setup menu

## To do
  - custom language file
  - lots of configuration options
  - game events (chest refills, etc)
  - party options (?)
  - team games
  - code optimization
  - 1.13+ compatible
  - coins (?)

## Something important about schematics and compatibility

The plugin works in 1.13+, but the schematics do not work.\
Schematic files from versions lower than 1.13 use block IDs\
Instead, schematic files from 1.13+ use block palettes with block names.\
I will have to remake the Schematic handler code (I will probably do at some point)\
but for now, the schematics do not work in 1.13+.
