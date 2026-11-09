# Data directives, loads, stores, and the heap.
.data
words:  .word 11, -22, 0x7fffffff
halves: .half 300, -300
bytes:  .byte 1, -1, 127
        .align 2
buffer: .space 16
text:   .ascii "abc"
        .asciiz "def"
.text
main:
    la $s0, words
    lw $a0, 4($s0)
    jal print
    la $s1, halves
    lh $a0, 2($s1)
    jal print
    lhu $a0, 2($s1)
    jal print
    la $s2, bytes
    lb $a0, 1($s2)
    jal print
    lbu $a0, 1($s2)
    jal print
    la $s3, buffer
    li $t0, 0x12345678
    sw $t0, 0($s3)
    lbu $a0, 0($s3)
    jal print
    lbu $a0, 3($s3)
    jal print
    sb $zero, 1($s3)
    lw $a0, 0($s3)
    jal print
    la $a0, text
    li $v0, 4
    syscall
    li $a0, 10
    li $v0, 11
    syscall
    li $a0, 64
    li $v0, 9
    syscall
    move $s4, $v0
    li $t0, 77
    sw $t0, 60($s4)
    lw $a0, 60($s4)
    jal print
    move $a0, $s4
    jal print
    li $v0, 10
    syscall
print:
    li $v0, 1
    syscall
    li $a0, 10
    li $v0, 11
    syscall
    jr $ra
