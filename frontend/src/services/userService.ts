import api from './api';
import { User, UserSummary } from '../types/user';

export const userService = {
  async getMe(): Promise<User> {
    const res = await api.get('/users/me');
    return res.data.data;
  },

  async searchUsers(query: string): Promise<UserSummary[]> {
    const res = await api.get('/users/search', { params: { q: query } });
    return res.data.data;
  },

  async updateProfile(data: { displayName?: string; avatarUrl?: string }): Promise<User> {
    const res = await api.put('/users/profile', data);
    return res.data.data;
  },
};
