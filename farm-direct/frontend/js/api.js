/* ============================================================
   FarmDirect — API helper
   ============================================================ */

const API_BASE = (window.FARMDIRECT_API_BASE || 'http://localhost:8080') + '/api';

const Auth = {
  token: () => localStorage.getItem('fd_token'),
  user:  () => { try { return JSON.parse(localStorage.getItem('fd_user') || 'null'); } catch (_) { return null; } },
  set: (token, user) => {
    localStorage.setItem('fd_token', token);
    localStorage.setItem('fd_user', JSON.stringify(user));
  },
  clear: () => {
    localStorage.removeItem('fd_token');
    localStorage.removeItem('fd_user');
  },
  isLoggedIn: () => !!localStorage.getItem('fd_token'),
  isFarmer:   () => Auth.user()?.role === 'FARMER',
  isBuyer:    () => Auth.user()?.role === 'BUYER',
};

async function apiFetch(path, opts = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(opts.headers || {}),
  };
  const token = Auth.token();
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const res = await fetch(API_BASE + path, { ...opts, headers });
  let data = null;
  const text = await res.text();
  if (text) {
    try { data = JSON.parse(text); } catch (_) { data = text; }
  }
  if (!res.ok) {
    const msg = (data && data.error) || (typeof data === 'string' ? data : `Request failed (${res.status})`);
    const err = new Error(msg);
    err.status = res.status;
    err.data = data;
    throw err;
  }
  return data;
}

const Api = {
  // Auth
  register: (body) => apiFetch('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  login:    (body) => apiFetch('/auth/login',    { method: 'POST', body: JSON.stringify(body) }),

  // Products
  listProducts: (q = {}) => {
    const sp = new URLSearchParams();
    if (q.category) sp.set('category', q.category);
    if (q.search)   sp.set('search', q.search);
    const qs = sp.toString();
    return apiFetch('/products' + (qs ? '?' + qs : ''));
  },
  getProduct:    (id) => apiFetch(`/products/${id}`),
  myProducts:    ()   => apiFetch('/products/mine'),
  createProduct: (b)  => apiFetch('/products', { method: 'POST', body: JSON.stringify(b) }),
  updateProduct: (id, b) => apiFetch(`/products/${id}`, { method: 'PUT', body: JSON.stringify(b) }),
  deleteProduct: (id) => apiFetch(`/products/${id}`, { method: 'DELETE' }),

  // Orders
  placeOrder:   (b) => apiFetch('/orders', { method: 'POST', body: JSON.stringify(b) }),
  buyerOrders:  ()  => apiFetch('/orders/buyer'),
  farmerOrders: ()  => apiFetch('/orders/farmer'),
  setOrderStatus: (id, status) =>
    apiFetch(`/orders/${id}/status`, { method: 'PUT', body: JSON.stringify({ status }) }),

  // User
  me: () => apiFetch('/users/me'),
};

/* ===== Toast ===== */
function toast(msg, type = '') {
  let t = document.getElementById('fd-toast');
  if (!t) {
    t = document.createElement('div');
    t.id = 'fd-toast';
    t.className = 'toast';
    document.body.appendChild(t);
  }
  t.className = 'toast ' + type;
  t.textContent = msg;
  requestAnimationFrame(() => t.classList.add('show'));
  clearTimeout(t._timer);
  t._timer = setTimeout(() => t.classList.remove('show'), 3200);
}

function requireAuth(role) {
  if (!Auth.isLoggedIn()) {
    location.href = 'login.html?redirect=' + encodeURIComponent(location.pathname + location.search);
    return false;
  }
  if (role && Auth.user()?.role !== role) {
    toast('You do not have access to this page', 'error');
    setTimeout(() => location.href = 'index.html', 1200);
    return false;
  }
  return true;
}

function logout() {
  Auth.clear();
  location.href = 'index.html';
}

/* ===== Render shared navbar ===== */
function renderNav(active = '') {
  const navHost = document.getElementById('navbar');
  if (!navHost) return;
  const u = Auth.user();
  let userArea = '';
  if (u) {
    const dash = u.role === 'FARMER' ? 'farmer-dashboard.html' : 'buyer-dashboard.html';
    userArea = `
      <span id="userBadge">Hi, ${escapeHtml(u.name.split(' ')[0])}</span>
      <a href="${dash}" class="btn btn-outline btn-sm">Dashboard</a>
      <button class="btn btn-ghost btn-sm" onclick="logout()">Logout</button>
    `;
  } else {
    userArea = `
      <a href="login.html" class="btn btn-ghost btn-sm">Log in</a>
      <a href="register.html" class="btn btn-primary btn-sm">Get started</a>
    `;
  }
  navHost.innerHTML = `
    <nav class="navbar">
      <div class="nav-inner">
        <a href="index.html" class="brand">
          <span class="brand-mark">FD</span> FarmDirect
        </a>
        <ul class="nav-links">
          <li><a href="index.html" class="${active==='home'?'active':''}">Home</a></li>
          <li><a href="products.html" class="${active==='products'?'active':''}">Marketplace</a></li>
          <li><a href="index.html#how" class="${active==='how'?'active':''}">How it works</a></li>
          <li><a href="index.html#about" class="${active==='about'?'active':''}">About</a></li>
        </ul>
        <div class="nav-cta">${userArea}</div>
      </div>
    </nav>
  `;
}

function renderFooter() {
  const f = document.getElementById('footer');
  if (!f) return;
  f.innerHTML = `
    <footer class="footer">
      <div class="footer-inner">
        <div>
          <div class="brand" style="color:#fff; margin-bottom:14px;">
            <span class="brand-mark">FD</span>
            <span style="color:#fff;">FarmDirect</span>
          </div>
          <p style="opacity:0.75; max-width:320px;">
            A direct marketplace between farmers and buyers. No middlemen.
            Fair prices. Fresh produce.
          </p>
        </div>
        <div>
          <h4>Marketplace</h4>
          <a href="products.html">Browse produce</a>
          <a href="register.html">Sell as a farmer</a>
          <a href="register.html">Buy as a household</a>
        </div>
        <div>
          <h4>Company</h4>
          <a href="index.html#about">Our story</a>
          <a href="index.html#how">How it works</a>
          <a href="#">Contact</a>
        </div>
        <div>
          <h4>Legal</h4>
          <a href="#">Terms</a>
          <a href="#">Privacy</a>
          <a href="#">Refund policy</a>
        </div>
      </div>
      <div class="footer-bottom">
        © ${new Date().getFullYear()} FarmDirect. Built for farming communities.
      </div>
    </footer>
  `;
}

function escapeHtml(s) {
  if (s == null) return '';
  return String(s)
    .replaceAll('&', '&amp;').replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

function fmtMoney(n) {
  const v = Number(n || 0);
  return '₹' + v.toLocaleString('en-IN', { maximumFractionDigits: 2 });
}

function fmtDate(d) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' });
}

document.addEventListener('DOMContentLoaded', () => {
  renderNav(document.body.dataset.page || '');
  renderFooter();
});
