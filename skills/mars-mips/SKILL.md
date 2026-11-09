---
name: mars-mips
description: Assemble, run, test, and debug MIPS assembly programs with the MARS simulator (Mars-Macos). Use this when the user works with MIPS assembly, .asm or .s files, MARS, SPIM-style syscalls, registers, or memory dumps, or asks why a MIPS program gives the wrong result.
---

# MIPS with MARS

Run the real program before you say what it does. Do not guess register values or output.

## Command

In this file, `MARS` means this command:

```
/Applications/Mars-Macos.app/Contents/MacOS/Mars-Macos
```

If the user installed the terminal command, `mars program.asm` also works. It adds `nc` for you.

## Always run it like this

```
MARS nc me ae2 se3 1000000 program.asm < /dev/null
```

| Part | Meaning |
|---|---|
| `nc` | No copyright text |
| `me` | MARS messages go to stderr, program output stays on stdout |
| `ae2` | Exit code 2 for an assemble error |
| `se3` | Exit code 3 for a runtime error |
| `1000000` | Step limit. **Always set one**, or a bad loop never ends |
| `< /dev/null` | Stops MARS from waiting for keyboard input |

## Read the result

| Result | Meaning |
|---|---|
| Exit code 0 | Finished, but also check stderr (see below) |
| Exit code 2 | Assemble error. stderr has the file and line |
| Exit code 3 | Runtime error, for example a bad address. stderr has the address |
| stderr has `maximum step limit ... reached` | **The exit code is still 0.** The program did not finish. It is probably an endless loop |
| stderr has `NullPointerException` in `SystemIO` | The program wanted input and there was none. Give it input |

## Give input

Put one answer on each line of a file:

```
MARS nc me ae2 se3 1000000 program.asm < input.txt
```

## Look inside the program

Add these before the file name. Values print to stderr at the end of the run.

```
MARS nc me ae2 se3 1000000 t0 t1 s0 program.asm < /dev/null
MARS nc me ae2 se3 1000000 0x10010000-0x10010020 program.asm < /dev/null
MARS nc me ae2 se3 1000000 dec ic v0 program.asm < /dev/null
```

| Option | Meaning |
|---|---|
| `t0`, `$t0` | Show this register. You can repeat it |
| `0x10010000-0x10010020` | Show these memory words. Addresses must be on a word boundary |
| `dec`, `hex`, `ascii` | How to show values |
| `ic` | Count the instructions that ran |
| `a` | Assemble only |
| `sm` | Start at the global label `main` |
| `db` | Turn on delayed branching (off by default) |
| `np` | Do not allow pseudo instructions |
| `p` | Assemble all files in the folder of this file |
| `pa one two` | Program arguments. Put this after all file names |

MARS has no breakpoints on the command line. To debug, run with a low step limit and show registers, then raise the limit and compare.

## MARS facts

- `.text` starts at `0x00400000`, `.data` starts at `0x10010000`.
- Without `.globl main` and `sm`, the program starts at the first instruction.
- A program should end with syscall 10. If it runs off the end of `.text`, MARS stops, but this is a bad habit.

| `$v0` | Syscall | Arguments and result |
|---|---|---|
| 1 | Print integer | `$a0` |
| 4 | Print string | `$a0` = address |
| 5 | Read integer | Result in `$v0` |
| 8 | Read string | `$a0` = buffer, `$a1` = size |
| 9 | Allocate memory | `$a0` = bytes, result in `$v0` |
| 10 | Exit | |
| 11 | Print character | `$a0` |
| 12 | Read character | Result in `$v0` |
| 17 | Exit with code | `$a0` |

## How to help

Many MARS users are students. If the work looks like homework, help them find and understand the bug. Explain which register or line is wrong and why, and do not only give a finished answer.

<!-- Installed by Mars-Macos -->
