package com.phapalesai.voicealert.speech

import com.phapalesai.voicealert.data.NotificationEvent
import java.util.concurrent.ConcurrentLinkedQueue

object SpeechQueue {
    private val queue = ConcurrentLinkedQueue<NotificationEvent>()

    fun enqueue(event: NotificationEvent) {
        queue.add(event)
    }

    fun poll(): NotificationEvent? {
        return queue.poll()
    }

    fun clear() {
        queue.clear()
    }

    fun isEmpty(): Boolean = queue.isEmpty()
}
