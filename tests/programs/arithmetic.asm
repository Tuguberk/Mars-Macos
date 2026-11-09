# Integer and logic instructions. Prints each result on one line.
.macro show (%reg)
    move $a0, %reg
    li $v0, 1
    syscall
    li $a0, 10
    li $v0, 11
    syscall
.end_macro
.text
main:
    li $t0, 1000
    li $t1, -7
    add $t2, $t0, $t1
    show ($t2)
    sub $t2, $t0, $t1
    show ($t2)
    mult $t0, $t1
    mflo $t2
    mfhi $t3
    show ($t2)
    show ($t3)
    div $t0, $t1
    mflo $t2
    mfhi $t3
    show ($t2)
    show ($t3)
    sll $t2, $t1, 3
    show ($t2)
    srl $t2, $t1, 3
    show ($t2)
    sra $t2, $t1, 3
    show ($t2)
    and $t2, $t0, $t1
    show ($t2)
    or $t2, $t0, $t1
    show ($t2)
    xor $t2, $t0, $t1
    show ($t2)
    nor $t2, $t0, $t1
    show ($t2)
    slt $t2, $t1, $t0
    show ($t2)
    sltu $t2, $t1, $t0
    show ($t2)
    addu $t2, $t1, $t1
    show ($t2)
    li $v0, 10
    syscall
