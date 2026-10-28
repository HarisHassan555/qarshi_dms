const BACKEND = 'http://localhost:8080';

const STATIC_ASSET = /\.(js|mjs|css|map|json|ico|png|jpe?g|gif|svg|webp|woff2?|ttf|eot)(\?.*)?$/i;

function isBackendHttpRequest(pathname, req) {
  if (!pathname || !pathname.startsWith('/velocity/')) {
    return false;
  }
  if (pathname.includes('ng-cli-ws') || pathname.includes('sockjs-node') || pathname.includes('hot-update')) {
    return false;
  }
  if (STATIC_ASSET.test(pathname)) {
    return false;
  }

  const method = (req.method || 'GET').toUpperCase();
  if (method !== 'GET' && method !== 'HEAD') {
    return true;
  }

  const accept = req.headers.accept || '';
  if (accept.includes('text/html')) {
    return false;
  }

  return true;
}

module.exports = [
  {
    context: (pathname, req) => isBackendHttpRequest(pathname, req),
    target: BACKEND,
    secure: false,
    changeOrigin: true,
    logLevel: 'warn',
  },
];
