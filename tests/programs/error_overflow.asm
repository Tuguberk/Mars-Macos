# This program must stop with an arithmetic overflow error.
.text
main:
    li $t0, 0x7fffffff
    add $t1, $t0, $t0
    li $v0, 10
    syscall
