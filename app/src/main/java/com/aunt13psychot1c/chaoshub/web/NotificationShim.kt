package com.aunt13psychot1c.chaoshub.web

/**
 * Chat sites announce new messages through the browser's Notification API. An Android WebView
 * doesn't have one, so this stands in for it before the site's own scripts run and hands every
 * notification to the app (window.ChaosHub), which shows it as a real phone notification.
 */
internal const val NOTIFICATION_SHIM = """
(function () {
  if (window.__chaosHub) return;
  window.__chaosHub = true;
  function send(title, opts) {
    opts = opts || {};
    try { ChaosHub.notify(String(title || ''), String(opts.body || '')); } catch (e) {}
  }
  function HubNotification(title, opts) {
    send(title, opts);
    this.title = title;
    this.body = (opts && opts.body) || '';
    this.onclick = null; this.onclose = null; this.onshow = null; this.onerror = null;
  }
  HubNotification.permission = 'granted';
  HubNotification.maxActions = 0;
  HubNotification.requestPermission = function (cb) {
    if (typeof cb === 'function') cb('granted');
    return Promise.resolve('granted');
  };
  HubNotification.prototype.close = function () {};
  HubNotification.prototype.addEventListener = function () {};
  HubNotification.prototype.removeEventListener = function () {};
  try {
    Object.defineProperty(window, 'Notification', { value: HubNotification, writable: true, configurable: true });
  } catch (e) { window.Notification = HubNotification; }
  if (window.ServiceWorkerRegistration && ServiceWorkerRegistration.prototype) {
    ServiceWorkerRegistration.prototype.showNotification = function (title, opts) {
      send(title, opts);
      return Promise.resolve();
    };
  }
  if (navigator.permissions && navigator.permissions.query) {
    var query = navigator.permissions.query.bind(navigator.permissions);
    navigator.permissions.query = function (desc) {
      if (desc && desc.name === 'notifications') {
        return Promise.resolve({ state: 'granted', onchange: null,
          addEventListener: function () {}, removeEventListener: function () {} });
      }
      return query(desc);
    };
  }
})();
"""

/** Width a "computer version" tab lays itself out at, like Chrome's Desktop site. */
internal const val DESKTOP_WIDTH = 1024

/**
 * Computer-version sites declare width=device-width, which squeezes a desktop layout into a
 * phone and pushes half of it (Discord's chat, for one) off the right edge where it can't be
 * reached. This keeps the page's viewport at a real desktop width so the whole layout fits on
 * screen, shrunk to fit, with pinch-to-zoom.
 */
internal const val DESKTOP_VIEWPORT = """
(function () {
  if (window.top !== window || window.__chaosHubDesktop) return;
  window.__chaosHubDesktop = true;
  var want = 'width=$DESKTOP_WIDTH';
  function fit() {
    var metas = document.querySelectorAll('meta[name="viewport"]');
    for (var i = 0; i < metas.length; i++) if (metas[i].content !== want) metas[i].content = want;
    if (metas.length === 0 && document.head) {
      var m = document.createElement('meta');
      m.name = 'viewport'; m.content = want; document.head.appendChild(m);
    }
  }
  // At document start the page doesn't exist yet, so watch the document itself and fix the
  // viewport tag the moment it appears, and every time the site rewrites it.
  new MutationObserver(fit).observe(document, {
    childList: true, subtree: true, attributes: true, attributeFilter: ['content']
  });
  fit();
})();
"""
