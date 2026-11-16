export interface TypingNotification {
  conversationId: string;
  userId: string;
  displayName: string;
  typing: boolean;
  timestamp: string;
}

export interface PresenceNotification {
  userId: string;
  status: 'ONLINE' | 'OFFLINE';
  timestamp: string;
}

export interface ReadReceiptNotification {
  conversationId: string;
  userId: string;
  messageId: string;
  readAt: string;
}
