import { create } from 'zustand';

interface PresenceState {
  onlineUsers: Record<string, boolean>;
  setUserOnline: (userId: string, isOnline: boolean) => void;
  setBulkPresence: (presenceMap: Record<string, boolean>) => void;
  isUserOnline: (userId: string) => boolean;
}

export const usePresenceStore = create<PresenceState>((set, get) => ({
  onlineUsers: {},

  setUserOnline: (userId, isOnline) => {
    set((state) => ({
      onlineUsers: {
        ...state.onlineUsers,
        [userId]: isOnline,
      },
    }));
  },

  setBulkPresence: (presenceMap) => {
    set((state) => ({
      onlineUsers: {
        ...state.onlineUsers,
        ...presenceMap,
      },
    }));
  },

  isUserOnline: (userId) => {
    return !!get().onlineUsers[userId];
  },
}));
