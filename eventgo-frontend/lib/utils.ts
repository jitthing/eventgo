import { format } from "date-fns";

export function formatDate(dateString: string) {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
} 

export function formatEventDuration(dateString: string) {
  const startDate = new Date(dateString);
  const endDate = new Date(startDate.getTime() + 3 * 60 * 60 * 1000); // Add 3 hours
  return `${format(startDate, "MMMM d, yyyy (h:mm a")} to ${format(endDate, "h:mm a")})`;
}