import { useState, useCallback, useEffect, useRef } from 'react';
import { useWebSocket, WebSocketMessage } from './useWebSocket';

export interface ChatMessage {
  id: number;
  type: 'client' | 'supervisor' | 'system';
  message: string;
  time: string;
  senderName?: string;
}

export interface UseClientChatReturn {
  messages: ChatMessage[];
  sendMessage: (text: string) => void;
  isConnected: boolean;
  sessionId: string | null;
  disconnect: () => void;
  connect: () => void;
}

const WS_URL = import.meta.env.VITE_WS_URL;

export const useClientChat = (): UseClientChatReturn => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [messageCounter, setMessageCounter] = useState(0);
  const [shouldConnect, setShouldConnect] = useState(false);
  const hasWelcomeMessage = useRef(false);

  const handleMessage = useCallback((wsMessage: WebSocketMessage) => {
    console.log('Received message:', wsMessage);

    switch (wsMessage.type) {
      case 'CONNECT':
        // Client receives sessionId on connection
        if (wsMessage.payload?.sessionId) {
          setSessionId(wsMessage.payload.sessionId as string);

          // Only add welcome message once
          if (!hasWelcomeMessage.current) {
            hasWelcomeMessage.current = true;
            const welcomeMessage: ChatMessage = {
              id: Date.now(),
              type: 'system',
              message: 'Bem-vindo ao suporte Opty! Em que posso ajudar?',
              time: new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
            };
            setMessages((prev) => [...prev, welcomeMessage]);
          }
        }
        // Supervisor joined notification
        else if (wsMessage.payload?.message?.includes('Supervisor')) {
          const systemMessage: ChatMessage = {
            id: Date.now(),
            type: 'system',
            message: wsMessage.payload.message as string,
            time: new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
          };
          setMessages((prev) => [...prev, systemMessage]);
        }
        break;

      case 'MESSAGE':
        // Message from supervisor
        if (wsMessage.from === 'SUPERVISOR' && wsMessage.payload?.text) {
          const newMessage: ChatMessage = {
            id: Date.now(),
            type: 'supervisor',
            message: wsMessage.payload.text as string,
            time: new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
            senderName: 'Supervisor',
          };
          setMessages((prev) => [...prev, newMessage]);
        }
        break;

      case 'DISCONNECT':
        // Supervisor disconnected
        const disconnectMessage: ChatMessage = {
          id: Date.now(),
          type: 'system',
          message: 'O supervisor saiu da conversa',
          time: new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
        };
        setMessages((prev) => [...prev, disconnectMessage]);
        break;

      case 'ERROR':
        // Error from server
        const errorMessage: ChatMessage = {
          id: Date.now(),
          type: 'system',
          message: `Erro: ${wsMessage.payload?.error || 'Erro desconhecido'}`,
          time: new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
        };
        setMessages((prev) => [...prev, errorMessage]);
        break;
    }
  }, []);

  const handleOpen = useCallback(() => {
    console.log('Client connected to WebSocket');
  }, []);

  const handleClose = useCallback(() => {
    console.log('Client disconnected from WebSocket');
  }, []);

  const handleError = useCallback((error: Event) => {
    console.error('WebSocket error:', error);
  }, []);

  const { sendMessage: wsSendMessage, isConnected, disconnect } = useWebSocket({
    url: `${WS_URL}/ws/client`,
    shouldConnect,
    onMessage: handleMessage,
    onOpen: handleOpen,
    onClose: handleClose,
    onError: handleError,
  });

  const connect = useCallback(() => {
    setShouldConnect(true);
  }, []);

  const sendMessage = useCallback(
    (text: string) => {
      if (!sessionId) {
        console.warn('Cannot send message: No sessionId');
        return;
      }

      if (!text.trim()) {
        return;
      }

      // Add message to local state immediately
      const newMessage: ChatMessage = {
        id: messageCounter,
        type: 'client',
        message: text,
        time: new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
        senderName: 'Você',
      };
      setMessages((prev) => [...prev, newMessage]);
      setMessageCounter((prev) => prev + 1);

      // Send via WebSocket
      const wsMessage: WebSocketMessage = {
        sessionId,
        from: 'CLIENT',
        type: 'MESSAGE',
        payload: {
          text,
          timestamp: new Date().toISOString(),
        },
      };
      wsSendMessage(wsMessage);
    },
    [sessionId, wsSendMessage, messageCounter]
  );

  // Cleanup on unmount only
  useEffect(() => {
    return () => {
      console.log('Client chat hook unmounting, disconnecting...');
      disconnect();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return {
    messages,
    sendMessage,
    isConnected,
    sessionId,
    disconnect,
    connect,
  };
};