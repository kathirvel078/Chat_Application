import React, { useState } from 'react';
import { Conversation } from '../../types/conversation';
import { ConversationItem } from '../chat/ConversationItem';
import { Avatar } from '../ui/Avatar';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Skeleton } from '../ui/Skeleton';
import { ThemeToggle } from '../profile/ThemeToggle';
import { ProfileModal } from '../profile/ProfileModal';
import { NewChatDialog } from '../chat/NewChatDialog';
import { CreateGroupDialog } from '../chat/CreateGroupDialog';
import { useAuthStore } from '../../stores/authStore';
import { useChatStore } from '../../stores/chatStore';
import {
  MessageSquarePlus,
  Users,
  Search,
  Settings,
  LogOut,
  MessageCircle,
} from 'lucide-react';

interface SidebarProps {
  conversations: Conversation[];
  isLoading: boolean;
  onRefreshConversations: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  conversations,
  isLoading,
  onRefreshConversations,
}) => {
  const { user, logout } = useAuthStore();
  const { activeConversationId, setActiveConversationId } = useChatStore();

  const [search, setSearch] = useState('');
  const [profileOpen, setProfileOpen] = useState(false);
  const [newChatOpen, setNewChatOpen] = useState(false);
  const [newGroupOpen, setNewGroupOpen] = useState(false);

  const filteredConversations = conversations.filter((c) =>
    c.name.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <aside className="w-full md:w-80 lg:w-96 h-full flex flex-col border-r border-border bg-card select-none">
      {/* Top User Bar */}
      <div className="flex items-center justify-between p-4 border-b border-border">
        <div
          onClick={() => setProfileOpen(true)}
          className="flex items-center gap-3 cursor-pointer group"
          title="Edit Profile"
        >
          <Avatar
            src={user?.avatarUrl}
            name={user?.displayName}
            size="md"
            isOnline={true}
          />
          <div className="min-w-0">
            <h3 className="text-sm font-semibold truncate group-hover:text-primary transition-colors">
              {user?.displayName}
            </h3>
            <span className="text-[11px] text-emerald-500 font-medium flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
              Active
            </span>
          </div>
        </div>

        <div className="flex items-center gap-1">
          <ThemeToggle />
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setProfileOpen(true)}
            title="Settings"
            className="text-muted-foreground hover:text-foreground"
          >
            <Settings className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={logout}
            title="Sign out"
            className="text-muted-foreground hover:text-destructive"
          >
            <LogOut className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Action Buttons & Search */}
      <div className="p-3 space-y-2 border-b border-border/60">
        <div className="grid grid-cols-2 gap-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setNewChatOpen(true)}
            className="w-full text-xs font-medium justify-center gap-1.5 rounded-xl"
          >
            <MessageSquarePlus className="h-3.5 w-3.5 text-primary" />
            New Chat
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setNewGroupOpen(true)}
            className="w-full text-xs font-medium justify-center gap-1.5 rounded-xl"
          >
            <Users className="h-3.5 w-3.5 text-primary" />
            New Group
          </Button>
        </div>

        <div className="relative">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search conversations..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9 h-9 text-xs rounded-xl bg-muted/40"
          />
        </div>
      </div>

      {/* Conversations List */}
      <div className="flex-1 overflow-y-auto p-2 space-y-1">
        {isLoading && (
          <div className="space-y-2 p-2">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="flex items-center gap-3 p-2">
                <Skeleton className="h-10 w-10 rounded-full" />
                <div className="space-y-1.5 flex-1">
                  <Skeleton className="h-3.5 w-3/4" />
                  <Skeleton className="h-2.5 w-1/2" />
                </div>
              </div>
            ))}
          </div>
        )}

        {!isLoading && filteredConversations.length === 0 && (
          <div className="flex flex-col items-center justify-center h-48 text-center p-4">
            <MessageCircle className="h-8 w-8 text-muted-foreground/60 mb-2" />
            <p className="text-sm font-medium">No conversations found</p>
            <p className="text-xs text-muted-foreground mt-0.5">
              {search
                ? 'Try a different search term'
                : 'Start a direct chat or create a group above'}
            </p>
          </div>
        )}

        {!isLoading &&
          filteredConversations.map((conv) => (
            <ConversationItem
              key={conv.id}
              conversation={conv}
              isActive={conv.id === activeConversationId}
              onClick={() => setActiveConversationId(conv.id)}
            />
          ))}
      </div>

      {/* Dialogs */}
      <ProfileModal
        isOpen={profileOpen}
        onClose={() => setProfileOpen(false)}
      />
      <NewChatDialog
        isOpen={newChatOpen}
        onClose={() => setNewChatOpen(false)}
        onConversationCreated={onRefreshConversations}
      />
      <CreateGroupDialog
        isOpen={newGroupOpen}
        onClose={() => setNewGroupOpen(false)}
        onConversationCreated={onRefreshConversations}
      />
    </aside>
  );
};
