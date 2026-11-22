<H1 align="center">MojoSuperLite Launcher</H1>

<img src="./app_mojo/src/main/assets/mojo.png" align="left" width="150" height="150" alt="MojoSuperLite Launcher logo">

[

![Android CI](https://github.com/Novz1221/mojolauncher-lite/workflows/Android%20CI/badge.svg)

](https://github.com/Novz1221/mojolauncher-lite/actions)

* MojoSuperLite Launcher is a lightweight launcher for playing Minecraft: Java Edition on your Android device. It is a fork of [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher), which itself is based on [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher).

* It can run almost every version of Minecraft, allowing you to use .jar only installers to install modloaders such as [Forge](https://files.minecraftforge.net/) and [Fabric](http://fabricmc.net/) and mods like [OptiFine](https://optifine.net).

## Navigation
- [Introduction](#introduction)
- [What's different](#whats-different)
- [Getting MojoSuperLite Launcher](#getting-mojosuperlite-launcher)
- [Building](#building)
- [Current roadmap](#current-roadmap)
- [License](#license)
- [Contributing](#contributing)
- [Credits & Third party components and their licenses](#third-party-components-licenses-and-sources-when-applicable)

## Introduction
* MojoSuperLite Launcher is a Minecraft: Java Edition launcher for Android, forked from [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher) (a.k.a. MJLauncher) and based on [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher).
* This launcher can launch almost all available Minecraft versions ranging from rd-132211 to 26.x snapshots (including Combat Test versions).
* Modding via Forge and Fabric are also supported.

## What's different
* Renamed to **MojoSuperLite Launcher**.
* Fixed a crash when opening the settings screen caused by an empty `ic_px_arrow_left` vector drawable.
* Added an "All files access" permission prompt (`MANAGE_EXTERNAL_STORAGE`) on Android 11 and newer, shown at startup.
* Added the **MobileGlues** renderer. It is detected automatically when `libmobileglues.so` is bundled in the app, or when the [MobileGlues plugin](https://github.com/MobileGL-Dev/MobileGlues-release/releases) app is installed. Its config is stored in `/sdcard/MG/` (shared with the MobileGlues app), falling back to the launcher folder if that is not writable.

> The application ID (`git.artdeell.mjlaunch`) and internal package names are unchanged, so existing data and builds stay compatible.

## Getting MojoSuperLite Launcher

You can get MojoSuperLite Launcher via two methods:

1. You can get early builds from [GitHub Actions](https://github.com/Novz1221/mojolauncher-lite/actions).

2. You can [build](#building) from source.

## Building
* Build the launcher (it will automatically download all required components)
If you are building on Windows:
* Replace `./gradlew` with `.\gradlew.bat`
* Make sure `mojoexec`, `sdl`, `glfw` are symlinked into `app_mojo/src/main/jni/`

## Current roadmap
- [x] Instance system in favor of profiles
- [x] Out-of-the box 1.21.5 support
- [x] mrpack/CurseForge zip import
- [x] LTW: enable compute shader/image extensions
- [ ] LTW: resolve issues with Create
- [ ] LTW: switch to a color-renderable format for framebuffers
- [ ] Modpack/mod management tool
- [ ] MMC-compatible instance import
- [ ] Vintage Story support
- [ ] Implement common native library standard

## Known Issues
- Some physical mice may have very slow mouse speed
- On Holy GL4ES, large texture atlases may be distorted (resulting in stretched/blocky textures in modpacks)
- Probably more, that's why we have a bug tracker ;)

## License
- MojoSuperLite Launcher, like the upstream MojoLauncher, is licensed under [GNU LGPLv3](https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/LICENSE).

## Contributing
Contributions are welcome! We appreciate any type of contribution, not only code. For example, you can help the wiki shape up. You can help the [translation](https://mojolauncher.ru/translate/translator.html) too!


Any code change to this repository should be submitted as a pull request. The description should explain what the code does and give steps to execute it.

## Third party components, licenses and sources (when applicable)
- [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher) (upstream of this fork): [GNU LGPLv3 License](https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/LICENSE)
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [GNU LGPLv3 License](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt).
- [Holy GL4ES](https://github.com/artdeell/gl4es_extra_extra/): [MIT License](https://github.com/ptitSeb/gl4es/blob/master/LICENSE).
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [GNU GPLv2 License](https://openjdk.java.net/legal/gplv2+ce.html).
- [GLFW](https://github.com/MojoLauncher/glfw): [zlib license](https://github.com/MojoLauncher/glfw/blob/glfw34/LICENSE.md)
- [SDL](https://github.com/MojoLauncher/MojoSDL): [zlib license](https://github.com/MojoLauncher/MojoSDL/blob/main/LICENSE.txt)
- [LWJGL2-GLFW](https://github.com/MojoLauncher/lwjgl2-glfw): 3-Clause BSD license
- [LWJGL3](https://github.com/LWJGL/lwjgl3): [BSD-3 License](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md).
- [mojoexec](https://github.com/MojoLauncher/mojoexec): [MIT License](https://github.com/MojoLauncher/mojoexec/blob/master/LICENSE)
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [MIT License](https://docs.mesa3d.org/license.html).
- [pro-grade](https://github.com/pro-grade/pro-grade) (Java sandboxing security manager): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt).
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector) (Used for authorisation via ely.by): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE).
- [alsoft](https://github.com/kcat/openal-soft/) (Audio output library): [GNU Library General Public License](https://github.com/kcat/openal-soft/blob/master/COPYING) and [Modified PFFFT](https://github.com/kcat/openal-soft/blob/master/LICENSE-pffft).
- [oboe](https://github.com/google/oboe): [Apache License 2.0](https://github.com/google/oboe/blob/main/LICENSE).
- Thanks to [Mineskin](https://mineskin.eu/) for providing Minecraft avatars.
