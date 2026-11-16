import api from './api';
import { Message, PagedResponse, SendMessageRequest } from '../types/message';

export const messageService = {
  async getMessages(conversationId: string, before?: string, limit: number = 30): Promise<PagedResponse<Message>> {
    const res = await api.get(`/conversations/${conversationId}/messages`, {
      params: { before, limit },
    });
    return res.data.data;
  },

  async sendMessage(conversationId: string, data: SendMessageRequest): Promise<Message> {
    const res = await api.post(`/conversations/${conversationId}/messages`, data);
    return res.data.data;
  },

  async markAsRead(conversationId: string, messageId?: string): Promise<void> {
    await api.post(`/conversations/${conversationId}/read`, { messageId });
  },
};
