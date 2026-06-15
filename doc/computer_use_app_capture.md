# Computer Use app capture workflow

This project uses LibGDX/LWJGL. On macOS, running the frontend through
`desktop:runGame` starts a bare `java` process. Some screen-control agents,
including Computer Use, can see the window visually but cannot resolve it as a
controllable app.

Use the packaged debug app when an agent needs to capture or operate the game
window.

## Build and install the debug app

From the repository root:

```bash
./gradlew desktop:installComputerUseApp
```

This creates a jpackage app image and installs it here:

```text
~/Applications/FabricatedBookDebug.app
```

The app bundle id is:

```text
com.fabricatedbook.debug
```

## Launch a debug screen

Use `openComputerUseApp` instead of `runGame`:

```bash
./gradlew desktop:openComputerUseApp -Pargs="title"
./gradlew desktop:openComputerUseApp -Pargs="font"
./gradlew desktop:openComputerUseApp -Pargs="map"
./gradlew desktop:openComputerUseApp -Pargs="battle"
./gradlew desktop:openComputerUseApp -Pargs="shop"
./gradlew desktop:openComputerUseApp -Pargs="event"
```

The `-Pargs` value is forwarded to `DesktopLauncher`, so all frontend debug
arguments documented in `doc/frontend_debug_cli.md` work here too.

## Capture with Computer Use

After launch, use one of these app targets:

```text
FabricatedBookDebug
/Users/mcjiansheng/Applications/FabricatedBookDebug.app
```

Prefer the full app path if multiple debug app copies exist. The build output
also contains:

```text
desktop/build/computerUseApp/image/FabricatedBookDebug.app
```

If both copies remain registered with macOS, using the bundle id
`com.fabricatedbook.debug` can be ambiguous.

## Why this works

The Gradle `runGame` task exposes the window as `java`. The jpackage app gives
macOS a stable app bundle path, bundle id, and app name. Computer Use can then
enumerate the game as `FabricatedBookDebug` and return a screenshot plus the
basic accessibility tree.

LibGDX renders most game UI inside an OpenGL surface, so Computer Use may only
see the top-level window and menu items in the accessibility tree. Use the
screenshot for visual inspection and coordinate clicks for game-canvas
interaction.
