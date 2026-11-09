# Mars-Macos

Mars-Macos is an unofficial macOS version of MARS, the MIPS Assembler and Runtime Simulator. It is not made by the MARS authors. MARS was made by Pete Sanderson and Ken Vollmar, and it uses the MIT license (see `MARSlicense.txt`).

It adds a real Mac app, the Mac menu bar, dark mode that follows macOS, Finder file open, reload of files that change on disk, a `mars` terminal command, and updates inside the app.

**Download:** get the `.dmg` file from [Releases](https://github.com/Tuguberk/Mars-Macos/releases/latest). It is for Apple Silicon Macs and has its own Java.

The app has no Apple Developer ID yet, so macOS blocks the first start. Open the app once, then go to System Settings > Privacy & Security and click **Open Anyway**.

The app checks for a new version once a day. You can turn this off in Settings, or check yourself with Help > Check for Updates.

---

# MARS
MARS (official) MIPS Assembler and Runtime Simulator

 MARS is a lightweight interactive development environment (IDE) for programming in MIPS assembly language, intended for educational-level use with Patterson and Hennessy's Computer Organization and Design.

 It is available for you to download as an executable JAR file.  Click the MARS v.4.5 link at right, under Releases, to access the download.

 We have relocated the MARS website to  https://dpetersanderson.github.io/    There are still a couple of display issues to work out but all website content is there.  This includes all documentation and the JAR download.  We are not experienced in github so if you know a way we can host the website in this MARS repository let us know.

 This is the original MARS, developed by Pete Sanderson and Ken Vollmar.  Developed while Pete was professor at Otterbein University and Ken was professor at Missouri State University.  Both are now retired. This is a legacy application but is very stable and continues to be used by universities throughout the world as of 2024. The last release was MARS 4.5 in August 2014.  
 
 Until October 2024 the MARS website, including executable JAR file download, was hosted by Missouri State University. That is no longer the case. That's why it is here.

## Mars-Macos development

Mars-Macos is a macOS fork of MARS. You need JDK 21.

```sh
git config core.hooksPath .githooks   # run this once after clone
scripts/build.sh                      # makes build/Mars.jar
scripts/test.sh                       # compares results with official MARS
```

The commit hook removes co-author lines. Please write commit messages in simple English.

To make the Mac app and the dmg file:

```sh
scripts/package-macos.sh   # makes build/package/Mars-Macos-<version>-<arch>.dmg
```

The app has its own Java. It has no Apple Developer ID signature yet.
