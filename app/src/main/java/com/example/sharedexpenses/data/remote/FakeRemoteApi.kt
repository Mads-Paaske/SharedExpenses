package com.example.sharedexpenses.data.remote

import com.example.sharedexpenses.data.local.OutboxEntity
import com.example.sharedexpenses.data.network.NetworkMonitor
import kotlinx.coroutines.delay
import java.io.IOException

/**
 * Mock backend (NF06: dummy data and mock interactions).
 * Behaves like a real network call: it takes time, and it fails when the device is offline
 * or when the demo "simulate network failure" switch is on.
 */
class FakeRemoteApi(
    private val network: NetworkMonitor,
    private val latencyMs: Long = 2_500,
) {
    /** Ids the "server" has accepted - makes sending the same request twice harmless. */
    private val received = mutableSetOf<Long>()

    suspend fun send(request: OutboxEntity) {
        delay(latencyMs)
        if (!network.isOnline()) throw IOException("Network unavailable")
        received += request.id
    }
}
