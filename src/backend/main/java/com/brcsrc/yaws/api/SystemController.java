package com.brcsrc.yaws.api;

import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.requests.StreamClientConnectionInfoRequest;
import com.brcsrc.yaws.model.requests.StreamNetworkConnectionInfoRequest;
import com.brcsrc.yaws.system.ClientConnectionInformation;
import com.brcsrc.yaws.system.NetworkConnectionInformation;
import com.brcsrc.yaws.system.WireguardInformationProvider;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * REST controller for system-level operations and real-time monitoring.
 *
 * <h2>Purpose</h2>
 * This controller provides Server-Sent Events (SSE) endpoints for streaming real-time WireGuard
 * connection information to clients. It enables live monitoring of network interfaces and client
 * connections without requiring clients to poll the server.
 *
 * <h2>Server-Sent Events (SSE) Overview</h2>
 * SSE is a server push technology that allows a server to send real-time updates to clients over
 * a single HTTP connection. Unlike WebSockets, SSE is unidirectional (server to client only) and
 * uses standard HTTP, making it simpler and firewall-friendly.
 *
 * <h3>SSE Lifecycle</h3>
 * <ol>
 *   <li>Client initiates connection with HTTP POST request</li>
 *   <li>Server responds with HTTP 200 and keeps connection open</li>
 *   <li>Server periodically sends events as text/event-stream</li>
 *   <li>Connection closes on timeout, client disconnect, or server error</li>
 * </ol>
 *
 * <h3>HTTP Status Codes in SSE</h3>
 * <ul>
 *   <li><b>Initial connection:</b> Standard HTTP status (200, 401, 500, etc.) sent once at start</li>
 *   <li><b>During streaming:</b> No HTTP status codes - errors communicated via event names or connection closure</li>
 *   <li><b>Mid-stream errors:</b> Sent as named events (e.g., "error") with error data, or connection is closed</li>
 * </ul>
 *
 * <h2>Architecture & Design Decisions</h2>
 *
 * <h3>Why POST instead of GET?</h3>
 * WireGuard public keys contain characters (=, +, /) that are problematic in URLs even with encoding.
 * Using POST with JSON body provides a cleaner, more reliable approach:
 * <pre>
 * POST /api/v1/system/wg-show/network
 * {"networkPublicKeyValue": "pHIHd17qGbJlqYmKnBZcXxXJkPUeARJNfIJadpgKHG0="}
 * </pre>
 *
 * <h3>Thread Management Strategy</h3>
 * <b>Problem:</b> Creating a dedicated thread per SSE connection doesn't scale. With 100 concurrent
 * connections, you'd have 100 threads constantly polling the cache, wasting resources on constrained
 * devices like Raspberry Pi.
 *
 * <b>Solution:</b> Use a small, fixed-size ScheduledExecutorService (2 threads) that all SSE connections
 * share. Each connection schedules a periodic task, but the actual execution is multiplexed across
 * the thread pool. This means:
 * <ul>
 *   <li>2 threads can serve unlimited SSE connections</li>
 *   <li>Tasks are lightweight (just cache reads and network I/O)</li>
 *   <li>Resource usage is bounded and predictable</li>
 * </ul>
 *
 * <h3>Data Source: Cache-Based Design</h3>
 * This controller does NOT execute 'wg show' commands directly. Instead, it reads from
 * {@link WireguardInformationProvider}, which maintains a singleton cache updated by a
 * background thread every 5 seconds. Benefits:
 * <ul>
 *   <li>Single command execution regardless of active SSE connections</li>
 *   <li>Consistent data across all clients</li>
 *   <li>Minimal system overhead on resource-constrained devices</li>
 * </ul>
 *
 * <h2>Event Types</h2>
 * This controller emits two types of events:
 * <ul>
 *   <li><b>network-info-update / client-info-update:</b> Successful data emission (JSON)</li>
 *   <li><b>error:</b> Application-level errors like "Network not found" (plain text)</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 *
 * <h3>Client Disconnects</h3>
 * When a client disconnects (browser close, network drop, timeout), the next {@code emitter.send()}
 * throws {@code IOException: Broken pipe}. This is NORMAL and expected - we handle it gracefully
 * by canceling the scheduled task and completing the emitter cleanly.
 *
 * <h3>Spring Security Integration</h3>
 * After a "Broken pipe" error, Spring Security may throw {@code AccessDeniedException} during
 * error handling cleanup. This is a harmless side effect of the response already being committed.
 * By calling {@code emitter.complete()} instead of {@code emitter.completeWithError()} for broken
 * pipes, we prevent this error cascade.
 *
 * <h3>Other IO Errors</h3>
 * Non-broken-pipe IOExceptions (serialization failures, actual network issues) are logged as
 * errors and result in {@code emitter.completeWithError()} to properly signal the client.
 *
 * <h2>Client-Side Usage</h2>
 *
 * <h3>Browser (Native EventSource - only supports GET)</h3>
 * Since these endpoints use POST, native EventSource won't work. Use a library like
 * {@code @microsoft/fetch-event-source} that supports POST requests with EventSource-style API.
 *
 * <h3>Example with fetch-event-source</h3>
 * <pre>
 * import { fetchEventSource } from '@microsoft/fetch-event-source';
 *
 * await fetchEventSource('/api/v1/system/wg-show/network', {
 *   method: 'POST',
 *   headers: { 'Content-Type': 'application/json' },
 *   body: JSON.stringify({ networkPublicKeyValue: 'pHI...' }),
 *   onmessage(event) {
 *     if (event.event === 'network-info-update') {
 *       const data = JSON.parse(event.data);
 *       console.log('Network update:', data);
 *     } else if (event.event === 'error') {
 *       console.error('Server error:', event.data);
 *     }
 *   },
 *   onerror(err) {
 *     console.error('Connection error:', err);
 *     // Connection closed or network failure
 *   }
 * });
 * </pre>
 *
 * <h2>Testing with Insomnia</h2>
 * Insomnia supports SSE but has a hardcoded 30-second timeout that cannot be configured
 * (see https://github.com/Kong/insomnia/discussions/3713). For testing:
 * <ol>
 *   <li>Create a new request and change type to "Event Stream" (not HTTP POST)</li>
 *   <li>Set method to POST and URL to endpoint</li>
 *   <li>Add JSON body with public key</li>
 *   <li>Expect connection to auto-close after ~30 seconds</li>
 * </ol>
 *
 * <h2>Configuration Constants</h2>
 * <ul>
 *   <li><b>SSE_TIMEOUT:</b> Maximum connection lifetime (30 minutes) before server closes stream</li>
 *   <li><b>SSE_EMIT_INTERVAL_MS:</b> How often to read cache and emit events (5 seconds)</li>
 *   <li><b>EVENT_NETWORK_INFO_UPDATE:</b> Event name for network data updates</li>
 *   <li><b>EVENT_CLIENT_INFO_UPDATE:</b> Event name for client data updates</li>
 *   <li><b>EVENT_ERROR:</b> Event name for application-level errors</li>
 * </ul>
 *
 * @see WireguardInformationProvider
 * @see SseEmitter
 */
@RestController
@RequestMapping(Constants.BASE_URL + "/system")
public class SystemController {

    private static final Logger logger = LoggerFactory.getLogger(SystemController.class);

    // SSE Configuration
    /** Maximum time an SSE connection will stay open before server-side timeout (30 minutes) */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    /** Interval between event emissions - how often to read cache and send updates (5 seconds) */
    private static final long SSE_EMIT_INTERVAL_MS = 5000L;

    // SSE Event Names
    /** Event name for network connection information updates */
    private static final String EVENT_NETWORK_INFO_UPDATE = "network-info-update";

    /** Event name for client/peer connection information updates */
    private static final String EVENT_CLIENT_INFO_UPDATE = "client-info-update";

    /** Event name for application-level errors (e.g., "Network not found") */
    private static final String EVENT_ERROR = "error";

    // Dependencies
    /** Provides cached WireGuard connection data updated by background thread */
    private final WireguardInformationProvider wireguardInfo;

    /**
     * Shared thread pool for scheduling SSE event emissions.
     * Only 2 threads serve all concurrent SSE connections via task scheduling.
     * Tasks are lightweight (cache reads + network I/O), so 2 threads is sufficient.
     */
    private final ScheduledExecutorService scheduler;

    @Autowired
    public SystemController(WireguardInformationProvider wireguardInfo) {
        this.wireguardInfo = wireguardInfo;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    /**
     * Streams real-time WireGuard network connection information via Server-Sent Events.
     *
     * <h3>How It Works</h3>
     * <ol>
     *   <li>Client POSTs network public key in JSON body</li>
     *   <li>Server immediately sends initial network data (or error if not found)</li>
     *   <li>Server schedules periodic task to emit updates every 5 seconds</li>
     *   <li>Connection stays open until timeout (30 min), client disconnect, or error</li>
     *   <li>On close, scheduled task is cancelled to prevent resource leaks</li>
     * </ol>
     *
     * <h3>Event Stream Format</h3>
     * <pre>
     * event: network-info-update
     * data: {"interfaceName":"wg0","publicKey":"pHI...","listeningPort":51820,"peers":{...}}
     *
     * event: network-info-update
     * data: {"interfaceName":"wg0","publicKey":"pHI...","listeningPort":51820,"peers":{...}}
     * </pre>
     *
     * <h3>Error Scenarios</h3>
     * <ul>
     *   <li><b>Network not found:</b> Emits "error" event with message, stream continues</li>
     *   <li><b>Client disconnects:</b> IOException caught, task cancelled, emitter completed cleanly</li>
     *   <li><b>Serialization error:</b> Logged as error, emitter completed with error, stream closes</li>
     * </ul>
     *
     * @param request Contains networkPublicKeyValue (WireGuard public key with =, +, / characters)
     * @return SseEmitter that streams network-info-update or error events as text/event-stream
     */
    @Operation(
            summary = "Stream Network WireGuard Information",
            description = "Server-Sent Events endpoint that streams real-time WireGuard connection data for a specific network and its peers"
    )
    @PostMapping(
            value = "/wg-show/network",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamNetworkConnectionInfo(@RequestBody StreamNetworkConnectionInfoRequest request) {
        String networkPublicKey = request.getNetworkPublicKeyValue();
        logger.info("SSE connection established for network: {}", networkPublicKey);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // Send initial data immediately to establish the SSE connection.
        // This ensures the client receives the HTTP 200 response and starts listening
        // without waiting for the first scheduled emission.
        try {
            NetworkConnectionInformation network = wireguardInfo.getNetworkByPublicKey(networkPublicKey);
            if (network != null) {
                emitter.send(SseEmitter.event()
                        .name(EVENT_NETWORK_INFO_UPDATE)
                        .data(network, MediaType.APPLICATION_JSON));
            } else {
                logger.warn("Network not found for public key: {}", networkPublicKey);
                emitter.send(SseEmitter.event()
                        .name(EVENT_ERROR)
                        .data("Network not found", MediaType.TEXT_PLAIN));
            }
        } catch (IOException e) {
            logger.error("Failed to send initial SSE data for network: {}", networkPublicKey, e);
            emitter.completeWithError(e);
            return emitter;
        }

        // Schedule a periodic task to read from cache and emit updates.
        // The task runs every SSE_EMIT_INTERVAL_MS (5 seconds) starting after the first interval.
        // Initial data was already sent above, so we start the schedule after a delay.
        // Using AtomicReference to allow lambda to reference the task for self-cancellation.
        AtomicReference<ScheduledFuture<?>> scheduledTaskRef = new AtomicReference<>();
        ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                NetworkConnectionInformation network = wireguardInfo.getNetworkByPublicKey(networkPublicKey);

                if (network != null) {
                    emitter.send(SseEmitter.event()
                            .name(EVENT_NETWORK_INFO_UPDATE)
                            .data(network, MediaType.APPLICATION_JSON));
                } else {
                    logger.warn("Network not found for public key: {}", networkPublicKey);
                    emitter.send(SseEmitter.event()
                            .name(EVENT_ERROR)
                            .data("Network not found", MediaType.TEXT_PLAIN));
                }
            } catch (IOException e) {
                // "Broken pipe" means the client disconnected (browser close, network drop, etc.)
                // This is NORMAL and expected - not an error condition.
                // We complete the emitter cleanly to avoid triggering Spring Security errors.
                if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                    logger.debug("SSE client disconnected for network: {}", networkPublicKey);
                    ScheduledFuture<?> task = scheduledTaskRef.get();
                    if (task != null) task.cancel(true);
                    emitter.complete();
                } else {
                    // Other IOExceptions (serialization failures, actual network issues) are real errors
                    logger.error("SSE IO error for network: {}", networkPublicKey, e);
                    ScheduledFuture<?> task = scheduledTaskRef.get();
                    if (task != null) task.cancel(true);
                    emitter.completeWithError(e);
                }
            } catch (Exception e) {
                // Unexpected errors (NPE, runtime exceptions) - log and close stream
                logger.error("Error in SSE stream for network: {}", networkPublicKey, e);
                ScheduledFuture<?> task = scheduledTaskRef.get();
                if (task != null) task.cancel(true);
                emitter.completeWithError(e);
            }
        }, SSE_EMIT_INTERVAL_MS, SSE_EMIT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        scheduledTaskRef.set(scheduledTask);

        // Register cleanup callbacks to cancel the scheduled task when the connection closes.
        // This prevents resource leaks by ensuring tasks don't continue after the client is gone.

        emitter.onCompletion(() -> {
            logger.info("SSE completed for network: {}", networkPublicKey);
            scheduledTask.cancel(true);
        });

        emitter.onTimeout(() -> {
            logger.info("SSE timeout for network: {}", networkPublicKey);
            scheduledTask.cancel(true);
            emitter.complete();
        });

        emitter.onError((ex) -> {
            logger.error("SSE error for network: {}", networkPublicKey, ex);
            scheduledTask.cancel(true);
        });

        return emitter;
    }

    /**
     * Streams real-time WireGuard client/peer connection information via Server-Sent Events.
     *
     * <h3>How It Works</h3>
     * <ol>
     *   <li>Client POSTs peer public key in JSON body</li>
     *   <li>Server immediately sends initial client data (or error if not found)</li>
     *   <li>Server schedules periodic task to emit updates every 5 seconds</li>
     *   <li>Connection stays open until timeout (30 min), client disconnect, or error</li>
     *   <li>On close, scheduled task is cancelled to prevent resource leaks</li>
     * </ol>
     *
     * <h3>Event Stream Format</h3>
     * <pre>
     * event: client-info-update
     * data: {"publicKey":"uRj...","endpoint":"172.56.149.120:62118","latestHandshakeEpochSeconds":1701234567,...}
     *
     * event: client-info-update
     * data: {"publicKey":"uRj...","endpoint":"172.56.149.120:62118","latestHandshakeEpochSeconds":1701234567,...}
     * </pre>
     *
     * <h3>Error Scenarios</h3>
     * <ul>
     *   <li><b>Client not found:</b> Emits "error" event with message, stream continues</li>
     *   <li><b>Client disconnects:</b> IOException caught, task cancelled, emitter completed cleanly</li>
     *   <li><b>Serialization error:</b> Logged as error, emitter completed with error, stream closes</li>
     * </ul>
     *
     * @param request Contains clientPublicKeyValue (WireGuard public key with =, +, / characters)
     * @return SseEmitter that streams client-info-update or error events as text/event-stream
     */
    @Operation(
            summary = "Stream Client WireGuard Information",
            description = "Server-Sent Events endpoint that streams real-time WireGuard connection data for a specific client/peer"
    )
    @PostMapping(
            value = "/wg-show/client",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamClientConnectionInfo(@RequestBody StreamClientConnectionInfoRequest request) {
        String clientPublicKey = request.getClientPublicKeyValue();
        logger.info("SSE connection established for client: {}", clientPublicKey);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // Send initial data immediately to establish the SSE connection.
        // This ensures the client receives the HTTP 200 response and starts listening
        // without waiting for the first scheduled emission.
        try {
            ClientConnectionInformation client = wireguardInfo.getPeerByPublicKey(clientPublicKey);
            if (client != null) {
                emitter.send(SseEmitter.event()
                        .name(EVENT_CLIENT_INFO_UPDATE)
                        .data(client, MediaType.APPLICATION_JSON));
            } else {
                logger.warn("Client not found for public key: {}", clientPublicKey);
                emitter.send(SseEmitter.event()
                        .name(EVENT_ERROR)
                        .data("Client not found", MediaType.TEXT_PLAIN));
            }
        } catch (IOException e) {
            logger.error("Failed to send initial SSE data for client: {}", clientPublicKey, e);
            emitter.completeWithError(e);
            return emitter;
        }

        // Schedule a periodic task to read from cache and emit updates.
        // The task runs every SSE_EMIT_INTERVAL_MS (5 seconds) starting after the first interval.
        // Initial data was already sent above, so we start the schedule after a delay.
        // Using AtomicReference to allow lambda to reference the task for self-cancellation.
        AtomicReference<ScheduledFuture<?>> scheduledTaskRef = new AtomicReference<>();
        ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                ClientConnectionInformation client = wireguardInfo.getPeerByPublicKey(clientPublicKey);

                if (client != null) {
                    emitter.send(SseEmitter.event()
                            .name(EVENT_CLIENT_INFO_UPDATE)
                            .data(client, MediaType.APPLICATION_JSON));
                } else {
                    logger.warn("Client not found for public key: {}", clientPublicKey);
                    emitter.send(SseEmitter.event()
                            .name(EVENT_ERROR)
                            .data("Client not found", MediaType.TEXT_PLAIN));
                }
            } catch (IOException e) {
                // "Broken pipe" means the client disconnected (browser close, network drop, etc.)
                // This is NORMAL and expected - not an error condition.
                // We complete the emitter cleanly to avoid triggering Spring Security errors.
                if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                    logger.debug("SSE client disconnected for client: {}", clientPublicKey);
                    ScheduledFuture<?> task = scheduledTaskRef.get();
                    if (task != null) task.cancel(true);
                    emitter.complete();
                } else {
                    // Other IOExceptions (serialization failures, actual network issues) are real errors
                    logger.error("SSE IO error for client: {}", clientPublicKey, e);
                    ScheduledFuture<?> task = scheduledTaskRef.get();
                    if (task != null) task.cancel(true);
                    emitter.completeWithError(e);
                }
            } catch (Exception e) {
                // Unexpected errors (NPE, runtime exceptions) - log and close stream
                logger.error("Error in SSE stream for client: {}", clientPublicKey, e);
                ScheduledFuture<?> task = scheduledTaskRef.get();
                if (task != null) task.cancel(true);
                emitter.completeWithError(e);
            }
        }, SSE_EMIT_INTERVAL_MS, SSE_EMIT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        scheduledTaskRef.set(scheduledTask);

        // Register cleanup callbacks to cancel the scheduled task when the connection closes.
        // This prevents resource leaks by ensuring tasks don't continue after the client is gone.

        emitter.onCompletion(() -> {
            logger.info("SSE completed for client: {}", clientPublicKey);
            scheduledTask.cancel(true);
        });

        emitter.onTimeout(() -> {
            logger.info("SSE timeout for client: {}", clientPublicKey);
            scheduledTask.cancel(true);
            emitter.complete();
        });

        emitter.onError((ex) -> {
            logger.error("SSE error for client: {}", clientPublicKey, ex);
            scheduledTask.cancel(true);
        });

        return emitter;
    }
}
