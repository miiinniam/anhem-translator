// anhem v0.99 Service Worker
const CACHE = "anhem-v099d-" + Date.now();

self.addEventListener("install", e => {
  e.waitUntil(
    caches.open(CACHE).then(c => c.addAll(["./", "./index.html", "./app.js"]))
  );
  self.skipWaiting();
});

self.addEventListener("activate", e => {
  e.waitUntil(
    caches.keys().then(keys => Promise.all(
      keys.filter(k => k !== CACHE).map(k => caches.delete(k))
    ))
  );
  clients.claim();
});

self.addEventListener("fetch", e => {
  const req = e.request;
  // 页面导航:network-first —— 保证每次打开都拿到最新版,不再被旧缓存卡住
  if (req.mode === "navigate") {
    e.respondWith(
      fetch(req).then(res => {
        const copy = res.clone();
        caches.open(CACHE).then(c => c.put("./index.html", copy));
        return res;
      }).catch(() => caches.match("./index.html"))
    );
    return;
  }
  // 静态资源:cache-first(缓存名带版本号,更新时自动换新)
  e.respondWith(
    caches.match(req).then(r => r || fetch(req).catch(() => caches.match("./index.html")))
  );
});

// 灵动岛通知点击 → 打开/聚焦页面并回填原文
self.addEventListener("notificationclick", e => {
  e.notification.close();
  const src = e.notification.data?.src || "";
  const dir = e.notification.data?.dir || "auto";
  e.waitUntil(
    clients.matchAll({ type: "window", includeUncontrolled: true }).then(cls => {
      if (cls.length > 0) {
        cls[0].focus();
        cls[0].postMessage({ type: "fillInput", src, dir });
      } else {
        clients.openWindow("./?src=" + encodeURIComponent(src) + "&dir=" + dir);
      }
    })
  );
});
