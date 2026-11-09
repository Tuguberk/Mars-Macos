# Mars-Macos for AI agents

Mars-Macos is a macOS version of the MARS MIPS simulator. The code is Java Swing and needs JDK 21.

## Run MIPS programs

Read `skills/mars-mips/SKILL.md`. It has the right command, exit codes, and options. After a build, the command is `java -jar build/Mars.jar`.

The app installs this skill for you in `~/.claude/skills` and `~/.agents/skills`.

## Work on this code

```sh
scripts/build.sh           # makes build/Mars.jar
scripts/test.sh            # compares 14 programs with official MARS 4.5
scripts/package-macos.sh   # makes the Mac app and the dmg (macOS only)
```

- Simulator behavior must stay the same as original MARS. Run `scripts/test.sh` after a change.
- Mac code is in `mars/venus/Mac*.java`. System file and color windows are in `FilePicker` and `ColorPicker`.
- To publish a version, push a tag such as `v4.6.1`. CI makes the dmg and the `latest.json` file that the updater reads.
