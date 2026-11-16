import React from 'react';
import { Message } from '../../types/message';
import { Avatar } from '../ui/Avatar';
import { formatMessageTime } from '../../utils/formatters';
import { Check, CheckCheck, Clock } from 'lucide-react';
import { cn } from '../../utils/cn';

interface MessageBubbleProps {
  message: Message;
  isSelf: boolean;
  showSender: boolean;
}

export const MessageBubble: React.FC<MessageBubbleProps> = ({
  message,
  isSelf,
  showSender,
}) => {
  if (message.type === 'SYSTEM') {
    return (
      <div className="flex justify-center my-3">
        <span className="rounded-full bg-muted/60 px-3.5 py-1 text-xs text-muted-foreground border border-border/40 select-none">
          {message.content}
        </span>
      </div>
    );
  }

  const renderStatus = () => {
    if (!isSelf) return null;

    switch (message.status) {
      case 'SENDING':
        return <Clock className="h-3 w-3 text-primary-foreground/70 animate-spin" />;
      case 'SENT':
        return <Check className="h-3.5 w-3.5 text-primary-foreground/80" />;
      case 'DELIVERED':
        return <CheckCheck className="h-3.5 w-3.5 text-primary-foreground/80" />;
      case 'READ':
        return <CheckCheck className="h-3.5 w-3.5 text-sky-200 dark:text-sky-300" />;
      default:
        return <Check className="h-3.5 w-3.5 text-primary-foreground/80" />;
    }
  };

  return (
    <div
      className={cn(
        'flex gap-2.5 my-1.5 group select-text',
        isSelf ? 'justify-end' : 'justify-start'
      )}
    >
      {!isSelf && (
        <Avatar
          src={message.senderAvatarUrl}
          name={message.senderName}
          size="sm"
          className="mt-1"
        />
      )}

      <div
        className={cn(
          'flex flex-col max-w-[78%] sm:max-w-[65%]',
          isSelf ? 'items-end' : 'items-start'
        )}
      >
        {!isSelf && showSender && (
          <span className="text-[11px] font-semibold text-muted-foreground ml-1 mb-1">
            {message.senderName}
          </span>
        )}

        <div
          className={cn(
            'rounded-2xl px-4 py-2.5 text-sm shadow-xs break-words transition-all leading-relaxed',
            isSelf
              ? 'bg-primary text-primary-foreground rounded-tr-xs'
              : 'bg-muted/80 text-foreground rounded-tl-xs border border-border/50'
          )}
        >
          <div className="whitespace-pre-wrap">{message.content}</div>

          <div
            className={cn(
              'flex items-center gap-1.5 justify-end mt-1 text-[10px] select-none',
              isSelf ? 'text-primary-foreground/80' : 'text-muted-foreground'
            )}
          >
            <span>{formatMessageTime(message.createdAt)}</span>
            {renderStatus()}
          </div>
        </div>
      </div>
    </div>
  );
};
