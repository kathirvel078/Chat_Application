import { create } from 'zustand';

interface ChatState {
  activeConversationId: string | null;
  setActiveConversationId: (id: string | null) => void;
  isDetailsOpen: boolean;
  setDetailsOpen: (open: boolean) => void;
  toggleDetails: () => void;
  typingMap: Record<string, Record<string, { displayName: string; timer: any }>>;
  setTyping: (conversationId: string, userId: string, displayName: string, isTyping: boolean) => void;
  clearTyping: (conversationId: string) => void;
}

export const useChatStore = create<ChatState>((set, get) => ({
  activeConversationId: null,
  setActiveConversationId: (id) => set({ activeConversationId: id }),
  isDetailsOpen: false,
  setDetailsOpen: (open) => set({ isDetailsOpen: open }),
  toggleDetails: () => set((state) => ({ isDetailsOpen: !state.isDetailsOpen })),
  typingMap: {},

  setTyping: (conversationId, userId, displayName, isTyping) => {
    const { typingMap } = get();
    const convTyping = { ...(typingMap[conversationId] || {}) };

    if (convTyping[userId]?.timer) {
      clearTimeout(convTyping[userId].timer);
    }

    if (!isTyping) {
      delete convTyping[userId];
    } else {
      // Automatically stop typing indicator after 4 seconds of inactivity
      const timer = setTimeout(() => {
        get().setTyping(conversationId, userId, displayName, false);
      }, 4000);

      convTyping[userId] = { displayName, timer };
    }

    set({
      typingMap: {
        ...typingMap,
        [conversationId]: convTyping,
      },
    });
  },

  clearTyping: (conversationId) => {
    const { typingMap } = get();
    if (typingMap[conversationId]) {
      Object.values(typingMap[conversationId]).forEach((item) => {
        if (item.timer) clearTimeout(item.timer);
      });
      const newMap = { ...typingMap };
      delete newMap[conversationId];
      set({ typingMap: newMap });
    }
  },
}));
