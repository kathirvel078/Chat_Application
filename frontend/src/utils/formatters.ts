import { format, isToday, isYesterday, formatDistanceToNow } from 'date-fns';

export function formatMessageTime(dateString: string | Date | undefined): string {
  if (!dateString) return '';
  const date = new Date(dateString);
  return format(date, 'h:mm a');
}

export function formatConversationTime(dateString: string | Date | undefined): string {
  if (!dateString) return '';
  const date = new Date(dateString);

  if (isToday(date)) {
    return format(date, 'h:mm a');
  }
  if (isYesterday(date)) {
    return 'Yesterday';
  }
  return format(date, 'MMM d');
}

export function formatLastSeen(dateString: string | Date | undefined): string {
  if (!dateString) return 'Offline';
  const date = new Date(dateString);
  return `Last seen ${formatDistanceToNow(date, { addSuffix: true })}`;
}

export function getInitials(name: string | undefined): string {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) {
    return parts[0].substring(0, 2).toUpperCase();
  }
  return (parts[0][0] + parts[1][0]).toUpperCase();
}
