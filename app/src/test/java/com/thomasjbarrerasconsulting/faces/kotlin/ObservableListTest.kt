package com.thomasjbarrerasconsulting.faces.kotlin

import android.util.Log
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ObservableListTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `items returns empty list initially`() {
        val list = ObservableList<String>()
        assertTrue(list.items().isEmpty())
    }

    @Test
    fun `updateIfDifferent with new list updates items`() {
        val list = ObservableList<String>()
        list.updateIfDifferent(listOf("a", "b"))

        assertEquals(listOf("a", "b"), list.items())
    }

    @Test
    fun `updateIfDifferent notifies listeners when list changes`() {
        val list = ObservableList<String>()
        var notified = false
        var receivedItems: List<String>? = null

        list.addListener(object : ObservableList.ListUpdatedListener<String> {
            override fun listUpdated(list: List<String>) {
                notified = true
                receivedItems = list
            }
        })

        list.updateIfDifferent(listOf("x", "y"))

        assertTrue(notified)
        assertEquals(listOf("x", "y"), receivedItems)
    }

    @Test
    fun `updateIfDifferent does not notify when list is identical`() {
        val list = ObservableList<String>()
        list.updateIfDifferent(listOf("a", "b"))

        var notifiedCount = 0
        list.addListener(object : ObservableList.ListUpdatedListener<String> {
            override fun listUpdated(list: List<String>) {
                notifiedCount++
            }
        })

        list.updateIfDifferent(listOf("a", "b"))
        assertEquals(0, notifiedCount)
    }

    @Test
    fun `removeListener stops notifications`() {
        val list = ObservableList<String>()
        var notified = false

        val listener = object : ObservableList.ListUpdatedListener<String> {
            override fun listUpdated(list: List<String>) {
                notified = true
            }
        }

        list.addListener(listener)
        list.removeListener(listener)
        list.updateIfDifferent(listOf("a"))

        assertFalse(notified)
    }

    @Test
    fun `duplicate addListener is ignored`() {
        val list = ObservableList<String>()
        var notifyCount = 0

        val listener = object : ObservableList.ListUpdatedListener<String> {
            override fun listUpdated(list: List<String>) {
                notifyCount++
            }
        }

        list.addListener(listener)
        list.addListener(listener)
        list.updateIfDifferent(listOf("a"))

        assertEquals(1, notifyCount)
    }

    @Test
    fun `listener exception does not prevent other listeners`() {
        val list = ObservableList<String>()
        var secondListenerCalled = false

        list.addListener(object : ObservableList.ListUpdatedListener<String> {
            override fun listUpdated(list: List<String>) {
                throw RuntimeException("test exception")
            }
        })

        list.addListener(object : ObservableList.ListUpdatedListener<String> {
            override fun listUpdated(list: List<String>) {
                secondListenerCalled = true
            }
        })

        list.updateIfDifferent(listOf("a"))

        assertTrue(secondListenerCalled)
    }

    @Test
    fun `items returns a copy not the internal list`() {
        val list = ObservableList<String>()
        list.updateIfDifferent(listOf("a", "b"))

        val items = list.items()
        list.updateIfDifferent(listOf("c"))

        // Original items reference should be unchanged
        assertEquals(listOf("a", "b"), items)
    }
}
