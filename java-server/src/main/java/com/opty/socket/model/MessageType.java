/**
 * Message types enum.
 */

package com.opty.socket.model;


/**
 * CODE
 */

/**
 * WebSocket message types.
 */
public enum MessageType {
    CONNECT,
    MESSAGE,
    DISCONNECT,
    ERROR,
    SESSION_QUEUE_UPDATE
}
