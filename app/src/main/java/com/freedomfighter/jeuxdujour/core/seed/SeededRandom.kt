package com.freedomfighter.jeuxdujour.core.seed

import java.util.Random

class SeededRandom(seed: Long) {
    private val random = Random(seed)

    fun nextInt(bound: Int): Int = random.nextInt(bound)

    fun nextFloat(): Float = random.nextFloat()

    fun <T> shuffle(list: MutableList<T>): MutableList<T> {
        for (i in list.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
        return list
    }

    fun <T> pick(list: List<T>, n: Int): List<T> {
        val copy = list.toMutableList()
        shuffle(copy)
        return copy.take(n)
    }
}
