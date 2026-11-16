import React, { useState, useEffect } from 'react';
import { Dialog } from '../ui/Dialog';
import { Input } from '../ui/Input';
import { Avatar } from '../ui/Avatar';
import { Button } from '../ui/Button';
import { userService } from '../../services/userService';
import { conversationService } from '../../services/conversationService';
import { useChatStore } from '../../stores/chatStore';
import { UserSummary } from '../../types/user';
import { Search, X, Users, Check } from 'lucide-react';

interface CreateGroupDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConversationCreated?: () => void;
}

export const CreateGroupDialog: React.FC<CreateGroupDialogProps> = ({
  isOpen,
  onClose,
  onConversationCreated,
}) => {
  const [name, setName] = useState('');
  const [query, setQuery] = useState('');
  const [searchResults, setSearchResults] = useState<UserSummary[]>([]);
  const [selectedUsers, setSelectedUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const setActiveConversationId = useChatStore((s) => s.setActiveConversationId);

  useEffect(() => {
    if (!isOpen) {
      setName('');
      setQuery('');
      setSearchResults([]);
      setSelectedUsers([]);
      setError(null);
      return;
    }

    const timer = setTimeout(async () => {
      if (!query.trim()) {
        setSearchResults([]);
        return;
      }
      setLoading(true);
      try {
        const results = await userService.searchUsers(query.trim());
        setSearchResults(results);
      } catch (err) {
        console.error('Failed to search users', err);
      } finally {
        setLoading(false);
      }
    }, 250);

    return () => clearTimeout(timer);
  }, [query, isOpen]);

  const toggleSelectUser = (user: UserSummary) => {
    if (selectedUsers.some((u) => u.id === user.id)) {
      setSelectedUsers(selectedUsers.filter((u) => u.id !== user.id));
    } else {
      setSelectedUsers([...selectedUsers, user]);
    }
  };

  const handleCreateGroup = async () => {
    if (!name.trim()) {
      setError('Please provide a group name');
      return;
    }
    if (selectedUsers.length === 0) {
      setError('Please select at least one colleague for the group');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const conv = await conversationService.createGroupConversation({
        name: name.trim(),
        memberIds: selectedUsers.map((u) => u.id),
      });
      setActiveConversationId(conv.id);
      if (onConversationCreated) onConversationCreated();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create group');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog
      isOpen={isOpen}
      onClose={onClose}
      title="Create New Group Chat"
      description="Collaborate with multiple team members in one place"
    >
      <div className="space-y-4">
        {error && (
          <div className="rounded-lg bg-destructive/10 border border-destructive/20 p-3 text-xs text-destructive">
            {error}
          </div>
        )}

        <Input
          label="Group Name"
          placeholder="e.g. Design Systems & Product"
          value={name}
          onChange={(e) => setName(e.target.value)}
          autoFocus
        />

        {selectedUsers.length > 0 && (
          <div className="space-y-1.5">
            <label className="text-xs font-medium text-muted-foreground block">
              Selected Members ({selectedUsers.length})
            </label>
            <div className="flex flex-wrap gap-1.5">
              {selectedUsers.map((u) => (
                <span
                  key={u.id}
                  className="inline-flex items-center gap-1.5 rounded-full bg-secondary px-2.5 py-1 text-xs font-medium text-secondary-foreground"
                >
                  <Avatar src={u.avatarUrl} name={u.displayName} size="xs" />
                  <span>{u.displayName}</span>
                  <button
                    onClick={() => toggleSelectUser(u)}
                    className="hover:text-destructive"
                  >
                    <X className="h-3 w-3" />
                  </button>
                </span>
              ))}
            </div>
          </div>
        )}

        <div className="space-y-1.5">
          <label className="text-xs font-medium text-muted-foreground block">
            Add Members
          </label>
          <div className="relative">
            <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search people to add..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="pl-9"
            />
          </div>
        </div>

        <div className="max-h-48 overflow-y-auto space-y-1 divide-y divide-border/40">
          {searchResults.map((u) => {
            const isSelected = selectedUsers.some((sel) => sel.id === u.id);
            return (
              <button
                key={u.id}
                type="button"
                onClick={() => toggleSelectUser(u)}
                className="w-full flex items-center justify-between p-2 rounded-xl hover:bg-muted/70 transition-colors text-left"
              >
                <div className="flex items-center space-x-3">
                  <Avatar src={u.avatarUrl} name={u.displayName} size="sm" />
                  <div>
                    <div className="text-sm font-medium">{u.displayName}</div>
                    <div className="text-xs text-muted-foreground">{u.email}</div>
                  </div>
                </div>
                <div
                  className={`h-5 w-5 rounded-md border flex items-center justify-center transition-colors ${
                    isSelected
                      ? 'bg-primary border-primary text-primary-foreground'
                      : 'border-border'
                  }`}
                >
                  {isSelected && <Check className="h-3.5 w-3.5" />}
                </div>
              </button>
            );
          })}
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <Button variant="ghost" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button onClick={handleCreateGroup} isLoading={submitting}>
            <Users className="h-4 w-4 mr-2" />
            Create Group
          </Button>
        </div>
      </div>
    </Dialog>
  );
};
