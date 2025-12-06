/**
 * SSE (Server-Sent Events) clients for streaming WireGuard connection information.
 *
 * These clients connect to the SSE endpoints defined in the backend's SystemController.
 * Unlike the HTTP clients, SSE provides real-time streaming data updates.
 *
 * Event types:
 * - 'network-info-update': Real-time WireGuard network and peer connection data
 * - 'client-info-update': Real-time WireGuard client/peer connection data
 * - 'error': Error events from the server
 */

export interface StreamNetworkConnectionInfoRequest {
  networkPublicKeyValue: string;
}

export interface StreamClientConnectionInfoRequest {
  clientPublicKeyValue: string;
}

/**
 * WireGuard peer/client connection information from wg show output.
 */
export interface WireGuardPeerInfo {
  publicKey: string;
  presharedKey: string;
  endpoint: string;
  allowedIps: string;
  latestHandshakeEpochSeconds: number;
  bytesReceived: number;
  bytesSent: number;
  persistentKeepalive: number | null;
}

/**
 * Network-level WireGuard information including all peers.
 * This is the data structure sent in 'network-info-update' events.
 * Note: peers is converted from the JSON object to a Map for easier iteration.
 */
export interface NetworkConnectionInfo {
  interfaceName: string;
  publicKey: string;
  listeningPort: number;
  // Map of peer public keys to their connection info
  peers: Map<string, WireGuardPeerInfo>;
}

/**
 * Client-level WireGuard information for a single peer.
 * This is the data structure sent in 'client-info-update' events.
 */
export interface ClientConnectionInfo extends WireGuardPeerInfo {}

export interface SSEConnectionOptions {
  onMessage: (event: MessageEvent) => void;
  onError?: (error: Event) => void;
  onOpen?: (event: Event) => void;
}

/**
 * Creates an SSE connection to stream network WireGuard connection information.
 *
 * @param request - Request containing the network public key
 * @param options - Callbacks for handling SSE events
 * @returns EventSource-like object with close() method to terminate the connection
 *
 * @example
 * ```ts
 * const eventSource = streamNetworkConnectionInfo(
 *   { networkPublicKeyValue: network.networkPublicKeyValue },
 *   {
 *     onMessage: (event) => {
 *       if (event.type === 'network-info-update') {
 *         const data: NetworkConnectionInfo = event.data;
 *         setNetworkInfo(data);
 *         // Iterate over peers map
 *         data.peers.forEach((peerInfo, publicKey) => {
 *           console.log(`Peer ${publicKey}: ${peerInfo.endpoint}`);
 *         });
 *       } else if (event.type === 'error') {
 *         handleError(event.data);
 *       }
 *     },
 *     onError: (error) => {
 *       eventSource.close();
 *     }
 *   }
 * );
 *
 * // Later, to stop streaming:
 * eventSource.close();
 * ```
 */
export function streamNetworkConnectionInfo(
  request: StreamNetworkConnectionInfoRequest,
  options: SSEConnectionOptions
): EventSource {
  const { onMessage, onError, onOpen } = options;

  const url = `${window.location.origin}/api/v1/system/wg-show/network`;

  const controller = new AbortController();

  (async () => {
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
        },
        credentials: 'include',
        body: JSON.stringify(request),
        signal: controller.signal,
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      if (onOpen) {
        onOpen(new Event('open'));
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('Response body is not readable');
      }

      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();

        if (done) {
          break;
        }

        buffer += decoder.decode(value, { stream: true });

        // Process complete SSE messages (separated by double newlines)
        const lines = buffer.split('\n\n');
        buffer = lines.pop() || ''; // Keep incomplete message in buffer

        for (const message of lines) {
          if (message.trim()) {
            // Parse SSE format: "event: eventName\ndata: {...}"
            const eventMatch = message.match(/^event:\s*(.+)$/m);
            const dataMatch = message.match(/^data:\s*(.+)$/m);

            if (dataMatch) {
              let eventData: any = dataMatch[1];

              // For network-info-update events, parse and convert peers object to Map
              if (eventMatch && eventMatch[1] === 'network-info-update') {
                const parsed = JSON.parse(eventData);
                if (parsed.peers && typeof parsed.peers === 'object') {
                  parsed.peers = new Map(Object.entries(parsed.peers));
                }
                eventData = parsed;
              }

              const event = new MessageEvent(eventMatch ? eventMatch[1] : 'message', {
                data: eventData,
              });
              onMessage(event);
            }
          }
        }
      }
    } catch (error) {
      if (onError && error.name !== 'AbortError') {
        onError(new ErrorEvent('error', { error, message: error.message }));
      }
    }
  })();

  // Return an EventSource-like object with a close method
  return {
    close: () => controller.abort(),
    CONNECTING: 0,
    OPEN: 1,
    CLOSED: 2,
    readyState: 1,
    url,
    withCredentials: true,
    onopen: null,
    onmessage: null,
    onerror: null,
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  } as EventSource;
}

/**
 * Creates an SSE connection to stream client WireGuard connection information.
 *
 * @param request - Request containing the client public key
 * @param options - Callbacks for handling SSE events
 * @returns EventSource-like object with close() method to terminate the connection
 *
 * @example
 * ```ts
 * const eventSource = streamClientConnectionInfo(
 *   { clientPublicKeyValue: client.clientPublicKeyValue },
 *   {
 *     onMessage: (event) => {
 *       if (event.type === 'client-info-update') {
 *         const data: ClientConnectionInfo = event.data;
 *         setClientInfo(data);
 *       } else if (event.type === 'error') {
 *         handleError(event.data);
 *       }
 *     },
 *     onError: (error) => {
 *       eventSource.close();
 *     }
 *   }
 * );
 *
 * // Later, to stop streaming:
 * eventSource.close();
 * ```
 */
export function streamClientConnectionInfo(
  request: StreamClientConnectionInfoRequest,
  options: SSEConnectionOptions
): EventSource {
  const { onMessage, onError, onOpen } = options;

  const url = `${window.location.origin}/api/v1/system/wg-show/client`;

  const controller = new AbortController();

  (async () => {
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
        },
        credentials: 'include',
        body: JSON.stringify(request),
        signal: controller.signal,
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      if (onOpen) {
        onOpen(new Event('open'));
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('Response body is not readable');
      }

      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();

        if (done) {
          break;
        }

        buffer += decoder.decode(value, { stream: true });

        // Process complete SSE messages (separated by double newlines)
        const lines = buffer.split('\n\n');
        buffer = lines.pop() || '';

        for (const message of lines) {
          if (message.trim()) {
            const eventMatch = message.match(/^event:\s*(.+)$/m);
            const dataMatch = message.match(/^data:\s*(.+)$/m);

            if (dataMatch) {
              let eventData: any = dataMatch[1];

              // For client-info-update events, parse the JSON
              if (eventMatch && eventMatch[1] === 'client-info-update') {
                eventData = JSON.parse(eventData);
              }

              const event = new MessageEvent(eventMatch ? eventMatch[1] : 'message', {
                data: eventData,
              });
              onMessage(event);
            }
          }
        }
      }
    } catch (error) {
      if (onError && error.name !== 'AbortError') {
        onError(new ErrorEvent('error', { error, message: error.message }));
      }
    }
  })();

  return {
    close: () => controller.abort(),
    CONNECTING: 0,
    OPEN: 1,
    CLOSED: 2,
    readyState: 1,
    url,
    withCredentials: true,
    onopen: null,
    onmessage: null,
    onerror: null,
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  } as EventSource;
}
