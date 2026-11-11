# Loffsfield Chess

Forge mod project for Minecraft 1.20.1. The project targets Java 17 and is
licensed under GPL-3.0, matching the repository license.

## Current milestone

The functional-blocks creative tab contains a chess table. Place it and
right-click to open the 2D board. The first two players who open that table
take White and Black; later players can spectate. The server validates moves
with the chess-rules module, stores the current position as FEN on the table,
and synchronizes it to players viewing that table.

The 2D screen and piece-set item icons use JohnPablok's improved Cburnett chess
set; the pieces rendered on the 3D table are still simple block sculptures.
When a player wins, the board is blurred behind
a clear winner banner. Left-click a piece and then a destination to move; pawn
promotions currently choose a queen. Right-click a square to highlight it, or
right-click and drag to draw an arrow. Left-clicking an empty square clears
annotations. These annotations are local to each viewer.

The White seat faces the placer. Players who open the table from that side take
White; the opposite side takes Black, and lateral approaches take the nearest
seat. A player can switch to the other seat by opening the table from that side
if it is unoccupied; occupied seats are not reassigned. The 2D board is rotated
for Black.

The clock button cycles through no clock, Bullet 1+0 and 2+1, Blitz 3+0, 3+2,
and 5+0, and Rapid 10+0 and 15+10. Choose it before the first move; a timed
game starts when both seats are filled. The server owns the clocks and applies
increments after legal moves. Players can offer/accept draws, resign (with a
confirmation click), copy a finished game's PGN to the clipboard, or start a
new game with the same players and clock preset.

The felt color comes from any of the 16 vanilla dyes. Craft the table by putting
the white set, a dye, and the black set across the top row, with three planks
of the same wood below. Its legs use that wood. To recolor an existing table,
combine its item with a dye; the wood, game, players, and clock data are kept.
The felt is inset within a narrow wooden rim that matches the table's wood.
The table item uses the table block model, and the piece-set items use the
matching pawn artwork as their inventory icons.
White or black chess-piece sets are crafted from any wooden button and the
matching dye. The independent rules module validates legal moves, check,
checkmate, stalemate, castling, en passant, and promotion. Remaining chess
polish includes choosing a promotion piece and automatic claimable draws such
as repetition and the fifty-move rule.

## Art attribution

Chess-piece graphics are adapted from [JohnPablok's improved Cburnett chess
set](https://opengameart.org/content/chess-pieces-and-board-squares), licensed
under [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/).

## Build and test

The Gradle wrapper provisions the JDK 17 toolchain used by Forge:

```sh
./gradlew test
./gradlew build
```

The mod jar is created at `build/libs/chess-minecraft-mod-0.1.0.jar`. To launch
a development client:

```sh
./gradlew runClient
```
