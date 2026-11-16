import React from 'react';
import { Conversation } from '../../types/conversation';
import { Avatar } from '../ui/Avatar';
import { Badge } from '../ui/Badge';
import { formatConversationTime } from '../../utils/formatters';
import { usePresenceStore } from '../../stores/presenceStore';
import { useAuthStore } from '../../stores/authStore';
import { Users } from 'lucide-react';
import { cn } from '../../utils/cn';

interface ConversationItemProps {
  conversation: Conversation;
  isActive: boolean;
  onClick: () => void;
}

export const ConversationItem: React.FC<ConversationItemProps> = ({
  conversation,
  isActive,
  onClick,
}) => {
  const currentUserId = useAuthStore((s) => s.user?.id);
  const isUserOnline = usePresenceStore((s) => s.isUserOnline);

  const isDirect = conversation.type === 'DIRECT';
  let isPeerOnline = false;

  if (isDirect) {
    const peerId = conversation.memberIds.find((id) => id !== currentUserId);
    if (peerId) {
      isPeerOnline = isUserOnline(peerId) || !!conversation.online;
    }
  }

  const lastMsg = conversation.lastMessage;
  const timeText = formatConversationTime(conversation.lastMessageAt || conversation.updatedAt);

  return (
    <button
      onClick={onClick}
      className={cn(
        'w-full flex items-center gap-3 p-3 rounded-2xl transition-all text-left relative group select-none',
        isActive
          ? 'bg-primary/10 text-foreground font-medium shadow-xs'
          : 'hover:bg-muted/60 text-muted-foreground hover:text-foreground'
      )}
    >
      <div className="relative">
        <Avatar
          src={conversation.avatarUrl}
          name={conversation.name}
          size="md"
          isOnline={isDirect ? isPeerOnline : undefined}
        />
        {!isDirect && (
          <span className="absolute -bottom-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full bg-secondary text-[10px] text-muted-foreground ring-1 ring-background">
            <Users className="h-2.5 w-2.5" />
          </span>
        )}
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between gap-1 mb-0.5">
          <span
            className={cn(
              'text-sm truncate font-medium',
              isActive ? 'text-primary font-semibold' : 'text-foreground'
            )}
          >
            {conversation.name}
          </span>
          <span className="text-[11px] text-muted-foreground whitespace-nowrap">
            {timeText}
          </span>
        </div>

        <div className="flex items-center justify-between gap-2">
          <p className="text-xs text-muted-foreground truncate">
            {lastMsg ? (
              <>
                {lastMsg.senderId === currentUserId ? 'You: ' : ''}
                {lastMsg.content}
              </>
            ) : (
              <span className="italic text-muted-foreground/70">No messages yet</span>
            )}
          </p>

          {conversation.unreadCount > 0 && (
            <Badge variant="default" className="h-5 px-1.5 text-[11px] flex-shrink-0 animate-pulse-fast">
              {conversation.unreadCount}
            </Badge>
          )}
        </div>
      </div>
    </button>
  );
};
