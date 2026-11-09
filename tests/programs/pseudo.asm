# Pseudo-instructions, .eqv, and a macro with a label.
.eqv LIMIT 3
.macro repeat_char (%char, %count)
    li $t8, 0
again:
    li $a0, %char
    li $v0, 11
    syscall
    addi $t8, $t8, 1
    blt $t8, %count, again
.end_macro
.macro line
    li $a0, 10
    li $v0, 11
    syscall
.end_macro
.text
main:
    repeat_char (65, LIMIT)
    line
    repeat_char (66, 2)
    line
    li $t0, 0xDEADBEEF
    move $a0, $t0
    li $v0, 34
    syscall
    line
    rol $a0, $t0, 8
    li $v0, 34
    syscall
    line
    ror $a0, $t0, 8
    li $v0, 34
    syscall
    line
    li $t1, -15
    abs $a0, $t1
    li $v0, 1
    syscall
    line
    neg $a0, $t1
    li $v0, 1
    syscall
    line
    not $a0, $t1
    li $v0, 1
    syscall
    line
    seq $a0, $t1, -15
    li $v0, 1
    syscall
    line
    sne $a0, $t1, -15
    li $v0, 1
    syscall
    line
    sge $a0, $t1, 0
    li $v0, 1
    syscall
    line
    rem $a0, $t1, 4
    li $v0, 1
    syscall
    line
    mulu $a0, $t1, 2
    li $v0, 36
    syscall
    line
    li $v0, 10
    syscall
