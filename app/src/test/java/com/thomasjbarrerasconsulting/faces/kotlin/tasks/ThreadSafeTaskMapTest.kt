package com.thomasjbarrerasconsulting.faces.kotlin.tasks

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger

class ThreadSafeTaskMapTest {

    @Test
    fun `pop on empty map returns null`() {
        val map = ThreadSafeTaskMap()
        assertNull(map.pop())
    }

    @Test
    fun `update and pop returns the task`() {
        val map = ThreadSafeTaskMap()
        var executed = false
        map.update("key1") { executed = true }

        val task = map.pop()
        assertNotNull(task)
        task!!.invoke()
        assertTrue(executed)
    }

    @Test
    fun `pop removes the entry`() {
        val map = ThreadSafeTaskMap()
        map.update("key1") { }

        assertNotNull(map.pop())
        assertNull(map.pop())
    }

    @Test
    fun `duplicate key is ignored`() {
        val map = ThreadSafeTaskMap()
        var firstCalled = false
        var secondCalled = false

        map.update("key1") { firstCalled = true }
        map.update("key1") { secondCalled = true }

        val task = map.pop()
        task!!.invoke()

        assertTrue(firstCalled)
        assertFalse(secondCalled)
        assertNull(map.pop())
    }

    @Test
    fun `multiple keys pop in insertion order`() {
        val map = ThreadSafeTaskMap()
        val order = mutableListOf<Int>()

        map.update("a") { order.add(1) }
        map.update("b") { order.add(2) }
        map.update("c") { order.add(3) }

        map.pop()!!.invoke()
        map.pop()!!.invoke()
        map.pop()!!.invoke()

        assertEquals(listOf(1, 2, 3), order)
    }

    @Test
    fun `concurrent updates do not lose entries`() {
        val map = ThreadSafeTaskMap()
        val threadCount = 10
        val barrier = CyclicBarrier(threadCount)
        val latch = CountDownLatch(threadCount)

        for (i in 0 until threadCount) {
            Thread {
                barrier.await()
                map.update("key_$i") { }
                latch.countDown()
            }.start()
        }

        latch.await()

        val popCount = AtomicInteger(0)
        while (map.pop() != null) {
            popCount.incrementAndGet()
        }

        assertEquals(threadCount, popCount.get())
    }
}
