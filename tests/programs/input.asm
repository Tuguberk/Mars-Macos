# Reads a number, a string, a character, and a float from standard input.
.data
buffer: .space 32
.text
main:
    li $v0, 5
    syscall
    addi $a0, $v0, 1
    li $v0, 1
    syscall
    li $a0, 10
    li $v0, 11
    syscall
    la $a0, buffer
    li $a1, 32
    li $v0, 8
    syscall
    li $v0, 4
    syscall
    li $v0, 12
    syscall
    move $a0, $v0
    li $v0, 11
    syscall
    li $a0, 10
    li $v0, 11
    syscall
    li $v0, 6
    syscall
    mov.s $f12, $f0
    li $v0, 2
    syscall
    li $v0, 10
    syscall
