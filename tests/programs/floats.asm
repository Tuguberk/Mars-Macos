# Coprocessor 1: float and double math, compare, convert.
.data
a: .float 1.5
b: .float -0.25
c: .double 3.141592653589793
d: .double 2.0
nl: .asciiz "\n"
.text
main:
    l.s $f0, a
    l.s $f1, b
    add.s $f12, $f0, $f1
    jal print_s
    mul.s $f12, $f0, $f1
    jal print_s
    div.s $f12, $f0, $f1
    jal print_s
    abs.s $f12, $f1
    jal print_s
    l.d $f2, c
    l.d $f4, d
    mul.d $f12, $f2, $f4
    jal print_d
    sqrt.d $f12, $f4
    jal print_d
    c.lt.s $f1, $f0
    bc1t smaller
    li $a0, 0
    j show_flag
smaller:
    li $a0, 1
show_flag:
    li $v0, 1
    syscall
    li $v0, 4
    la $a0, nl
    syscall
    li $t0, 42
    mtc1 $t0, $f6
    cvt.s.w $f12, $f6
    jal print_s
    cvt.w.d $f8, $f2
    mfc1 $a0, $f8
    li $v0, 1
    syscall
    li $v0, 10
    syscall
print_s:
    li $v0, 2
    syscall
    j newline
print_d:
    li $v0, 3
    syscall
newline:
    li $v0, 4
    la $a0, nl
    syscall
    jr $ra
