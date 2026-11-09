# A trap goes to the exception handler, then the program continues.
.data
after: .asciiz "after trap\n"
.kdata
message: .asciiz "in handler\n"
.text
main:
    li $t0, 1
    teqi $t0, 1
    li $v0, 4
    la $a0, after
    syscall
    li $v0, 10
    syscall
.ktext 0x80000180
    li $v0, 4
    la $a0, message
    syscall
    mfc0 $k0, $14
    addi $k0, $k0, 4
    mtc0 $k0, $14
    eret
