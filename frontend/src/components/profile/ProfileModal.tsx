import React, { useState } from 'react';
import { Dialog } from '../ui/Dialog';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { Avatar } from '../ui/Avatar';
import { useAuthStore } from '../../stores/authStore';
import { userService } from '../../services/userService';
import { User, Mail, Sparkles, Check } from 'lucide-react';

interface ProfileModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const ProfileModal: React.FC<ProfileModalProps> = ({ isOpen, onClose }) => {
  const { user, updateUser } = useAuthStore();
  const [displayName, setDisplayName] = useState(user?.displayName || '');
  const [avatarUrl, setAvatarUrl] = useState(user?.avatarUrl || '');
  const [loading, setLoading] = useState(false);
  const [savedSuccess, setSavedSuccess] = useState(false);

  const handleRandomAvatar = () => {
    const seed = Math.random().toString(36).substring(7);
    const newAvatar = `https://api.dicebear.com/7.x/initials/svg?seed=${seed}`;
    setAvatarUrl(newAvatar);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!displayName.trim()) return;

    setLoading(true);
    setSavedSuccess(false);

    try {
      const updated = await userService.updateProfile({
        displayName: displayName.trim(),
        avatarUrl: avatarUrl.trim(),
      });
      updateUser(updated);
      setSavedSuccess(true);
      setTimeout(() => {
        setSavedSuccess(false);
        onClose();
      }, 800);
    } catch (err) {
      console.error('Failed to update profile', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog
      isOpen={isOpen}
      onClose={onClose}
      title="User Profile & Settings"
      description="Update your personal details and avatar"
    >
      <form onSubmit={handleSave} className="space-y-5">
        <div className="flex flex-col items-center justify-center space-y-3 pt-2">
          <Avatar src={avatarUrl} name={displayName} size="xl" />
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleRandomAvatar}
            className="text-xs"
          >
            <Sparkles className="h-3.5 w-3.5 mr-1 text-primary" />
            Randomize Avatar
          </Button>
        </div>

        <Input
          label="Display Name"
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
          placeholder="Your full name"
          required
        />

        <div className="space-y-1.5">
          <label className="text-xs font-medium text-muted-foreground block">
            Email Address
          </label>
          <div className="flex h-10 w-full items-center rounded-lg border border-input bg-muted/40 px-3 py-2 text-sm text-muted-foreground">
            <Mail className="h-4 w-4 mr-2" />
            <span>{user?.email}</span>
          </div>
          <span className="text-[11px] text-muted-foreground">
            Account email cannot be changed
          </span>
        </div>

        <div className="flex justify-end gap-2 pt-4">
          <Button type="button" variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" isLoading={loading}>
            {savedSuccess ? (
              <>
                <Check className="h-4 w-4 mr-1 text-emerald-400" />
                Saved!
              </>
            ) : (
              'Save Changes'
            )}
          </Button>
        </div>
      </form>
    </Dialog>
  );
};
