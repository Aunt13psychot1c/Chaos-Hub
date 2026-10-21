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
