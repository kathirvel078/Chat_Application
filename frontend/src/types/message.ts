export type MessageType = 'TEXT' | 'SYSTEM';
export type MessageStatus = 'SENDING' | 'SENT' | 'DELIVERED' | 'READ';

export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  senderAvatarUrl?: string;
  content: string;
  type: MessageType;
  clientMessageId?: string;
  status: MessageStatus;
  readBy?: Record<string, string>;
  createdAt: string;
  editedAt?: string;
  deletedAt?: string;
}

export interface PagedResponse<T> {
  items: T[];
  nextCursor?: string;
  hasMore: boolean;
  limit: number;
}

export interface SendMessageRequest {
  content: string;
  clientMessageId?: string;
}
