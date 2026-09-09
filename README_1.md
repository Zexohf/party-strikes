# Party Strikes (RuneLite plugin)

A dumb-fun accountability plugin: give the people in your RuneLite **party** a strike
whenever they mess up. Three strikes and you're out.

It piggybacks on RuneLite's built-in Party system, so strikes sync to everyone in the
party automatically over the same WebSocket the Party plugin uses. No server of your own.

## Features

- **Side panel** with every party member, sorted worst-first (a "wall of shame"), each
  with a `Strike!` button and an optional reason box.
- **Live sync** across the whole party. Strikes, resets, and late-joiner catch-up all
  propagate. Every event carries a unique id, so echoes and re-syncs never double count.
- **Chat announcements**, e.g. `Party Strikes: Zezima struck Woox for "misclicked the vengeance" -- that's 2 now.`
- **Sound effect** when a strike lands (configurable sound ID; default is the teleport whoosh).
- **"Three strikes and you're out"** callout at a configurable threshold.
- **Overlay** showing your own strike count and a flavor title
  (Squeaky Clean -> Rookie Mistake -> On Thin Ice -> Three Strikes! -> Certified Menace -> Walking Disaster).
- **Reset button** to wipe the party's tally and start a fresh session.

Party members who don't have this plugin are unaffected: RuneLite silently drops
message types it doesn't recognize, so the strike messages just pass them by.

## How to use

1. Enable the built-in **Party** plugin and join/create a party (share the passphrase).
2. Enable **Party Strikes**.
3. Open the panel from the sidebar (the red `!` icon), pick someone, optionally type a
   reason, and hit **Strike!**.

## Building / running

**Easiest (dev client):** clone the RuneLite client repo, drop the `com/partystrikes`
package into `runelite-client/src/main/java/`, and run the client's `RuneLite` main class
from your IDE with `--developer-mode`.

**As an external plugin:** import this folder as a Gradle project (it pulls the RuneLite
client as a `compileOnly` dependency). Use the standard
[RuneLite external plugin](https://github.com/runelite/example-plugin) run configuration
to launch the client with this plugin on the classpath.

**Plugin Hub:** `runelite-plugin.properties` is included. Follow the Plugin Hub
submission guide if you want to publish it — set a real `author` first.

## Config

| Option | Default | What it does |
| --- | --- | --- |
| Chat announcements | on | Print a game message on each strike |
| Only announce/sound for me | off | React only when *you* get struck |
| Play a sound | on | Play a sound effect on each strike |
| Strike sound ID | 200 | In-game sound effect id (try 2266 / 2277 / 3813) |
| Strikes to "out" | 3 | Threshold for the big OUT callout (0 = off) |
| Show overlay | on | Show the personal strike-count overlay |
