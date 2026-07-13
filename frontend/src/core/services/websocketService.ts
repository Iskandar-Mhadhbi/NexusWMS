/**
 * websocketService.ts
 *
 * Low-level STOMP-over-SockJS client wrapper. Deliberately knows nothing
 * about warehouse domain concepts (zones, events) — it only opens a STOMP
 * connection, subscribes to a destination, and hands raw parsed JSON to a
 * callback. Domain logic (mapping events to zone state) lives in
 * stores/liveOpsStore.ts, which is the only consumer of this file.
 *
 * Backend counterpart: WebSocketConfig.java (STOMP broker, /ws endpoint,
 * SockJS enabled — see phase5_progress.md). Auth is via JWT passed as a
 * query param on the SockJS handshake URL (?token=<jwt>) — this is the
 * documented, intentional pattern for SockJS HTTP pre-flight, not a
 * workaround (see phase6_progress.md).
 */
import SockJS from 'sockjs-client';
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import { tokenService } from './tokenService';

type MessageHandler = (payload: unknown) => void;

let client: Client | null = null;
const subscriptions = new Map<string, StompSubscription>(); 

/**
 * Opens the STOMP connection to the manager dashboard socket. Safe to call
 * multiple times — a no-op if already connected or connecting.
 *
 * @param onConnect - called once the STOMP CONNECTED frame is received.
 *   Subscriptions must be created inside this callback, not before it —
 *   STOMP requires an active connection before SUBSCRIBE frames can be sent.
 */
/**
 * Opens the STOMP connection to the manager dashboard socket. Auth is sent
 * via the STOMP CONNECT frame's Authorization header (connectHeaders),
 * NOT a query-string token — this was the deferred item from
 * phase6_progress.md ("STOMP frame header auth deferred... until frontend
 * work begins"). Query-param tokens leak into server/proxy access logs and
 * browser history; the CONNECT frame is invisible to both.
 *
 * Backend must read this header in a ChannelInterceptor on CONNECT frames
 * (see WebSocketConfig.java) — the existing JwtAuthFilter query-param
 * fallback only covers the initial SockJS HTTP handshake, not this.
 *
 * @param onConnect - called once the STOMP CONNECTED frame is received.
 */
export function connectDashboardSocket(onConnect: () => void): void {
  if (client?.active) return;

  const token = tokenService.get();
  // No token in the URL anymore — just the plain endpoint.
  const socketUrl = `${import.meta.env.VITE_WS_BASE_URL}/ws/dashboard`;

  client = new Client({
    webSocketFactory: () => new SockJS(socketUrl),
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    reconnectDelay: 5000,
    onConnect,
    onStompError: (frame) => {
      console.error('[websocketService] STOMP broker error:', frame.headers['message']);
    },
  });

  client.activate();
}

/**
 * Subscribes to a STOMP destination and parses every incoming frame body
 * as JSON before handing it to the callback.
 *
 * @param destination - STOMP destination path, e.g. "/topic/dashboard"
 * @param handler - receives the parsed JSON payload of each message
 */
export function subscribe(destination: string, handler: MessageHandler): void {
  if (!client?.active) {
    console.warn(`[websocketService] Cannot subscribe to ${destination} — not connected`);
    return;
  }

  const subscription = client.subscribe(destination, (message: IMessage) => {
    try {
      handler(JSON.parse(message.body));
    } catch (err) {
      console.error(`[websocketService] Failed to parse message on ${destination}:`, err);
    }
  });

  subscriptions.set(destination, subscription);
}

/** Tears down all subscriptions and closes the STOMP connection. */
export function disconnectDashboardSocket(): void {
  subscriptions.forEach((sub) => sub.unsubscribe());
  subscriptions.clear();
  client?.deactivate();
  client = null;
}