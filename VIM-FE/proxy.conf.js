/**
 * Dev proxy configuration:
 * - Keep SPA routes/assets under /velocity on Angular dev server
 * - Proxy API calls under /velocity to backend (8080)
 */
module.exports = {
  '/velocity': {
    target: 'http://127.0.0.1:8080',
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
    bypass: function bypass(req) {
      const url = req.url || '';
      const accept = (req.headers && req.headers.accept) || '';
      const isHtml = accept.includes('text/html');
      const isAsset = /\.(js|mjs|css|map|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf|eot)$/.test(url);

      // Let Angular dev server handle browser route navigation.
      if (req.method === 'GET' && isHtml) {
        return '/index.html';
      }

      // Let Angular dev server handle static bundles/assets requested with /velocity prefix.
      if (req.method === 'GET' && isAsset && url.startsWith('/velocity/')) {
        return url.replace(/^\/velocity/, '');
      }

      // Otherwise proxy to backend (API requests).
      return null;
    }
  }
};

