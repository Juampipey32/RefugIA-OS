// ============================================================
// RefugIA OS — Service Worker (PWA Offline Support)
// ============================================================
// Cache-first for static assets, network-first for API calls.
// Enables "Add to Home Screen" and offline simulation mode.
// ============================================================

const CACHE_NAME = 'refugia-v1';

const PRECACHE_URLS = [
  '/',
  '/manifest.json',
  '/icons/icon.svg',
];

// --- Install: precache static assets ---
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(PRECACHE_URLS))
      .then(() => self.skipWaiting())
  );
});

// --- Activate: clean old caches ---
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))
      )
    ).then(() => self.clients.claim())
  );
});

// --- Fetch: strategy based on request type ---
self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  // API calls: network-first (fallback to cache)
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(
      fetch(event.request)
        .then((response) => {
          const clone = response.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
          return response;
        })
        .catch(() => caches.match(event.request))
    );
    return;
  }

  // Static assets: cache-first (fallback to network)
  event.respondWith(
    caches.match(event.request)
      .then((cached) => {
        if (cached) return cached;
        return fetch(event.request).then((response) => {
          if (response.ok) {
            const clone = response.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
          }
          return response;
        });
      })
  );
});
