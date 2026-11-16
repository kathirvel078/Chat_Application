import React, { useState, useRef, useEffect } from 'react';
import { Send, Smile } from 'lucide-react';
import { Button } from '../ui/Button';
import { websocketService } from '../../services/websocketService';

interface MessageInputProps {
  conversationId: string;
  onSendMessage: (content: string) => Promise<void>;
  disabled?: boolean;
}

export const MessageInput: React.FC<MessageInputProps> = ({
  conversationId,
  onSendMessage,
  disabled,
}) => {
  const [content, setContent] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const typingTimeoutRef = useRef<any>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    // Focus textarea on conversation change
    textareaRef.current?.focus();
    setContent('');
  }, [conversationId]);

  const handleTypingEvent = (typing: boolean) => {
    if (typing !== isTyping) {
      setIsTyping(typing);
      websocketService.sendTyping(conversationId, typing);
    }

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    if (typing) {
      typingTimeoutRef.current = setTimeout(() => {
        setIsTyping(false);
        websocketService.sendTyping(conversationId, false);
      }, 2500);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleSend = async () => {
    const trimmed = content.trim();
    if (!trimmed || disabled) return;

    // Reset typing status immediately
    if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
    setIsTyping(false);
    websocketService.sendTyping(conversationId, false);

    setContent('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }

    try {
      await onSendMessage(trimmed);
    } catch (err) {
      console.error('Failed to send message', err);
      // Restore content on error
      setContent(trimmed);
    }
  };

  const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContent(e.target.value);
    handleTypingEvent(true);

    // Auto resize
    const target = e.target;
    target.style.height = 'auto';
    target.style.height = `${Math.min(target.scrollHeight, 120)}px`;
  };

  return (
    <div className="p-3 border-t border-border bg-card/60 backdrop-blur-md">
      <div className="flex items-end gap-2 bg-background/80 rounded-2xl border border-input p-1.5 focus-within:border-primary/80 focus-within:ring-2 focus-within:ring-primary/20 transition-all shadow-xs">
        <textarea
          ref={textareaRef}
          value={content}
          onChange={handleInput}
          onKeyDown={handleKeyDown}
          onBlur={() => handleTypingEvent(false)}
          placeholder="Write a message... (Press Enter to send, Shift+Enter for new line)"
          disabled={disabled}
          rows={1}
          className="flex-1 max-h-32 min-h-[38px] resize-none bg-transparent px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none disabled:cursor-not-allowed leading-relaxed"
        />

        <Button
          type="button"
          size="icon"
          variant="default"
          onClick={handleSend}
          disabled={!content.trim() || disabled}
          className="rounded-xl h-9 w-9 flex-shrink-0 mb-0.5"
          title="Send message"
        >
          <Send className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
};
