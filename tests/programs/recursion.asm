# Recursion with the stack, and a bubble sort loop.
.data
list: .word 9, 3, 7, 1, 8, 2
.text
main:
    li $a0, 10
    jal factorial
    move $a0, $v0
    li $v0, 1
    syscall
    li $a0, 10
    li $v0, 11
    syscall

    la $s0, list
    li $s1, 6
outer:
    li $t0, 0
    li $t1, 0
inner:
    addi $t2, $s1, -1
    bge $t1, $t2, inner_end
    sll $t3, $t1, 2
    add $t3, $t3, $s0
    lw $t4, 0($t3)
    lw $t5, 4($t3)
    ble $t4, $t5, no_swap
    sw $t5, 0($t3)
    sw $t4, 4($t3)
    li $t0, 1
no_swap:
    addi $t1, $t1, 1
    j inner
inner_end:
    bnez $t0, outer

    li $t1, 0
show:
    beq $t1, $s1, done
    sll $t3, $t1, 2
    add $t3, $t3, $s0
    lw $a0, 0($t3)
    li $v0, 1
    syscall
    li $a0, 32
    li $v0, 11
    syscall
    addi $t1, $t1, 1
    j show
done:
    li $v0, 10
    syscall

factorial:
    addi $sp, $sp, -8
    sw $ra, 4($sp)
    sw $a0, 0($sp)
    bgt $a0, 1, recurse
    li $v0, 1
    addi $sp, $sp, 8
    jr $ra
recurse:
    addi $a0, $a0, -1
    jal factorial
    lw $a0, 0($sp)
    lw $ra, 4($sp)
    addi $sp, $sp, 8
    mul $v0, $v0, $a0
    jr $ra
