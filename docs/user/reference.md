# Reference

Every command, permission, placeholder and file of uxmSkyblock, read from the code and the shipped files.
Command words are the shipped ones; `commands.conf` renames, aliases or turns off any of them.

## Commands

`/island` (alias `/is`) with no word opens the island menu. `/is help [page]` lists the words a player may use.

| Command | What it does |
|---|---|
| `/is help [page]` | Lists the commands you can use |
| `/is menu` | Opens the island control panel |
| `/is create [preset]` | Creates your island |
| `/is home` | Takes you to your island |
| `/is nether` | Takes you to your nether island |
| `/is end` | Takes you to your end island |
| `/is visit <player> [confirm]` | Visits another player's island, paying its visit fee when you confirm |
| `/is setspawn` | Sets where your island begins |
| `/is sethome <name>` | Saves where you stand as a named home |
| `/is gohome <name>` | Goes to a named home |
| `/is homes [list]` | Opens your saved homes, or lists them in chat |
| `/is delhome <name>` | Deletes a named home |
| `/is vault [page]` | Opens the shared island vault |
| `/is activity` | Shows what happened on your island lately |
| `/is bank [deposit\|withdraw\|balance\|status\|paydebt]` | Manages the island bank and upkeep debt |
| `/is biome [list\|<type>]` | Opens the biomes, or changes the island biome |
| `/is top [level\|worth\|bank]` | Shows the leaderboards |
| `/is level [recalculate]` | Shows the island level and block worth |
| `/is worth` | Shows what the island is worth |
| `/is limits` | Shows block and entity limits |
| `/is quarantine` | Shows the island quarantine state |
| `/is booster` | Shows active island boosters and multipliers |
| `/is missions` | Opens the island missions |
| `/is course [season [number]]` | Shows the best times on the Parkour course you stand on, of all time or of a season: the one under way, or the one numbered |
| `/is plot <time\|weather> <setting> \| reset [confirm]` | Sets the time or weather of the Brix plot you stand on, or clears everything built on it and lays its ground again |
| `/is plots` | Lists every Brix plot, those in the contest first, and takes you to the one you choose |
| `/is fly` | Flies on an island you belong to, or comes down |
| `/is values` | Shows what each block adds to an island: its worth, its points and the levels they make |
| `/is visitfee [amount]` | Shows or sets what a visitor pays your island's bank to visit it, 0 for nothing |
| `/is contest [join \| vote <stars>]` | Shows the Brix build contest under way, enters the plot you stand on, or gives the plot you stand on your stars |
| `/is courses` | Lists every Parkour course, the most run first, and takes you to the one you choose |
| `/is coursetag <difficulty\|category> [name]` | Gives the Parkour course you stand on a difficulty or a category, or takes it off |
| `/is courserate <stars>` | Rates the Parkour course you stand on, once you have finished it |
| `/is lore` | Reads the lore of the sea on a Poseidon island |
| `/is trade <player>` | Asks a player to trade, or answers their question |
| `/is cargo` | Opens the cargo hold of the TradeWinds vessel you are aboard |
| `/is sail` | Chooses the port your TradeWinds vessel sails for next |
| `/is market` | Opens the market of the port your TradeWinds vessel lies in |
| `/is contracts` | Takes and delivers the contracts of the port your TradeWinds vessel lies in |
| `/is plunder` | Takes part of the hold of the TradeWinds vessel you stand by, at sea |
| `/is boxbuy` | Buys growth for your Boxed island's box |
| `/is boxside [side]` | Shows or chooses the side your box grows toward |
| `/is oneblock` | Shows where your OneBlock island stands |
| `/is chunks` | Shows how far your ChunkBlock island has opened |
| `/is border` | Shows, hides and colours the island border |
| `/is reset` | Resets and recycles your island |
| `/is delete` | Erases your island, the same as reset |
| `/is disband` | Gives up your island |
| `/is rename <name>` | Renames your island |
| `/is info` | Shows facts about your island |
| `/is invite <player>` | Invites a player to your island |
| `/is accept` | Accepts an island invite |
| `/is deny` | Turns an island invite down |
| `/is kick <player>` | Removes a member from your island |
| `/is leave` | Leaves the island you belong to |
| `/is members [list]` | Opens the island members, or lists them in chat |
| `/is role <player> <role>` | Gives a member a role |
| `/is permissions [role] [permission] [on\|off]` | Shows or changes what each role may do |
| `/is trust <player>` | Lets a visitor build on your island |
| `/is untrust <player>` | Takes that trust back |
| `/is trusted` | Lists the players you trust |
| `/is ban <player> [reason]` | Bans a player from your island |
| `/is unban <player>` | Lifts an island ban |
| `/is bans` | Lists the players banned from your island |
| `/is lock` | Closes your island to visitors |
| `/is unlock` | Opens your island to visitors |
| `/is flag [list\|<name> <amount>]` | Shows or sets island settings |
| `/is warp [create\|delete\|...]` | Manages your island warps |
| `/is warps [category]` | Browses the public warps |
| `/is warplock <name>` | Closes a warp to visitors |
| `/is warpunlock <name>` | Opens a warp to visitors |
| `/is guestbook [sign <message>]` | Reads or signs an island guestbook |
| `/is rate <score>` | Rates the island you are on |
| `/is bookmarks [toggle]` | Lists or saves island bookmarks |
| `/is alliance [invite\|accept\|break\|...]` | Manages island alliances |
| `/is allychat <message>` | Speaks to your allied islands |
| `/is chat [message]` | Toggles island chat or sends one message |
| `/is spy` | Toggles listening in on island chat |
| `/is rewards` | Claims the rewards waiting for you |
| `/is season [list\|rewards\|top]` | Shows the current season |
| `/is upgrades [list]` | Opens the island upgrades, or lists them in chat |
| `/is shop [list\|buy <item>]` | Opens the island shop |
| `/is profile switch <id>` | Switches your active profile |
| `/is reload` | Reloads the catalogues, the menus and the OneBlock phases |
| `/is doctor` | Checks the plugin's health |
| `/is schematic <pos1\|pos2\|save\|paste\|list>` | Saves and pastes structures |
| `/is restore <backup> [mode]` | Restores an island from a backup |
| `/is admin <freeze\|unfreeze\|inspect\|inactivity>` | Staff tools for islands |

### Operator commands

`/isadmin` holds the verbs of the game modes, each behind its own node:

| Command | Permission | What it does |
|---|---|---|
| `/isadmin oneblock setphase` | `uxmskyblock.admin.oneblock.setphase` | Move a OneBlock island to a phase |
| `/isadmin oneblock setcount` | `uxmskyblock.admin.oneblock.setcount` | Set a OneBlock island's count of breaks |
| `/isadmin oneblock addcount` | `uxmskyblock.admin.oneblock.addcount` | Add to or take from a OneBlock island's count of breaks |
| `/isadmin oneblock check` | `uxmskyblock.admin.oneblock.check` | Read where a OneBlock island stands |
| `/isadmin oneblock respawn` | `uxmskyblock.admin.oneblock.respawn` | Lay a OneBlock island's block again |
| `/isadmin chunkblock open` | `uxmskyblock.admin.chunkblock.open` | Open a chunk of a ChunkBlock island |
| `/isadmin chunkblock close` | `uxmskyblock.admin.chunkblock.close` | Close a chunk of a ChunkBlock island |
| `/isadmin chunkblock reset` | `uxmskyblock.admin.chunkblock.reset` | Close every chunk of a ChunkBlock island but the first |
| `/isadmin tradewinds setrank` | `uxmskyblock.admin.tradewinds.setrank` | Give a TradeWinds vessel a rank |
| `/isadmin tradewinds setstanding` | `uxmskyblock.admin.tradewinds.setstanding` | Set a TradeWinds vessel's standing in a port |
| `/isadmin tradewinds moveport` | `uxmskyblock.admin.tradewinds.moveport` | Put a TradeWinds vessel in a port at once |
| `/isadmin realms remirror` | `uxmskyblock.admin.realms.remirror` | Lay a StrangerRealms island's Upside Down again from its land |
| `/isadmin parkour resettimes` | `uxmskyblock.admin.parkour.resettimes` | Forget the runs on a Parkour course, of every runner or of one |
| `/isadmin brix startcontest` | `uxmskyblock.admin.brix.startcontest` | Open a Brix build contest, on a theme written or a configured one |
| `/isadmin brix stopcontest` | `uxmskyblock.admin.brix.stopcontest` | Take the Brix build contest under way off, with its entries and votes |
| `/isadmin loot mark` | `uxmskyblock.admin.loot.mark` | Give a container a loot table, filled when it is first opened |
| `/isadmin top add` | `uxmskyblock.admin.top.add` | Make the sign, head or spot looked at show an island of a leaderboard |
| `/isadmin top remove` | `uxmskyblock.admin.top.remove` | Take a leaderboard display away |
| `/isadmin top list` | `uxmskyblock.admin.top.list` | List the leaderboard displays |
| `/isadmin boxed setsize` | `uxmskyblock.admin.boxed.setsize` | Set how far across a Boxed island's box is |
| `/isadmin boxed grow` | `uxmskyblock.admin.boxed.grow` | Push a Boxed island's box out |

## Permissions

| Node | Default | What it allows |
|---|---|---|
| `uxmskyblock.admin.bypass` | op | Bypass all island protection and boundaries |
| `uxmskyblock.admin.reload` | op | Reload plugin configuration |
| `uxmskyblock.admin.doctor` | op | Ask how the plugin is, one line per thing that can be wrong |
| `uxmskyblock.admin.manage` | op | Administrative island management |
| `uxmskyblock.admin.freeze` | op | Freeze and unfreeze islands administratively |
| `uxmskyblock.admin.inspect` | op | Inspect and bypass frozen island quarantines |
| `uxmskyblock.admin.restore` | op | Back up and restore islands and the database |
| `uxmskyblock.admin.booster` | op | Grant island boosters administratively |
| `uxmskyblock.admin.schematic` | op | Save and paste schematics |
| `uxmskyblock.admin.oneblock.setphase` | op | Move a OneBlock island to a phase |
| `uxmskyblock.admin.oneblock.setcount` | op | Set a OneBlock island's count of breaks |
| `uxmskyblock.admin.oneblock.addcount` | op | Add to or take from a OneBlock island's count of breaks |
| `uxmskyblock.admin.oneblock.check` | op | Read where a OneBlock island stands |
| `uxmskyblock.admin.boxed.setsize` | op | Set how far across a Boxed island's box is |
| `uxmskyblock.admin.boxed.grow` | op | Push a Boxed island's box out |
| `uxmskyblock.admin.chunkblock.open` | op | Open a chunk of a ChunkBlock island |
| `uxmskyblock.admin.chunkblock.close` | op | Close a chunk of a ChunkBlock island |
| `uxmskyblock.admin.chunkblock.reset` | op | Close every chunk of a ChunkBlock island but the first |
| `uxmskyblock.admin.tradewinds.setrank` | op | Give a TradeWinds vessel a rank |
| `uxmskyblock.admin.tradewinds.setstanding` | op | Set a TradeWinds vessel's standing in a port |
| `uxmskyblock.admin.tradewinds.moveport` | op | Put a TradeWinds vessel in a port at once |
| `uxmskyblock.admin.realms.remirror` | op | Lay a StrangerRealms island's Upside Down again from its land |
| `uxmskyblock.admin.parkour.resettimes` | op | Forget the runs on a Parkour course, of every runner or of one |
| `uxmskyblock.admin.brix.startcontest` | op | Open a Brix build contest, on a theme written or a configured one |
| `uxmskyblock.admin.brix.stopcontest` | op | Take the Brix build contest under way off, with its entries and votes |
| `uxmskyblock.admin.loot.mark` | op | Give a container a loot table, filled when it is first opened |
| `uxmskyblock.admin.top.add` | op | Make the sign, head or spot looked at show an island of a leaderboard |
| `uxmskyblock.admin.top.remove` | op | Take a leaderboard display away |
| `uxmskyblock.admin.top.list` | op | List the leaderboard displays |
| `uxmskyblock.admin.oneblock.respawn` | op | Lay a OneBlock island's block again |
| `uxmskyblock.island.create` | everyone | Create a new skyblock island |
| `uxmskyblock.island.delete` | everyone | Delete an island |
| `uxmskyblock.island.home` | everyone | Teleport to island home |
| `uxmskyblock.island.sethome` | everyone | Set island home location |
| `uxmskyblock.island.invite` | everyone | Invite members to island |
| `uxmskyblock.island.kick` | everyone | Kick members from island |
| `uxmskyblock.island.ban` | everyone | Ban players from island |
| `uxmskyblock.island.unban` | everyone | Unban players from island |
| `uxmskyblock.island.bank` | everyone | Access island bank |
| `uxmskyblock.island.biome` | everyone | Change island biome |
| `uxmskyblock.island.upgrade` | everyone | Upgrade island perks |
| `uxmskyblock.island.top` | everyone | View island leaderboards |
| `uxmskyblock.island.fly` | everyone | Fly on your own island with /is fly |
| `uxmskyblock.bypass.limits` | nobody | Place past an island's block and creature limits |
| `uxmskyblock.bypass.quarantine` | nobody | Enter a quarantined island |
| `uxmskyblock.bypass.coopcooldown` | nobody | Join another island before the join cooldown ends |
| `uxmskyblock.bypass.resetlimits` | nobody | Reset an island past the reset limits |
| `uxmskyblock.brix.keepmode` | op | Keep one's game mode on a Brix plot |
| `uxmskyblock.parkour.keepmode` | op | Keep one's game mode on a Parkour course |
| `uxmskyblock.parkour.checks.bypass` | nobody | Run a Parkour course without its checks |
| `uxmskyblock.chat.spy` | op | Spy on island private chat channels |
| `uxmskyblock.admin.*` | op | Every `uxmskyblock.admin.` node above |
| `uxmskyblock.bypass.*` | nobody | Every `uxmskyblock.bypass.` node above |
| `uxmskyblock.*` | nobody | Every node above |

The bypass, keep-mode and Parkour check nodes are the shipped names a module file sets; changing the name there changes the node.
What a member may do on their island is decided by island roles (`/is permissions`), not by these nodes. A word a player may not use
stays out of their command list, and typed anyway it answers with the no-permission line in their language.

## Placeholders

PlaceholderAPI, identifier `uxmskyblock`. Values are cached and refreshed in the background.

| Placeholder | Value |
|---|---|
| `%uxmskyblock_has_island%` | `true` when the player belongs to an island |
| `%uxmskyblock_island_id%` | the island's id |
| `%uxmskyblock_island_role%`, `%uxmskyblock_role%` | the player's role on the island |
| `%uxmskyblock_island_members_count%`, `%uxmskyblock_members_count%` | how many members the island has |
| `%uxmskyblock_island_bank_balance%`, `%uxmskyblock_bank_balance%` | the bank balance, formatted |
| `%uxmskyblock_island_bank_balance_raw%`, `%uxmskyblock_bank_balance_raw%` | the bank balance in minor units |
| `%uxmskyblock_island_bank_crystals%`, `%uxmskyblock_bank_crystals%` | the island's crystals |
| `%uxmskyblock_island_upgrade_tier_<upgrade>%`, `%uxmskyblock_upgrade_tier_<upgrade>%` | the tier the island holds of an upgrade |
| `%uxmskyblock_oneblock_is_oneblock%` | `true` on a OneBlock island |
| `%uxmskyblock_oneblock_<value>%` | `phase`, `phase_id`, `phase_number`, `phase_count`, `blocks_broken`, `phase_blocks`, `phase_length`, `phase_left`, `phase_percent`, `next_phase`, `next_phase_id`, `next_moment`, `cycle`, `my_breaks`, `top_member`, `top_member_breaks` |
| `%uxmskyblock_chunkblock_is_chunkblock%` | `true` on a ChunkBlock island |
| `%uxmskyblock_chunkblock_open_chunks%`, `%uxmskyblock_chunkblock_next_level%` | the open chunks and the next level of a ChunkBlock island |

### Leaderboards

Three boards, `level`, `worth` and `bank`, each for all time (`alltime`). A place is an island, under its owner's
name and head; a player's own place and value are their island's.

| Placeholder | Value |
|---|---|
| `%uxmskyblock_top_<board>_alltime_<n>_name%` | the owner of the island at place `n` |
| `%uxmskyblock_top_<board>_alltime_<n>_value%` | that island's level, worth or bank balance |
| `%uxmskyblock_top_<board>_alltime_<n>_head%` | the owner's name or id, for a `<head:...>` tag |
| `%uxmskyblock_rank_<board>_alltime%` | the reader's island's place |
| `%uxmskyblock_value_<board>_alltime%` | the reader's island's value |

A place nobody holds answers the catalogue's `placeholder.nobody`, and a missing value `placeholder.none`.

## Files

Everything is under `plugins/uxmSkyblock/`. Each start adds the keys a new release brought and keeps what the
operator wrote.

| File | What it holds |
|---|---|
| `config.conf` | This server on the network, the island grid, language, storage, player state, notifications, island roles, homes, the REST API, island backups and menu sounds |
| `modules.conf` | Which modules run, and the capabilities they need |
| `commands.conf` | The words of `/island` and `/isadmin`: names, aliases, words per language, on or off |
| `theme.conf` | Colours, glyphs and the writing of the interface |
| `text-input.conf` | How a menu asks a player to write something |
| `messages/messages_<language>.conf` | Every line a player reads, one file per language |
| `menus/*.conf` | One file per window: the island menu, members, roles, bank, vault, homes, warps, upgrades, missions, leaderboards, settings, biomes, and the windows of the game modes |
| `modules/*.conf` | One file per module: the game modes (`acidisland`, `boxed`, `brix`, `caveblock`, `chunkblock`, `oneblock`, `parkour`, `poseidon`, `skygrid`, `strangerrealms`, `tradewinds`), and `alliances`, `anti_abuse`, `bank`, `biomes`, `boosters`, `chat`, `dimensions`, `discord`, `effects`, `fly`, `generators`, `inactivity`, `interactables`, `island-effects`, `levels`, `lifecycle`, `limits`, `missions`, `performance`, `player-state`, `presets`, `protection`, `rewards`, `seasons`, `settings`, `shop`, `social`, `stacking`, `temporary-access`, `trade`, `upgrades`, `vault`, `visitors`, `walls`, `warps`, `webmap`, `world` |
| `oneblock/phases/*.conf` | One file per OneBlock phase: its blocks, creatures and chests |
| `loot/*.conf` | Loot tables containers draw from: SkyGrid chests, treasure and the deep boss |
| `data/` | The SQLite database (`skyblock.db`) and, by default, island backups (`island-backups/`) |
| `backups/` | The last five copies of each configuration file, taken before a start writes it again |
