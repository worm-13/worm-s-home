export const BACKEND_ORIGIN = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export function resolveAssetUrl(path?: string | null): string {
  if (!path) return '';
  if (/^https?:\/\//i.test(path)) return path;
  return path.startsWith('/') ? `${BACKEND_ORIGIN}${path}` : `${BACKEND_ORIGIN}/${path}`;
}
