// =======================
// الإعدادات العامة
// =======================

// هام جدًا: تأكد من أن هذا هو رابط الـ API الصحيح الخاص بك والمرفوع على Render أو أي خدمة أخرى
const API = "https://pro-blog-api.onrender.com"; 

// التحقق من صلاحيات المدير يتم الآن من خلال صفحة admin.html المؤمنة
const ADMIN = true; 

// عناصر DOM
const grid = document.getElementById("articlesGrid");
const modal = document.getElementById("articleModal");
const modalTitle = document.getElementById("modalTitle");
const form = document.getElementById("articleForm");
const qInput = document.getElementById("q");
const addBtn = document.getElementById("addBtn");
const cancelBtn = document.getElementById("cancelModal");
const themeBtn = document.getElementById("themeBtn");
const toast = document.getElementById("toast");

// بروفايل وصورة
const siteAvatar = document.getElementById("siteAvatar");
const changeAvatarBtn = document.getElementById("changeAvatarBtn");
const resetAvatarBtn = document.getElementById("resetAvatarBtn");
const avatarFile = document.getElementById("avatarFile");

// سطر التعريف
const siteTagline = document.getElementById("siteTagline");
const editTaglineBtn = document.getElementById("editTaglineBtn");
const saveTaglineBtn = document.getElementById("saveTaglineBtn");

let articles = [];
let editingId = "";

// =======================
// أدوات مساعدة
// =======================
const $ = (id) => document.getElementById(id);

function showToast(msg, type = "info") {
  toast.textContent = msg;
  toast.className = `toast show ${type}`;
  setTimeout(() => (toast.className = "toast"), 2200);
}

function formatDate(ts) {
  const d = new Date(ts || Date.now());
  return d.toLocaleDateString("ar-EG", { year: "numeric", month: "long", day: "numeric" });
}

function escapeHtml(s) {
  return String(s || "").replace(/[&<>"']/g, (m) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;" }[m]));
}

function btn(kind, label) {
  const b = document.createElement("button");
  b.className = "btn " + (kind || "");
  b.textContent = label;
  return b;
}

// =======================
// تبديل الوضع (نهار/ليل)
// =======================
(function initThemeToggle() {
  const saved = localStorage.getItem("theme");
  if (saved === "light") document.documentElement.setAttribute("data-theme", "light");

  themeBtn.addEventListener("click", () => {
    const isLight = document.documentElement.getAttribute("data-theme") === "light";
    if (isLight) {
      document.documentElement.removeAttribute("data-theme");
      localStorage.removeItem("theme");
    } else {
      document.documentElement.setAttribute("data-theme", "light");
      localStorage.setItem("theme", "light");
    }
  });
})();

// =======================
// إعدادات عامة (Tagline/Avatar) — API أو LocalStorage
// =======================
async function loadSettings() {
  const savedAvatar = localStorage.getItem("siteAvatarUrl");
  if (savedAvatar) siteAvatar.src = savedAvatar;

  try {
    const res = await fetch(`${API}/settings`, { cache: "no-store" });
    if (res.ok) {
      const s = await res.json();
      if (s?.tagline) siteTagline.textContent = s.tagline;
      if (s?.avatarUrl) {
        siteAvatar.src = s.avatarUrl;
        localStorage.setItem("siteAvatarUrl", s.avatarUrl);
      }
    } else {
      const local = localStorage.getItem("siteTagline");
      if (local) siteTagline.textContent = local;
    }
  } catch {
    const local = localStorage.getItem("siteTagline");
    if (local) siteTagline.textContent = local;
  }

  // إظهار/إخفاء أدوات الإدارة
  if (!ADMIN) {
    editTaglineBtn.style.display = "none";
    saveTaglineBtn.style.display = "none";
    changeAvatarBtn.style.display = "none";
    resetAvatarBtn.style.display = "none";
  }
}

async function saveSettings(partial) {
  try {
    const res = await fetch(`${API}/settings`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(partial),
    });
    if (!res.ok) throw new Error("no settings endpoint");
  } catch {
    if (partial.tagline !== undefined) localStorage.setItem("siteTagline", String(partial.tagline));
    if (partial.avatarUrl !== undefined) localStorage.setItem("siteAvatarUrl", String(partial.avatarUrl));
  }
}

if (ADMIN) {
  editTaglineBtn.addEventListener("click", () => {
    siteTagline.setAttribute("contenteditable", "true");
    siteTagline.focus();
    editTaglineBtn.style.display = "none";
    saveTaglineBtn.style.display = "inline-flex";
  });

  saveTaglineBtn.addEventListener("click", async () => {
    siteTagline.removeAttribute("contenteditable");
    editTaglineBtn.style.display = "inline-flex";
    saveTaglineBtn.style.display = "none";
    await saveSettings({ tagline: siteTagline.textContent.trim() });
    showToast("تم حفظ سطر التعريف ✅", "success");
  });
}

(function initAvatar() {
  if (!ADMIN) return;

  changeAvatarBtn.addEventListener("click", () => avatarFile.click());
  avatarFile.addEventListener("change", async () => {
    const file = avatarFile.files[0];
    if (!file) return;

    try {
      const fd = new FormData();
      fd.append("file", file);
      const up = await fetch(`${API}/upload`, { method: "POST", body: fd });
      const data = await up.json();
      if (data?.url) {
        siteAvatar.src = data.url;
        await saveSettings({ avatarUrl: data.url });
        showToast("تم تغيير صورة البروفايل ✅", "success");
      } else {
        showToast("تعذّر رفع الصورة", "error");
      }
    } catch (e) {
      console.error(e);
      showToast("تعذّر رفع الصورة", "error");
    } finally {
      avatarFile.value = "";
    }
  });

  resetAvatarBtn.addEventListener("click", async () => {
    const fallback = "data:image/svg+xml;utf8," + encodeURIComponent("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 200'><rect width='200' height='200' fill='#0f172a'/><circle cx='100' cy='74' r='36' fill='#334155'/><rect x='28' y='126' width='144' height='50' rx='24' fill='#334155'/></svg>");
    siteAvatar.src = fallback;
    await saveSettings({ avatarUrl: fallback });
    showToast("تمت إعادة الصورة الافتراضية", "info");
  });
})();

// =======================
// عرض المقالات
// =======================
function renderArticles(list) {
  grid.innerHTML = "";
  if (!list.length) {
    const d = document.createElement("div");
    d.className = "card";
    d.innerHTML = '<div class="content"><h3 class="title">لا توجد مقالات</h3><p class="date">ابدأ بإضافة أول مقال الآن</p></div>';
    grid.appendChild(d);
    return;
  }
  list.forEach((a) => grid.appendChild(renderCard(a)));
}

function renderCard(a) {
  const wrap = document.createElement("article");
  wrap.className = "card";

  const cover = document.createElement("img");
  cover.className = "cover";
  cover.loading = "lazy";
  cover.alt = a.title || "";
  cover.src = a.cover || "data:image/svg+xml;utf8," + encodeURIComponent("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1200 675'><rect width='1200' height='675' fill='#0f172a'/><text x='50%' y='52%' font-size='56' fill='#fff' text-anchor='middle' font-family='system-ui,Segoe UI,Arial'>بدون صورة</text></svg>");

  const content = document.createElement("div");
  content.className = "content";

  const meta = document.createElement("div");
  meta.className = "meta";
  const h = document.createElement("h3");
  h.className = "title";
  h.textContent = a.title || "";
  const date = document.createElement("div");
  date.className = "date";
  date.textContent = a.createdAt ? formatDate(a.createdAt) : "";
  meta.append(h, date);

  const tags = document.createElement("div");
  tags.className = "tags";
  const tagsArr = Array.isArray(a.tags) ? a.tags : String(a.tags || "").split(",").map((s) => s.trim()).filter(Boolean);
  tagsArr.slice(0, 6).forEach((t) => {
    const s = document.createElement("span");
    s.className = "tag";
    s.textContent = "#" + t;
    tags.appendChild(s);
  });

  const ex = document.createElement("p");
  ex.className = "excerpt";
  const raw = a.excerpt || a.content || "";
  ex.textContent = raw.slice(0, 140) + (raw.length > 140 ? "…" : "");

  const ctrls = document.createElement("div");
  ctrls.className = "controls";
  const readBtn = btn("", "📖 قراءة");
  ctrls.append(readBtn);

  if (ADMIN) {
    const editBtn = btn("", "✏️ تعديل");
    const delBtn = btn("", "🗑️ حذف");
    ctrls.append(editBtn, delBtn);
    editBtn.onclick = () => openEditor(a);
    delBtn.onclick = () => deleteArticle(a._id);
  }

  const details = document.createElement("div");
  details.className = "details";
  const body = document.createElement("div");
  body.className = "details-content";
  body.innerHTML = (a.content || "").split(/\n{2,}/).map((p) => `<p>${escapeHtml(p).replace(/\n/g, "<br>")}</p>`).join("");
  details.appendChild(body);

  let open = false;
  const toggle = () => {
    open = !open;
    if (open) {
      details.classList.add("open");
      details.style.height = "auto";
      const h = details.clientHeight + "px";
      details.style.height = "0px";
      details.offsetHeight;
      details.style.height = h;
    } else {
      details.style.height = details.clientHeight + "px";
      details.offsetHeight;
      details.style.height = "0px";
      details.addEventListener("transitionend", () => details.classList.remove("open"), { once: true });
    }
  };

  readBtn.onclick = toggle;
  h.onclick = toggle;
  cover.onclick = toggle;

  content.append(meta, tags, ex);
  wrap.append(cover, content, ctrls, details);
  return wrap;
}

// =======================
// جلب المقالات
// =======================
async function loadArticles(query = "") {
  try {
    const url = query ? `${API}/articles?q=${encodeURIComponent(query)}` : `${API}/articles`;
    const res = await fetch(url);
    articles = await res.json();
    renderArticles(articles);
  } catch (e) {
    console.error(e);
    showToast("فشل تحميل المقالات", "error");
  }
}

// =======================
// المودال + الحفظ (Admins فقط)
// =======================
function openEditor(article = null) {
  if (!ADMIN) return;
  modal.style.display = "grid";
  document.body.style.overflow = "hidden";
  if (article) {
    modalTitle.textContent = "تعديل مقال";
    $("title").value = article.title || "";
    $("tags").value = (Array.isArray(article.tags) ? article.tags : String(article.tags || "").split(",")).join(", ");
    $("excerpt").value = article.excerpt || "";
    $("content").value = article.content || "";
    $("coverUrl").value = article.cover || "";
    $("coverFile").value = "";
    editingId = article._id || "";
  } else {
    modalTitle.textContent = "إضافة مقال";
    form.reset();
    editingId = "";
  }
  $("title").focus();
}

function closeEditor() {
  modal.style.display = "none";
  document.body.style.overflow = "";
}

if (ADMIN) {
  addBtn.addEventListener("click", () => openEditor(null));
  cancelBtn.addEventListener("click", closeEditor);
  form.addEventListener("submit", upsertArticle);
} else {
  if (addBtn) addBtn.style.display = "none";
  if (modal) modal.remove();
}

async function upsertArticle(e) {
  e.preventDefault();
  if (!ADMIN) return;

  const payload = {
    title: $("title").value.trim(),
    excerpt: $("excerpt").value.trim(),
    content: $("content").value.trim(),
    tags: $("tags").value.split(",").map((s) => s.trim()).filter(Boolean),
    cover: $("coverUrl").value.trim() || "",
  };
  if (!payload.title) {
    showToast("العنوان مطلوب", "error");
    return;
  }

  const file = $("coverFile").files[0];
  if (file) {
    try {
      const fd = new FormData();
      fd.append("file", file);
      const up = await fetch(`${API}/upload`, { method: "POST", body: fd });
      const data = await up.json();
      if (data?.url) payload.cover = data.url;
    } catch (e) {
      console.warn("upload failed, continue without cover", e);
    }
  }

  try {
    if (editingId) {
      await fetch(`${API}/articles/${editingId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      showToast("تم تحديث المقال", "success");
    } else {
      await fetch(`${API}/articles`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      showToast("تمت إضافة المقال", "success");
    }
    closeEditor();
    loadArticles(qInput.value.trim());
  } catch (e) {
    console.error(e);
    showToast("خطأ أثناء الحفظ", "error");
  }
}

window.editArticle = (id) => {
  if (!ADMIN) return;
  const a = articles.find((x) => x._id === id);
  if (!a) return;
  openEditor(a);
};
window.deleteArticle = async (id) => {
  if (!ADMIN) return;
  if (!confirm("حذف المقال نهائيًا؟")) return;
  try {
    await fetch(`${API}/articles/${id}`, { method: "DELETE" });
    showToast("تم الحذف", "success");
    loadArticles(qInput.value.trim());
  } catch (e) {
    console.error(e);
    showToast("خطأ أثناء الحذف", "error");
  }
};

// البحث
qInput.addEventListener("input", () => loadArticles(qInput.value.trim()));

// بدء التشغيل
loadSettings();
loadArticles();
