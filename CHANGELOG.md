# Changelog

## 0.0.1 (2026-11-05)


### ⚠ BREAKING CHANGES

* **storage:** placeholders are `%uxmskyblock_...%`; `leaderboard_top_` and `rank` give way to the board grammar.
* **storage:** `database` is now `storage` with the standard keys, and the old `storage` block is `island-backups`.
* **config:** `server-node` is `network` with `server-name`; `settings.conf` and `protection.conf` keys moved; unread keys removed.
* **build:** versions restart at 0.0.1.

### Features

* **command:** let a player choose a language, and an operator set it ([#9](https://github.com/UXPLIMA/uxm-skyblock/issues/9)) ([ec1f75f](https://github.com/UXPLIMA/uxm-skyblock/commit/ec1f75fb69a236785192150c8541ad4c46c4eb4e))
* **command:** refuse with the catalog line, never Brigadier's text ([#6](https://github.com/UXPLIMA/uxm-skyblock/issues/6)) ([1f4917f](https://github.com/UXPLIMA/uxm-skyblock/commit/1f4917f9de5c9c9a7c208d05fd6575dae578397e))
* **message:** read German, Spanish, French, Portuguese and Russian ([#10](https://github.com/UXPLIMA/uxm-skyblock/issues/10)) ([05dbbb5](https://github.com/UXPLIMA/uxm-skyblock/commit/05dbbb5098c5c533749c170a7a77fad018b7e09c))
* **storage:** keep the database in data/, config copies in backups/ ([#4](https://github.com/UXPLIMA/uxm-skyblock/issues/4)) ([c7575ee](https://github.com/UXPLIMA/uxm-skyblock/commit/c7575ee85ca4a671c553ff63221e1ed42fb57002))


### Bug Fixes

* **build:** describe the plugin in plain words ([#14](https://github.com/UXPLIMA/uxm-skyblock/issues/14)) ([958f7eb](https://github.com/UXPLIMA/uxm-skyblock/commit/958f7eb719d6dc4a42cbdf85b1d29f1e49df631c))
* **core:** run console action lines on the global region ([#5](https://github.com/UXPLIMA/uxm-skyblock/issues/5)) ([667d185](https://github.com/UXPLIMA/uxm-skyblock/commit/667d18506a2885f87823bbdce1456ceb3289b119))
* **island:** let a player who joins an island use it at once ([#13](https://github.com/UXPLIMA/uxm-skyblock/issues/13)) ([d9c50e7](https://github.com/UXPLIMA/uxm-skyblock/commit/d9c50e7bc572c920fd38b9c4b084e30bf9152e06))
* **menu:** put menu navigation in the middle of the bottom row ([#7](https://github.com/UXPLIMA/uxm-skyblock/issues/7)) ([686dfc4](https://github.com/UXPLIMA/uxm-skyblock/commit/686dfc40cb50d3c7250e96bae98b81f1c07bc2eb))
* **menu:** take the close button out of six windows ([#15](https://github.com/UXPLIMA/uxm-skyblock/issues/15)) ([ca88fe6](https://github.com/UXPLIMA/uxm-skyblock/commit/ca88fe63028c020a20f7aa6e78ccd290b8a31c0a))


### Code Refactoring

* **build:** move to uxm-gradle and the repository standard ([#1](https://github.com/UXPLIMA/uxm-skyblock/issues/1)) ([0f3c2ed](https://github.com/UXPLIMA/uxm-skyblock/commit/0f3c2eda34acbae3e5db6b82cec77b86eee1f2b2))
* **config:** speak to the server owner and drop unread keys ([#2](https://github.com/UXPLIMA/uxm-skyblock/issues/2)) ([5e4470d](https://github.com/UXPLIMA/uxm-skyblock/commit/5e4470daa257bd124e44d0e876b0e1fb75c1ba12))
* **storage:** run the island boards on uxmlib-board ([#8](https://github.com/UXPLIMA/uxm-skyblock/issues/8)) ([eea8a0e](https://github.com/UXPLIMA/uxm-skyblock/commit/eea8a0e3ffa8e2d9a6dcd7daddceba186d7aca5b))
