import React, { useState } from 'react';
import { Conversation } from '../../types/conversation';
import { Avatar } from '../ui/Avatar';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { Dialog } from '../ui/Dialog';
import { Input } from '../ui/Input';
import { useAuthStore } from '../../stores/authStore';
import { usePresenceStore } from '../../stores/presenceStore';
import { conversationService } from '../../services/conversationService';
import { userService } from '../../services/userService';
import { UserSummary } from '../../types/user';
import { X, UserPlus, LogOut, Shield, Search, Loader2 } from 'lucide-react';
import { format } from 'date-fns';

interface DetailsPanelProps {
  conversation: Conversation;
  onClose: () => void;
  onConversationUpdated: () => void;
}

export const DetailsPanel: React.FC<DetailsPanelProps> = ({
  conversation,
  onClose,
  onConversationUpdated,
}) => {
  const currentUserId = useAuthStore((s) => s.user?.id);
  const isUserOnline = usePresenceStore((s) => s.isUserOnline);

  const [addMemberOpen, setAddMemberOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<UserSummary[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  const isGroup = conversation.type === 'GROUP';
  const isAdmin = conversation.adminIds?.includes(currentUserId || '');

  const handleSearchUsers = async (q: string) => {
    setSearchQuery(q);
    if (!q.trim()) {
      setSearchResults([]);
      return;
    }
    setSearchLoading(true);
    try {
      const results = await userService.searchUsers(q.trim());
      // Filter out users already in conversation
      const filtered = results.filter((u) => !conversation.memberIds.includes(u.id));
      setSearchResults(filtered);
    } catch (err) {
      console.error(err);
    } finally {
      setSearchLoading(false);
    }
  };

  const handleAddMember = async (userId: string) => {
    setActionLoading(true);
    try {
      await conversationService.addMember(conversation.id, userId);
      setAddMemberOpen(false);
      onConversationUpdated();
    } catch (err) {
      console.error('Failed to add member', err);
    } finally {
      setActionLoading(false);
    }
  };

  const handleRemoveMember = async (userId: string) => {
    if (!window.confirm(userId === currentUserId ? 'Leave this group?' : 'Remove this member?')) {
      return;
    }
    setActionLoading(true);
    try {
      await conversationService.removeMember(conversation.id, userId);
      onConversationUpdated();
      if (userId === currentUserId) {
        onClose();
      }
    } catch (err) {
      console.error('Failed to remove member', err);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="w-80 h-full border-l border-border bg-card flex flex-col flex-shrink-0 animate-in slide-in-from-right duration-200">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-border">
        <h3 className="font-semibold text-sm">Conversation Info</h3>
        <button
          onClick={onClose}
          className="rounded-full p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-6">
        {/* Profile Card */}
        <div className="flex flex-col items-center text-center space-y-3 pb-4 border-b border-border">
          <Avatar
            src={conversation.avatarUrl}
            name={conversation.name}
            size="xl"
          />
          <div>
            <h4 className="font-semibold text-base">{conversation.name}</h4>
            <p className="text-xs text-muted-foreground mt-0.5">
              {isGroup
                ? `Group · ${conversation.memberIds.length} members`
                : 'Direct Conversation'}
            </p>
          </div>
          <span className="text-[11px] text-muted-foreground">
            Created {format(new Date(conversation.createdAt), 'MMMM d, yyyy')}
          </span>
        </div>

        {/* Member Management (for Groups) */}
        {isGroup && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                Members ({conversation.members?.length || conversation.memberIds.length})
              </span>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setAddMemberOpen(true)}
                className="h-7 text-xs text-primary"
              >
                <UserPlus className="h-3.5 w-3.5 mr-1" />
                Add
              </Button>
            </div>

            <div className="space-y-2">
              {conversation.members?.map((member) => {
                const isMemberAdmin = conversation.adminIds?.includes(member.id);
                const online = isUserOnline(member.id);

                return (
                  <div
                    key={member.id}
                    className="flex items-center justify-between p-2 rounded-xl hover:bg-muted/50 transition-colors"
                  >
                    <div className="flex items-center space-x-2.5 min-w-0">
                      <Avatar
                        src={member.avatarUrl}
                        name={member.displayName}
                        size="sm"
                        isOnline={online}
                      />
                      <div className="min-w-0">
                        <div className="flex items-center gap-1.5">
                          <span className="text-xs font-medium truncate">
                            {member.displayName}
                          </span>
                          {isMemberAdmin && (
                            <Badge variant="outline" className="text-[9px] py-0 px-1">
                              Admin
                            </Badge>
                          )}
                        </div>
                        <span className="text-[11px] text-muted-foreground block truncate">
                          {online ? 'Online' : 'Offline'}
                        </span>
                      </div>
                    </div>

                    {isAdmin && member.id !== currentUserId && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleRemoveMember(member.id)}
                        disabled={actionLoading}
                        className="h-7 text-[11px] text-destructive hover:bg-destructive/10"
                      >
                        Remove
                      </Button>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Leave Group Action */}
            <div className="pt-4 border-t border-border">
              <Button
                variant="destructive"
                className="w-full text-xs"
                size="sm"
                onClick={() => handleRemoveMember(currentUserId || '')}
                disabled={actionLoading}
              >
                <LogOut className="h-3.5 w-3.5 mr-1.5" />
                Leave Group
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Add Member Dialog */}
      <Dialog
        isOpen={addMemberOpen}
        onClose={() => setAddMemberOpen(false)}
        title="Add Colleague to Group"
        description="Search for registered users not yet in this group"
      >
        <div className="space-y-4">
          <div className="relative">
            <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search colleagues..."
              value={searchQuery}
              onChange={(e) => handleSearchUsers(e.target.value)}
              className="pl-9"
              autoFocus
            />
          </div>

          <div className="max-h-48 overflow-y-auto space-y-1 divide-y divide-border/40">
            {searchLoading && (
              <div className="flex items-center justify-center py-4 text-xs text-muted-foreground">
                <Loader2 className="h-4 w-4 animate-spin mr-2" />
                Searching...
              </div>
            )}
            {!searchLoading && searchResults.map((u) => (
              <div
                key={u.id}
                className="flex items-center justify-between p-2 rounded-xl hover:bg-muted/70 text-left"
              >
                <div className="flex items-center space-x-2.5">
                  <Avatar src={u.avatarUrl} name={u.displayName} size="sm" />
                  <div>
                    <div className="text-xs font-medium">{u.displayName}</div>
                    <div className="text-[11px] text-muted-foreground">{u.email}</div>
                  </div>
                </div>
                <Button
                  size="sm"
                  onClick={() => handleAddMember(u.id)}
                  isLoading={actionLoading}
                >
                  Add
                </Button>
              </div>
            ))}
          </div>
        </div>
      </Dialog>
    </div>
  );
};
