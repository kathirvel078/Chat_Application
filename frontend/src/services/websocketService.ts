import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Message } from '../types/message';
import { PresenceNotification, TypingNotification } from '../types/websocket';

class WebSocketService {
  private client: Client | null = null;
  private connected = false;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private connectionPromise: Promise<void> | null = null;
  private heartbeatInterval: any = null;

  public connect(token: string): Promise<void> {
    if (this.connected && this.client?.active) {
      return Promise.resolve();
    }
    if (this.connectionPromise) {
      return this.connectionPromise;
    }

    this.connectionPromise = new Promise((resolve, reject) => {
      this.client = new Client({
        webSocketFactory: () => new SockJS('/ws'),
        connectHeaders: {
          Authorization: `Bearer ${token}`,
          token: token,
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 40000,
        heartbeatOutgoing: 40000,
        debug: (str) => {
          // console.debug('[STOMP]', str);
        },
        onConnect: () => {
          this.connected = true;
          this.startHeartbeat();
          resolve();
        },
        onStompError: (frame) => {
          console.error('STOMP protocol error: ' + frame.headers['message']);
          this.connected = false;
          reject(new Error(frame.headers['message'] || 'STOMP connection error'));
        },
        onWebSocketClose: () => {
          this.connected = false;
          this.stopHeartbeat();
        },
      });

      this.client.activate();
    });

    return this.connectionPromise;
  }

  public disconnect() {
    this.stopHeartbeat();
    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.subscriptions.clear();
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    this.connected = false;
    this.connectionPromise = null;
  }

  public isConnected(): boolean {
    return this.connected;
  }

  private startHeartbeat() {
    this.stopHeartbeat();
    this.heartbeatInterval = setInterval(() => {
      if (this.connected && this.client) {
        this.client.publish({
          destination: '/app/presence.heartbeat',
          body: JSON.stringify({ timestamp: Date.now() }),
        });
      }
    }, 30000);
  }

  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  public subscribeToConversation(
    conversationId: string,
    callback: (message: Message) => void
  ): () => void {
    const topic = `/topic/conversations/${conversationId}`;
    if (!this.client || !this.connected) return () => {};

    const sub = this.client.subscribe(topic, (msg: IMessage) => {
      try {
        const parsed: Message = JSON.parse(msg.body);
        callback(parsed);
      } catch (e) {
        console.error('Failed to parse message from topic ' + topic, e);
      }
    });

    this.subscriptions.set(topic, sub);
    return () => {
      sub.unsubscribe();
      this.subscriptions.delete(topic);
    };
  }

  public subscribeToTyping(
    conversationId: string,
    callback: (event: TypingNotification) => void
  ): () => void {
    const topic = `/topic/conversations/${conversationId}/typing`;
    if (!this.client || !this.connected) return () => {};

    const sub = this.client.subscribe(topic, (msg: IMessage) => {
      try {
        const parsed: TypingNotification = JSON.parse(msg.body);
        callback(parsed);
      } catch (e) {
        console.error('Failed to parse typing event', e);
      }
    });

    this.subscriptions.set(topic, sub);
    return () => {
      sub.unsubscribe();
      this.subscriptions.delete(topic);
    };
  }

  public subscribeToReceipts(
    conversationId: string,
    callback: (event: any) => void
  ): () => void {
    const topic = `/topic/conversations/${conversationId}/receipts`;
    if (!this.client || !this.connected) return () => {};

    const sub = this.client.subscribe(topic, (msg: IMessage) => {
      try {
        const parsed = JSON.parse(msg.body);
        callback(parsed);
      } catch (e) {
        console.error('Failed to parse receipt event', e);
      }
    });

    this.subscriptions.set(topic, sub);
    return () => {
      sub.unsubscribe();
      this.subscriptions.delete(topic);
    };
  }

  public subscribeToPresence(callback: (event: PresenceNotification) => void): () => void {
    const topic = '/topic/presence';
    if (!this.client || !this.connected) return () => {};

    const sub = this.client.subscribe(topic, (msg: IMessage) => {
      try {
        const parsed: PresenceNotification = JSON.parse(msg.body);
        callback(parsed);
      } catch (e) {
        console.error('Failed to parse presence notification', e);
      }
    });

    this.subscriptions.set(topic, sub);
    return () => {
      sub.unsubscribe();
      this.subscriptions.delete(topic);
    };
  }

  public subscribeToUserQueue(callback: (message: any) => void): () => void {
    const destination = '/user/queue/messages';
    if (!this.client || !this.connected) return () => {};

    const sub = this.client.subscribe(destination, (msg: IMessage) => {
      try {
        const parsed = JSON.parse(msg.body);
        callback(parsed);
      } catch (e) {
        console.error('Failed to parse user queue message', e);
      }
    });

    this.subscriptions.set(destination, sub);
    return () => {
      sub.unsubscribe();
      this.subscriptions.delete(destination);
    };
  }

  public sendMessage(conversationId: string, content: string, clientMessageId: string) {
    if (!this.client || !this.connected) {
      throw new Error('WebSocket client is not connected');
    }
    this.client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ conversationId, content, clientMessageId }),
    });
  }

  public sendTyping(conversationId: string, isTyping: boolean) {
    if (!this.client || !this.connected) return;
    this.client.publish({
      destination: '/app/chat.typing',
      body: JSON.stringify({ conversationId, typing: isTyping }),
    });
  }

  public sendReadReceipt(conversationId: string, messageId?: string) {
    if (!this.client || !this.connected) return;
    this.client.publish({
      destination: '/app/chat.read',
      body: JSON.stringify({ conversationId, messageId }),
    });
  }
}

export const websocketService = new WebSocketService();
