/* =============================================================
   FarmConnect – API Client + Shared Utilities
   Base URL: http://localhost:8080
   ============================================================= */

const API_BASE = 'http://localhost:8080/api';

// ── Auth helpers ──────────────────────────────────────────────
const Auth = {
  save(data) {
    localStorage.setItem('fc_token',   data.token);
    localStorage.setItem('fc_role',    data.role);
    localStorage.setItem('fc_userId',  data.userId);
    localStorage.setItem('fc_name',    data.fullName);
    localStorage.setItem('fc_email',   data.email);
  },
  token()  { return localStorage.getItem('fc_token'); },
  role()   { return localStorage.getItem('fc_role'); },
  userId() { return localStorage.getItem('fc_userId'); },
  name()   { return localStorage.getItem('fc_name'); },
  email()  { return localStorage.getItem('fc_email'); },
  clear()  {
    ['fc_token','fc_role','fc_userId','fc_name','fc_email']
      .forEach(k => localStorage.removeItem(k));
  },
  isLoggedIn() { return !!this.token(); },
  isFarmer()   { return this.role() === 'FARMER'; },
  isBuyer()    { return this.role() === 'BUYER'; },
  requireLogin(redirect = 'index.html') {
    if (!this.isLoggedIn()) { window.location.href = redirect; return false; }
    return true;
  },
  requireFarmer() {
    if (!this.isLoggedIn()) { window.location.href = 'index.html'; return false; }
    if (!this.isFarmer())   { window.location.href = 'buyer-dashboard.html'; return false; }
    return true;
  },
  requireBuyer() {
    if (!this.isLoggedIn()) { window.location.href = 'index.html'; return false; }
    if (!this.isBuyer())    { window.location.href = 'farmer-dashboard.html'; return false; }
    return true;
  }
};

// ── HTTP client ───────────────────────────────────────────────
async function http(method, path, body = null, auth = true) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth && Auth.token()) headers['Authorization'] = 'Bearer ' + Auth.token();

  const opts = { method, headers };
  if (body) opts.body = JSON.stringify(body);

  try {
    const res = await fetch(API_BASE + path, opts);
    const json = await res.json();
    if (!res.ok) throw new Error(json.message || 'Request failed');
    return json;
  } catch (err) {
    throw err;
  }
}

const GET    = (path, auth)       => http('GET',    path, null, auth);
const POST   = (path, body, auth) => http('POST',   path, body, auth);
const PUT    = (path, body)       => http('PUT',    path, body);
const PATCH  = (path, body)       => http('PATCH',  path, body);
const DELETE = (path)             => http('DELETE', path);

// ── API calls ─────────────────────────────────────────────────
const API = {
  // Auth
  register: (data)         => POST('/auth/register', data, false),
  login:    (data)         => POST('/auth/login',    data, false),

  // Products - public
  browseProducts: (params) => {
    const q = new URLSearchParams(params).toString();
    return GET('/products/public' + (q ? '?' + q : ''), false);
  },
  getProduct: (id)         => GET('/products/public/' + id, false),

  // Products - farmer
  myProducts:     (fid)    => GET('/farmer/products/' + fid),
  addProduct:     (fid, d) => POST('/farmer/products/' + fid, d),
  updateProduct:  (fid, pid, d) => PUT('/farmer/products/' + fid + '/' + pid, d),
  deleteProduct:  (fid, pid)    => DELETE('/farmer/products/' + fid + '/' + pid),

  // Orders - buyer
  placeOrder:   (bid, d)   => POST('/buyer/orders/' + bid, d),
  buyerOrders:  (bid)      => GET('/buyer/orders/' + bid),
  cancelOrder:  (bid, oid) => DELETE('/buyer/orders/' + bid + '/' + oid + '/cancel'),

  // Orders - farmer
  farmerOrders:   (fid)         => GET('/farmer/orders/' + fid),
  updateStatus:   (fid, oid, s) => PATCH('/farmer/orders/' + fid + '/' + oid + '/status?status=' + s),
  farmerEarnings: (fid)         => GET('/farmer/orders/' + fid + '/earnings'),

  // Delivery options
  deliveryOptions:  (fid) => GET('/farmer/delivery/' + fid),
  enabledDelivery:  (fid) => GET('/farmer/delivery/' + fid + '/enabled', false),
  updateDelivery:   (fid, d)    => PUT('/farmer/delivery/' + fid, d),

  // Ratings
  submitRating:   (bid, d) => POST('/ratings/buyer/' + bid, d),
  farmerRatings:  (fid)    => GET('/ratings/farmer/' + fid, false),
};

// ── Toast ─────────────────────────────────────────────────────
function toast(msg, duration = 3000) {
  let el = document.getElementById('toast');
  if (!el) {
    el = document.createElement('div');
    el.id = 'toast';
    el.className = 'toast';
    document.body.appendChild(el);
  }
  el.textContent = msg;
  el.classList.add('show');
  setTimeout(() => el.classList.remove('show'), duration);
}

// ── Alert box ─────────────────────────────────────────────────
function showAlert(id, msg, type = 'error') {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = msg;
  el.className = 'alert alert-' + type;
  el.style.display = 'block';
  setTimeout(() => el.style.display = 'none', 5000);
}

// ── Loading spinner ───────────────────────────────────────────
function setLoading(btnId, loading, text = '') {
  const btn = document.getElementById(btnId);
  if (!btn) return;
  btn.disabled = loading;
  if (loading) {
    btn.dataset.origText = btn.textContent;
    btn.textContent = '⏳ Loading...';
  } else {
    btn.textContent = text || btn.dataset.origText || btn.textContent;
  }
}

// ── Format currency ───────────────────────────────────────────
function fmt(n) { return '₹' + Number(n).toLocaleString('en-IN'); }

// ── Format date ───────────────────────────────────────────────
function fmtDate(d) {
  return new Date(d).toLocaleDateString('en-IN', {
    day:'numeric', month:'short', year:'numeric'
  });
}

// ── Logout ────────────────────────────────────────────────────
function logout() {
  Auth.clear();
  window.location.href = 'index.html';
}

// ── Render nav user chip ──────────────────────────────────────
function renderUserChip() {
  const el = document.getElementById('userChip');
  if (!el) return;
  const isFarmer = Auth.isFarmer();
  el.innerHTML = `
    <span class="avatar ${isFarmer ? 'avatar-farmer' : 'avatar-buyer'}">
      ${isFarmer ? '👨‍🌾' : '🛒'}
    </span>
    ${Auth.name()} · ${Auth.role()}
  `;
}
