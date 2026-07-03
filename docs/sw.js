// ============================================================
// RefugIA Web de Emergencia — Service Worker
// Cachea toda la app en la primera visita para que funcione
// 100% offline después. Estrategia: cache-first + actualización
// en segundo plano cuando hay red (stale-while-revalidate).
// ============================================================

const CACHE = 'refugia-web-v3';

const ASSETS = [
    './',
    './index.html',
    './data.js',
    './manifest.json',
    './icon.svg',
    './cartel-qr.png',
];

self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE).then((c) => c.addAll(ASSETS)).then(() => self.skipWaiting())
    );
});

self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys()
            .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
            .then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (event) => {
    if (event.request.method !== 'GET') return;

    event.respondWith(
        caches.match(event.request, { ignoreSearch: true }).then((cached) => {
            // Refresca la copia en cache cuando hay red, sin bloquear la respuesta.
            const update = fetch(event.request)
                .then((resp) => {
                    if (resp && resp.ok && new URL(event.request.url).origin === location.origin) {
                        const clone = resp.clone();
                        caches.open(CACHE).then((c) => c.put(event.request, clone));
                    }
                    return resp;
                })
                .catch(() => cached);
            return cached || update;
        })
    );
});
