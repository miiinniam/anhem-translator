// anhem v4.0 Service Worker
const CACHE = "anhem-v4-" + Date.now();

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
  const url = new URL(e.request.url);
  // Web Share Target: 拦截 POST /share-target
  if (e.request.method === "POST" && url.pathname.endsWith("/share-target")) {
    e.respondWith((async () => {
      try {
        const formData = await e.request.formData();
        const text = formData.get("text") || formData.get("title") || "";
        if (!text.trim()) return Response.redirect("./", 303);
        const params = new URLSearchParams({ src: text.trim(), auto: "1" });
        return Response.redirect("./?" + params.toString(), 303);
      } catch { return Response.redirect("./", 303); }
    })());
  }
  e.respondWith(
    caches.match(e.request).then(r => r || fetch(e.request).catch(() => caches.match("./index.html")))
  );
});

// 灵动岛通知点击/操作 → 打开/聚焦页面并回填原文
self.addEventListener("notificationclick", e => {
  e.notification.close();
  const data = e.notification.data || {};
  const src = data.src || "";
  const dir = data.dir || "auto";
  const out = data.out || "";

  e.waitUntil(
    clients.matchAll({ type: "window", includeUncontrolled: true }).then(cls => {
      // 打开目标窗口
      const focusOrOpen = (url) => {
        if (cls.length > 0) {
          cls[0].focus();
          cls[0].navigate(url);
        } else {
          clients.openWindow(url);
        }
      };

      if (e.action === "copy") {
        // 复制译文到剪贴板
        const params = new URLSearchParams({ src, dir, out, action: "copy" });
        focusOrOpen("./?" + params.toString());
      } else {
        // 默认: 打开/回填原文
        const params = new URLSearchParams({ src, dir, auto: "1" });
        focusOrOpen("./?" + params.toString());
      }
    })
  );
});
