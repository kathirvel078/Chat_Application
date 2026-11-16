import React, { useState, useEffect } from 'react';
import { Dialog } from '../ui/Dialog';
import { Input } from '../ui/Input';
import { Avatar } from '../ui/Avatar';
import { Button } from '../ui/Button';
import { userService } from '../../services/userService';
import { conversationService } from '../../services/conversationService';
import { useChatStore } from '../../stores/chatStore';
import { UserSummary } from '../../types/user';
import { Search, Loader2 } from 'lucide-react';

interface NewChatDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConversationCreated?: () => void;
}

export const NewChatDialog: React.FC<NewChatDialogProps> = ({
  isOpen,
  onClose,
  onConversationCreated,
}) => {
  const [query, setQuery] = useState('');
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const setActiveConversationId = useChatStore((s) => s.setActiveConversationId);

  useEffect(() => {
    if (!isOpen) {
      setQuery('');
      setUsers([]);
      return;
    }

    const timer = setTimeout(async () => {
      if (!query.trim()) {
        setUsers([]);
        return;
      }
      setLoading(true);
      try {
        const results = await userService.searchUsers(query.trim());
        setUsers(results);
      } catch (err) {
        console.error('Failed to search users', err);
      } finally {
        setLoading(false);
      }
    }, 250);

    return () => clearTimeout(timer);
  }, [query, isOpen]);

  const handleStartChat = async (recipientId: string) => {
    setCreating(true);
    try {
      const conv = await conversationService.createDirectConversation(recipientId);
      setActiveConversationId(conv.id);
      if (onConversationCreated) onConversationCreated();
      onClose();
    } catch (err) {
      console.error('Failed to start conversation', err);
    } finally {
      setCreating(false);
    }
  };

  return (
    <Dialog
      isOpen={isOpen}
      onClose={onClose}
      title="Start New Conversation"
      description="Search for a registered colleague to send a direct message"
    >
      <div className="space-y-4">
        <div className="relative">
          <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search by name or email..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="pl-9"
            autoFocus
          />
        </div>

        <div className="max-h-64 overflow-y-auto space-y-1 divide-y divide-border/40">
          {loading && (
            <div className="flex items-center justify-center py-6 text-sm text-muted-foreground">
              <Loader2 className="h-5 w-5 animate-spin mr-2" />
              Searching users...
            </div>
          )}

          {!loading && query && users.length === 0 && (
            <div className="py-6 text-center text-sm text-muted-foreground">
              No registered users found matching "{query}"
            </div>
          )}

          {!loading && !query && (
            <div className="py-6 text-center text-xs text-muted-foreground">
              Type a name or email address to search
            </div>
          )}

          {!loading &&
            users.map((u) => (
              <button
                key={u.id}
                disabled={creating}
                onClick={() => handleStartChat(u.id)}
                className="w-full flex items-center justify-between p-2.5 rounded-xl hover:bg-muted/70 transition-colors text-left"
              >
                <div className="flex items-center space-x-3">
                  <Avatar src={u.avatarUrl} name={u.displayName} size="md" />
                  <div>
                    <div className="text-sm font-medium">{u.displayName}</div>
                    <div className="text-xs text-muted-foreground">{u.email}</div>
                  </div>
                </div>
                <Button size="sm" variant="secondary" disabled={creating}>
                  Chat
                </Button>
              </button>
            ))}
        </div>
      </div>
    </Dialog>
  );
};
