import api, { setAccessToken } from './api';
import { AuthResponse, TokenRefreshResponse } from '../types/auth';

export const authService = {
  async register(data: { displayName: string; email: string; password: string }): Promise<AuthResponse> {
    const res = await api.post('/auth/register', data);
    const authData: AuthResponse = res.data.data;
    setAccessToken(authData.accessToken);
    return authData;
  },

  async login(data: { email: string; password: string }): Promise<AuthResponse> {
    const res = await api.post('/auth/login', data);
    const authData: AuthResponse = res.data.data;
    setAccessToken(authData.accessToken);
    return authData;
  },

  async refreshToken(): Promise<TokenRefreshResponse> {
    const res = await api.post('/auth/refresh');
    const refreshData: TokenRefreshResponse = res.data.data;
    setAccessToken(refreshData.accessToken);
    return refreshData;
  },

  async logout(): Promise<void> {
    try {
      await api.post('/auth/logout');
    } finally {
      setAccessToken(null);
    }
  },
};
