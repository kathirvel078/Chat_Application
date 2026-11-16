import { UserSummary } from './user';

export type ConversationType = 'DIRECT' | 'GROUP';

export interface LastMessage {
  id: string;
  senderId: string;
  senderName: string;
  content: string;
  type: string;
  createdAt: string;
}

export interface Conversation {
  id: string;
  type: ConversationType;
  name: string;
  avatarUrl?: string;
  memberIds: string[];
  adminIds: string[];
  members: UserSummary[];
  createdBy: string;
  lastMessage?: LastMessage;
  lastMessageAt?: string;
  unreadCount: number;
  online?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDirectConversationRequest {
  recipientId: string;
}

export interface CreateGroupConversationRequest {
  name: string;
  memberIds: string[];
  avatarUrl?: string;
}
