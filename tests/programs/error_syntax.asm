# This program must fail in the assembler.
.text
main:
    addx $t0, $t1, $t2
    li $t0, $t1
    j missing_label
