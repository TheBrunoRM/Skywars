# Skywars

一个添加Skywars小游戏的Minecraft插件。
兼容Bukkit和Spigot，支持1.8及以上版本。

* [English](README_EN.md)

[![Codacy徽章](https://app.codacy.com/project/badge/Grade/786de08d9dfa4332bc1e15e8f4373bd6)](https://www.codacy.com/gh/TheBrunoRM/Skywars/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=TheBrunoRM/Skywars&amp;utm_campaign=Badge_Grade)
[![GitHub最新提交](https://img.shields.io/github/last-commit/TheBrunoRM/Skywars.svg)](https://github.com/TheBrunoRM/Skywars/commits/master)
[![版本](https://img.shields.io/github/release/TheBrunoRM/Skywars.svg?colorB=7418f5)](https://github.com/TheBrunoRM/Skywars/releases/latest)
[![在SpigotMC上查看](https://img.shields.io/badge/view%20on-spigotmc-orange.svg)](https://www.spigotmc.org/resources/98709)

# 目录
1. [如何安装](#如何安装)
2. [如何使用](#如何使用)
    * [如何玩游戏](#如何玩游戏)
    * [创建竞技场](#创建竞技场)
    * [创建告示牌](#创建告示牌)
3. [当前功能](#当前功能)
4. [待办事项](#待办事项)
5. [兼容性问题](#兼容性问题)

## 如何安装

要安装插件，请将.jar文件拖到服务器的**plugins**文件夹中。\
插件不需要任何其他依赖项，因为它应该可以独立工作。\
安装插件后，您可以启动或重新加载服务器。

## 如何使用

输入**/sw help**查看可用命令。\
将地图添加到"worlds"文件夹；插件将\
加载世界并为其地图创建配置文件。

要禁用默认大厅记分板，\
将**scoreboard.yml**文件中的**lobby**值设置为**false**。

如果插件没有设置**主大厅**，\
玩家将被传送到他们之前所在的**最后位置**。\
如果设置了，则他们将被传送到那里。

### 如何玩游戏

输入**/sw play**打开竞技场菜单并点击一个竞技场加入。\
输入**/sw start**开始游戏倒计时。\
输入**/sw forcestart**立即开始游戏。

### 创建竞技场

要创建竞技场，请输入**/sw create <arena>**\
创建竞技场后，输入**/sw config <arena>**打开配置菜单\
您可以在配置菜单上配置竞技场的大部分值，\
对于找不到的值，您可以参考竞技场的配置文件。\
确保选择一个世界文件夹。\
要加载世界文件夹，请将其放在插件文件夹内的**worlds**文件夹中\
您可以通过配置菜单设置竞技场的世界

### 创建告示牌

要创建告示牌，请这样做：
* 第2行：[SkyWars]
* 第3行：竞技场名称。

\* 告示牌的大小写不重要；不能有两个名称相同但大小写不同的竞技场。

![告示牌图片](https://cdn.discordapp.com/attachments/835594221456064544/876946375110189146/unknown.png)

要删除告示牌，在创造模式下潜行（shift）并左键单击。

## 插件挂钩
插件将挂钩到其他插件以添加额外功能。
### 经济系统
#### 当前经济功能：
* 硬币
#### 当前实现的经济插件API（[Skywars.java](https://github.com/TheBrunoRM/Skywars/blob/master/src/main/java/me/brunorm/skywars/Skywars.java#L250)）：
* Vault
### 全息显示
#### 当前全息功能：
* 指示补给时间的箱子上方全息图。

#### 当前实现的全息插件API（[HologramController.java](https://github.com/TheBrunoRM/Skywars/blob/master/src/main/java/me/brunorm/skywars/holograms/HologramController.java)）：
* HolographicDisplays
* DecentHolograms

## 当前功能
* 地图
* 套装
* 简单的地图设置菜单
* 自定义语言文件（不是100%的消息，仍在开发中）
* 配置选项
* 箱子补给
* 1.13+兼容（仍在开发中，但基本可用）
* Vault支持（经济系统）

## 待办事项
* 更多游戏事件
* 更多配置选项
* 队伍选项（？）
* 团队游戏
* 自定义箱子（结构图）
* 购买套装的能力（游戏内外）
* 更改箱子的能力（游戏内外）
* 游戏选项（时间、天气、箱子等）
* 代码优化
* 插件API事件和方法

## 兼容性问题
* **结构图文件**\
  插件不再支持结构图文件。\
  出于兼容性等各种原因，我决定放弃对这些类型文件的支持。\
  我可能会考虑为此实现一个新系统。\
  目前，插件支持世界文件。
* **Bukkit上的动作栏消息**\
  由于Bukkit在消息发送上的限制而无法工作。\
  如果您找到在Bukkit上发送动作栏消息的方法，请开启问题或拉取请求。

在Discord上联系原作者：brunorm
分支作者官网：https://cnmsb.xin