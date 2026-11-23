* Renamed to **MojoSuperLite Launcher**. English is the only bundled language.
* Fixed crashes in the settings screens caused by empty vector icons (all placeholder icons were replaced with valid ones).
* Added an "All files access" permission prompt (`MANAGE_EXTERNAL_STORAGE`) on Android 11 and newer, shown at startup.
* Added the **MobileGlues** renderer. It is detected automatically when `libmobileglues.so` is bundled in the app, or when the [MobileGlues plugin](https://github.com/MobileGL-Dev/MobileGlues-release/releases) app is installed. Its config is stored in `/sdcard/MG/` (shared with the MobileGlues app), falling back to the launcher folder if that is not writable.
* Restored the "Share log file" button in portrait mode and the "Create modpack instance" / "Import local modpack" options.
* Performance options (Settings > Video):
  * **Fast load**: skips bytecode verification when loading classes for shorter loading times. It has no effect on FPS. Turn it off if a mod crashes with an unclear error.
  * **More Performance**: asks Android to run the game as fast as the device allows, without changing any Minecraft setting. It requests the highest screen refresh rate, gives the busiest game threads to the system performance hint service (Android 12+), sets the Game Mode state (Android 13+) and raises thread priority. It uses more battery and may heat up the phone, and it replaces sustained performance mode while enabled.

> The application ID (`git.artdeell.mjlaunch`) and internal package names are unchanged, so existing data and builds stay compatible.
