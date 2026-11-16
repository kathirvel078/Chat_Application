export interface User {
  id: string;
  displayName: string;
  email: string;
  avatarUrl?: string;
  status: 'ONLINE' | 'OFFLINE';
  lastSeenAt?: string;
  createdAt: string;
}

export interface UserSummary {
  id: string;
  displayName: string;
  email: string;
  avatarUrl?: string;
  status?: string;
  lastSeenAt?: string;
}
