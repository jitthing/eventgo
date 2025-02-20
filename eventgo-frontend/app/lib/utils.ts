export function formatDate(dateString: string): string {
  // Use UTC to ensure consistent formatting between server and client
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    timeZone: 'UTC'
  });
} 