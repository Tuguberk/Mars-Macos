# This program must stop with an address error.
.text
main:
    li $t0, 1
    lw $t1, 0($t0)
    li $v0, 10
    syscall
