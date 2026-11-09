# Print formats and the random number generator with a fixed seed.
.text
main:
    li $t0, -2
    move $a0, $t0
    li $v0, 34
    syscall
    jal line
    move $a0, $t0
    li $v0, 35
    syscall
    jal line
    move $a0, $t0
    li $v0, 36
    syscall
    jal line
    li $a0, 1
    li $a1, 12345
    li $v0, 40
    syscall
    li $a0, 1
    li $a1, 1000
    li $v0, 42
    syscall
    li $v0, 1
    syscall
    jal line
    li $a0, 1
    li $v0, 41
    syscall
    li $v0, 1
    syscall
    jal line
    li $v0, 10
    syscall
line:
    li $a0, 10
    li $v0, 11
    syscall
    jr $ra
