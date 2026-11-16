import api from './api';
import { Conversation, CreateGroupConversationRequest } from '../types/conversation';

export const conversationService = {
  async getConversations(): Promise<Conversation[]> {
    const res = await api.get('/conversations');
    return res.data.data;
  },

  async getConversation(id: string): Promise<Conversation> {
    const res = await api.get(`/conversations/${id}`);
    return res.data.data;
  },

  async createDirectConversation(recipientId: string): Promise<Conversation> {
    const res = await api.post('/conversations/direct', { recipientId });
    return res.data.data;
  },

  async createGroupConversation(data: CreateGroupConversationRequest): Promise<Conversation> {
    const res = await api.post('/conversations/group', data);
    return res.data.data;
  },

  async addMember(conversationId: string, userId: string): Promise<Conversation> {
    const res = await api.post(`/conversations/${conversationId}/members`, { userId });
    return res.data.data;
  },

  async removeMember(conversationId: string, userId: string): Promise<Conversation> {
    const res = await api.delete(`/conversations/${conversationId}/members/${userId}`);
    return res.data.data;
  },
};
