import React, { useEffect, useRef } from 'react';
import { Message } from '../../types/message';
import { MessageBubble } from './MessageBubble';
import { Skeleton } from '../ui/Skeleton';
import { useAuthStore } from '../../stores/authStore';
import { MessageSquare, ArrowDown } from 'lucide-react';
import { Button } from '../ui/Button';

interface MessageListProps {
  messages: Message[];
  isLoading: boolean;
  isLoadingMore: boolean;
  hasMore: boolean;
  onLoadMore: () => void;
  isGroup: boolean;
}

export const MessageList: React.FC<MessageListProps> = ({
  messages,
  isLoading,
  isLoadingMore,
  hasMore,
  onLoadMore,
  isGroup,
}) => {
  const currentUserId = useAuthStore((s) => s.user?.id);
  const containerRef = useRef<HTMLDivElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const prevScrollHeightRef = useRef<number>(0);
  const isNearBottomRef = useRef<boolean>(true);
  const [showScrollBottom, setShowScrollBottom] = React.useState(false);

  const checkIfNearBottom = () => {
    if (!containerRef.current) return true;
    const { scrollTop, scrollHeight, clientHeight } = containerRef.current;
    return scrollHeight - scrollTop - clientHeight < 150;
  };

  const scrollToBottom = (behavior: ScrollBehavior = 'smooth') => {
    bottomRef.current?.scrollIntoView({ behavior });
  };

  const handleScroll = () => {
    if (!containerRef.current) return;
    const { scrollTop, scrollHeight } = containerRef.current;
    const nearBottom = checkIfNearBottom();
    isNearBottomRef.current = nearBottom;
    setShowScrollBottom(!nearBottom);

    // If scrolled to top and more messages available, load previous page
    if (scrollTop === 0 && hasMore && !isLoadingMore) {
      prevScrollHeightRef.current = scrollHeight;
      onLoadMore();
    }
  };

  // Preserve scroll offset when older messages are loaded into the top
  useEffect(() => {
    if (containerRef.current && prevScrollHeightRef.current > 0) {
      const newScrollHeight = containerRef.current.scrollHeight;
      containerRef.current.scrollTop = newScrollHeight - prevScrollHeightRef.current;
      prevScrollHeightRef.current = 0;
    }
  }, [messages.length]);

  // Scroll to bottom on initial load or if user is already near bottom
  useEffect(() => {
    if (isNearBottomRef.current) {
      scrollToBottom('auto');
    }
  }, [messages]);

  if (isLoading) {
    return (
      <div className="flex-1 p-6 space-y-4 overflow-y-auto">
        <div className="flex justify-start"><Skeleton className="h-10 w-48 rounded-2xl" /></div>
        <div className="flex justify-end"><Skeleton className="h-12 w-64 rounded-2xl" /></div>
        <div className="flex justify-start"><Skeleton className="h-14 w-56 rounded-2xl" /></div>
        <div className="flex justify-end"><Skeleton className="h-10 w-40 rounded-2xl" /></div>
      </div>
    );
  }

  if (messages.length === 0) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-8 text-center">
        <div className="h-16 w-16 rounded-3xl bg-muted/70 flex items-center justify-center text-muted-foreground mb-4">
          <MessageSquare className="h-8 w-8" />
        </div>
        <h4 className="text-base font-semibold">No messages yet</h4>
        <p className="text-xs text-muted-foreground max-w-xs mt-1">
          Say hello and start the conversation! Messages are persistent and synchronized in real time.
        </p>
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      onScroll={handleScroll}
      className="flex-1 overflow-y-auto px-4 py-4 space-y-1 relative"
    >
      {/* Loading older messages indicator */}
      {isLoadingMore && (
        <div className="flex justify-center py-2">
          <div className="flex items-center gap-2 text-xs text-muted-foreground bg-muted/60 px-3 py-1 rounded-full">
            <span className="w-2 h-2 rounded-full bg-primary animate-ping" />
            Loading earlier messages...
          </div>
        </div>
      )}

      {hasMore && !isLoadingMore && (
        <div className="flex justify-center py-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={onLoadMore}
            className="text-xs text-muted-foreground hover:text-foreground"
          >
            Load earlier messages
          </Button>
        </div>
      )}

      {messages.map((message) => (
        <MessageBubble
          key={message.id || message.clientMessageId}
          message={message}
          isSelf={message.senderId === currentUserId}
          showSender={isGroup}
        />
      ))}

      <div ref={bottomRef} className="h-1" />

      {/* Scroll to bottom button */}
      {showScrollBottom && (
        <button
          onClick={() => scrollToBottom('smooth')}
          className="absolute bottom-4 right-6 p-2 rounded-full bg-primary text-primary-foreground shadow-lg hover:bg-primary-hover transition-all animate-bounce"
          title="Scroll to bottom"
        >
          <ArrowDown className="h-4 w-4" />
        </button>
      )}
    </div>
  );
};
