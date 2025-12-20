package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.events.SyncProgressEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Component
class SyncProgressEmitter {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val emitters = ConcurrentHashMap<UUID, MutableList<SseEmitter>>()
    private val eventHistory = ConcurrentHashMap<UUID, MutableList<IndexedEvent>>()

    data class IndexedEvent(val id: Long, val event: SyncProgressEvent)

    fun register(syncId: UUID, emitter: SseEmitter, lastEventId: Long? = null): SseEmitter {
        val syncEmitters = emitters.computeIfAbsent(syncId) { CopyOnWriteArrayList() }
        syncEmitters.add(emitter)

        emitter.onCompletion {
            logger.debug("SSE emitter completed for sync {}", syncId)
            syncEmitters.remove(emitter)
            cleanupIfEmpty(syncId)
        }

        emitter.onTimeout {
            logger.debug("SSE emitter timed out for sync {}", syncId)
            syncEmitters.remove(emitter)
            cleanupIfEmpty(syncId)
        }

        emitter.onError { error ->
            logger.debug("SSE emitter error for sync {}: {}", syncId, error.message)
            syncEmitters.remove(emitter)
            cleanupIfEmpty(syncId)
        }

        // Replay missed events if reconnecting
        if (lastEventId != null) {
            replayEvents(syncId, emitter, lastEventId)
        }

        logger.info("SSE client connected for sync {} (total clients: {})", syncId, syncEmitters.size)
        return emitter
    }

    fun emit(syncId: UUID, event: SyncProgressEvent) {
        val eventId = storeEvent(syncId, event)
        val syncEmitters = emitters[syncId] ?: return

        val deadEmitters = mutableListOf<SseEmitter>()

        syncEmitters.forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .id(eventId.toString())
                        .data(event.toJson())
                )
            } catch (e: Exception) {
                logger.debug("Failed to send event to emitter: {}", e.message)
                deadEmitters.add(emitter)
            }
        }

        deadEmitters.forEach { syncEmitters.remove(it) }

        // If sync is complete/failed/cancelled, schedule cleanup
        if (event is SyncProgressEvent.Completed ||
            event is SyncProgressEvent.Error ||
            event is SyncProgressEvent.Cancelled
        ) {
            scheduleCleanup(syncId)
        }
    }

    fun hasConnectedClients(syncId: UUID): Boolean =
        emitters[syncId]?.isNotEmpty() == true

    private fun storeEvent(syncId: UUID, event: SyncProgressEvent): Long {
        val history = eventHistory.computeIfAbsent(syncId) { CopyOnWriteArrayList() }
        val eventId = history.size.toLong() + 1
        history.add(IndexedEvent(eventId, event))
        return eventId
    }

    private fun replayEvents(syncId: UUID, emitter: SseEmitter, afterEventId: Long) {
        val history = eventHistory[syncId] ?: return

        history.filter { it.id > afterEventId }.forEach { indexedEvent ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .id(indexedEvent.id.toString())
                        .data(indexedEvent.event.toJson())
                )
            } catch (e: Exception) {
                logger.warn("Failed to replay event {} for sync {}", indexedEvent.id, syncId)
            }
        }
    }

    private fun cleanupIfEmpty(syncId: UUID) {
        val syncEmitters = emitters[syncId]
        if (syncEmitters?.isEmpty() == true) {
            emitters.remove(syncId)
        }
    }

    private fun scheduleCleanup(syncId: UUID) {
        // Keep event history for 5 minutes after completion for reconnection
        Thread.startVirtualThread {
            Thread.sleep(5 * 60 * 1000) // 5 minutes
            eventHistory.remove(syncId)
            emitters.remove(syncId)
            logger.debug("Cleaned up event history for sync {}", syncId)
        }
    }
}
