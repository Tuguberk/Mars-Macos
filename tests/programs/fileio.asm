# Writes a file, reads it back, and prints it.
.data
name:   .asciiz "output.txt"
text:   .asciiz "file works\n"
buffer: .space 32
.text
main:
    la $a0, name
    li $a1, 1
    li $a2, 0
    li $v0, 13
    syscall
    move $s0, $v0
    move $a0, $s0
    la $a1, text
    li $a2, 11
    li $v0, 15
    syscall
    move $a0, $v0
    li $v0, 1
    syscall
    li $a0, 10
    li $v0, 11
    syscall
    move $a0, $s0
    li $v0, 16
    syscall
    la $a0, name
    li $a1, 0
    li $a2, 0
    li $v0, 13
    syscall
    move $s0, $v0
    move $a0, $s0
    la $a1, buffer
    li $a2, 32
    li $v0, 14
    syscall
    move $a0, $s0
    li $v0, 16
    syscall
    la $a0, buffer
    li $v0, 4
    syscall
    li $v0, 10
    syscall
