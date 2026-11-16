import React, { useEffect, useState, useCallback } from 'react';
import { Sidebar } from './Sidebar';
import { ChatPanel } from './ChatPanel';
import { DetailsPanel } from './DetailsPanel';
import { Conversation } from '../../types/conversation';
import { conversationService } from '../../services/conversationService';
import { websocketService } from '../../services/websocketService';
import { useChatStore } from '../../stores/chatStore';
import { usePresenceStore } from '../../stores/presenceStore';
import { useAuthStore } from '../../stores/authStore';

export const AppLayout: React.FC = () => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [loading, setLoading] = useState(true);

  const { activeConversationId, setActiveConversationId, isDetailsOpen, setDetailsOpen } =
    useChatStore();
  const setUserOnline = usePresenceStore((s) => s.setUserOnline);
  const currentUserId = useAuthStore((s) => s.user?.id);

  const loadConversations = useCallback(async () => {
    try {
      const data = await conversationService.getConversations();
      setConversations(data);
      // Auto select first conversation if on desktop and none selected
      if (!activeConversationId && data.length > 0 && window.innerWidth >= 768) {
        setActiveConversationId(data[0].id);
      }
    } catch (err) {
      console.error('Failed to load conversations', err);
    } finally {
      setLoading(false);
    }
  }, [activeConversationId, setActiveConversationId]);

  useEffect(() => {
    loadConversations();
  }, [loadConversations]);

  // Global WebSocket subscriptions for presence and user-queue updates
  useEffect(() => {
    const unsubPresence = websocketService.subscribeToPresence((event) => {
      setUserOnline(event.userId, event.status === 'ONLINE');
    });

    const unsubUserQueue = websocketService.subscribeToUserQueue((msg) => {
      // Refresh conversation list to get latest snippets, unread badges, or newly created chats
      loadConversations();
    });

    return () => {
      unsubPresence();
      unsubUserQueue();
    };
  }, [loadConversations, setUserOnline]);

  const activeConversation =
    conversations.find((c) => c.id === activeConversationId) || null;

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-background">
      {/* Left Sidebar (Conversations) */}
      <div
        className={`${
          activeConversationId ? 'hidden md:flex' : 'flex'
        } h-full flex-shrink-0 w-full md:w-auto`}
      >
        <Sidebar
          conversations={conversations}
          isLoading={loading}
          onRefreshConversations={loadConversations}
        />
      </div>

      {/* Main Central Chat Panel */}
      <div
        className={`${
          !activeConversationId ? 'hidden md:flex' : 'flex'
        } flex-1 h-full min-w-0`}
      >
        <ChatPanel
          conversation={activeConversation}
          onBackMobile={() => setActiveConversationId(null)}
          onMessageSentOrReceived={loadConversations}
        />
      </div>

      {/* Right Details Panel (Desktop) */}
      {isDetailsOpen && activeConversation && (
        <DetailsPanel
          conversation={activeConversation}
          onClose={() => setDetailsOpen(false)}
          onConversationUpdated={loadConversations}
        />
      )}
    </div>
  );
};
