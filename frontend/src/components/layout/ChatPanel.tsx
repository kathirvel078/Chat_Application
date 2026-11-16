import React, { useEffect, useState, useCallback } from 'react';
import { Conversation } from '../../types/conversation';
import { Message } from '../../types/message';
import { Avatar } from '../ui/Avatar';
import { Button } from '../ui/Button';
import { MessageList } from '../chat/MessageList';
import { MessageInput } from '../chat/MessageInput';
import { TypingIndicator } from '../chat/TypingIndicator';
import { messageService } from '../../services/messageService';
import { websocketService } from '../../services/websocketService';
import { useAuthStore } from '../../stores/authStore';
import { useChatStore } from '../../stores/chatStore';
import { usePresenceStore } from '../../stores/presenceStore';
import { formatLastSeen } from '../../utils/formatters';
import {
  ChevronLeft,
  Info,
  Users,
  MessageSquare,
} from 'lucide-react';

interface ChatPanelProps {
  conversation: Conversation | null;
  onBackMobile: () => void;
  onMessageSentOrReceived?: () => void;
}

export const ChatPanel: React.FC<ChatPanelProps> = ({
  conversation,
  onBackMobile,
  onMessageSentOrReceived,
}) => {
  const currentUserId = useAuthStore((s) => s.user?.id);
  const { isDetailsOpen, toggleDetails, typingMap, setTyping } = useChatStore();
  const isUserOnline = usePresenceStore((s) => s.isUserOnline);

  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [nextCursor, setNextCursor] = useState<string | undefined>(undefined);

  // Fetch initial messages when active conversation changes
  useEffect(() => {
    if (!conversation) {
      setMessages([]);
      return;
    }

    let isMounted = true;
    setIsLoading(true);

    messageService
      .getMessages(conversation.id, undefined, 30)
      .then((data) => {
        if (!isMounted) return;
        setMessages(data.items);
        setHasMore(data.hasMore);
        setNextCursor(data.nextCursor);
        setIsLoading(false);

        // Mark as read immediately
        messageService.markAsRead(conversation.id);
        websocketService.sendReadReceipt(conversation.id);
        if (onMessageSentOrReceived) onMessageSentOrReceived();
      })
      .catch((err) => {
        console.error('Failed to load messages', err);
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [conversation?.id]);

  // Subscribe to real-time conversation messages, typing, and read receipts
  useEffect(() => {
    if (!conversation) return;

    const convId = conversation.id;

    // 1. Subscribe to new messages
    const unsubscribeMessages = websocketService.subscribeToConversation(
      convId,
      (newMsg: Message) => {
        setMessages((prev) => {
          // Prevent duplicates by ID or clientMessageId
          if (prev.some((m) => m.id === newMsg.id || (newMsg.clientMessageId && m.clientMessageId === newMsg.clientMessageId))) {
            return prev.map((m) => (m.clientMessageId === newMsg.clientMessageId ? newMsg : m));
          }
          return [...prev, newMsg];
        });

        // Mark as read if active
        if (newMsg.senderId !== currentUserId) {
          messageService.markAsRead(convId, newMsg.id);
          websocketService.sendReadReceipt(convId, newMsg.id);
        }

        if (onMessageSentOrReceived) onMessageSentOrReceived();
      }
    );

    // 2. Subscribe to typing indicator events
    const unsubscribeTyping = websocketService.subscribeToTyping(
      convId,
      (event) => {
        if (event.userId !== currentUserId) {
          setTyping(convId, event.userId, event.displayName, event.typing);
        }
      }
    );

    // 3. Subscribe to read receipts
    const unsubscribeReceipts = websocketService.subscribeToReceipts(
      convId,
      (receipt) => {
        setMessages((prev) =>
          prev.map((m) => {
            if (m.senderId === currentUserId) {
              return { ...m, status: 'READ' };
            }
            return m;
          })
        );
      }
    );

    return () => {
      unsubscribeMessages();
      unsubscribeTyping();
      unsubscribeReceipts();
    };
  }, [conversation?.id, currentUserId]);

  const handleLoadMore = useCallback(async () => {
    if (!conversation || !hasMore || isLoadingMore || !nextCursor) return;

    setIsLoadingMore(true);
    try {
      const data = await messageService.getMessages(conversation.id, nextCursor, 30);
      setMessages((prev) => [...data.items, ...prev]);
      setHasMore(data.hasMore);
      setNextCursor(data.nextCursor);
    } catch (err) {
      console.error('Failed to load older messages', err);
    } finally {
      setIsLoadingMore(false);
    }
  }, [conversation, hasMore, isLoadingMore, nextCursor]);

  const handleSendMessage = async (content: string) => {
    if (!conversation) return;

    const clientMsgId = 'msg_' + Math.random().toString(36).substring(2, 11) + Date.now();

    // Optimistic message
    const optimisticMsg: Message = {
      id: clientMsgId,
      conversationId: conversation.id,
      senderId: currentUserId || '',
      senderName: 'You',
      content,
      type: 'TEXT',
      clientMessageId: clientMsgId,
      status: 'SENDING',
      createdAt: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, optimisticMsg]);

    try {
      // Send via STOMP WebSocket if connected, fallback to REST
      if (websocketService.isConnected()) {
        websocketService.sendMessage(conversation.id, content, clientMsgId);
      } else {
        const saved = await messageService.sendMessage(conversation.id, {
          content,
          clientMessageId: clientMsgId,
        });
        setMessages((prev) =>
          prev.map((m) => (m.clientMessageId === clientMsgId ? saved : m))
        );
      }
      if (onMessageSentOrReceived) onMessageSentOrReceived();
    } catch (err) {
      console.error('Error sending message', err);
      // Remove optimistic or flag error
      setMessages((prev) => prev.filter((m) => m.clientMessageId !== clientMsgId));
    }
  };

  if (!conversation) {
    return (
      <main className="flex-1 hidden md:flex flex-col items-center justify-center p-8 text-center bg-background/50">
        <div className="h-20 w-20 rounded-3xl bg-primary/10 flex items-center justify-center text-primary mb-4 shadow-sm">
          <MessageSquare className="h-10 w-10" />
        </div>
        <h3 className="text-xl font-semibold">Select a conversation</h3>
        <p className="text-sm text-muted-foreground max-w-sm mt-1">
          Choose a conversation from the left sidebar or start a new chat with a colleague
        </p>
      </main>
    );
  }

  const isDirect = conversation.type === 'DIRECT';
  let isPeerOnline = false;
  let peerSubtitle = '';

  if (isDirect) {
    const peerId = conversation.memberIds.find((id) => id !== currentUserId);
    if (peerId) {
      isPeerOnline = isUserOnline(peerId) || !!conversation.online;
      const peerSummary = conversation.members?.find((m) => m.id === peerId);
      peerSubtitle = isPeerOnline
        ? 'Online now'
        : formatLastSeen(peerSummary?.lastSeenAt);
    }
  } else {
    peerSubtitle = `${conversation.memberIds.length} members`;
  }

  // Get active typing users for this conversation
  const typingEntries = typingMap[conversation.id] || {};
  const typingNames = Object.values(typingEntries).map((t) => t.displayName);

  return (
    <main className="flex-1 flex flex-col h-full bg-background relative overflow-hidden">
      {/* Active Header */}
      <header className="h-16 border-b border-border bg-card/80 backdrop-blur-md px-4 flex items-center justify-between flex-shrink-0 z-10 shadow-2xs">
        <div className="flex items-center gap-3 min-w-0">
          <Button
            variant="ghost"
            size="icon"
            onClick={onBackMobile}
            className="md:hidden -ml-2 text-muted-foreground"
          >
            <ChevronLeft className="h-6 w-6" />
          </Button>

          <Avatar
            src={conversation.avatarUrl}
            name={conversation.name}
            size="md"
            isOnline={isDirect ? isPeerOnline : undefined}
          />

          <div className="min-w-0">
            <h2 className="text-sm font-semibold truncate leading-tight">
              {conversation.name}
            </h2>
            <p className="text-xs text-muted-foreground truncate flex items-center gap-1.5 mt-0.5">
              {isDirect && isPeerOnline && (
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse-fast" />
              )}
              {peerSubtitle}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1">
          <Button
            variant="ghost"
            size="icon"
            onClick={toggleDetails}
            className={isDetailsOpen ? 'text-primary bg-primary/10' : 'text-muted-foreground'}
            title="Conversation details"
          >
            <Info className="h-5 w-5" />
          </Button>
        </div>
      </header>

      {/* Message History */}
      <MessageList
        messages={messages}
        isLoading={isLoading}
        isLoadingMore={isLoadingMore}
        hasMore={hasMore}
        onLoadMore={handleLoadMore}
        isGroup={!isDirect}
      />

      {/* Real-time Typing Indicator */}
      <TypingIndicator names={typingNames} />

      {/* Composer */}
      <MessageInput
        conversationId={conversation.id}
        onSendMessage={handleSendMessage}
        disabled={isLoading}
      />
    </main>
  );
};
