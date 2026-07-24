/**
 * Lecteur de briefing Markdown avec filtre par thème.
 * Préférences sauvegardées en localStorage (par appareil).
 */

const STORAGE_KEY = "actu-maison-themes";
const BRIEFING_URL = "/briefings/latest.md";

let parsedSections = [];
let activeThemes = loadSavedThemes();

function loadSavedThemes() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw);
  } catch (_) { /* ignore */ }
  return ["all"];
}

function saveThemes(themes) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(themes));
}

function parseFrontmatter(text) {
  const match = text.match(/^---\n([\s\S]*?)\n---\n([\s\S]*)$/);
  if (!match) return { meta: {}, body: text };
  const meta = {};
  for (const line of match[1].split("\n")) {
    const [key, ...rest] = line.split(":");
    if (key && rest.length) meta[key.trim()] = rest.join(":").trim();
  }
  return { meta, body: match[2] };
}

function slugify(text) {
  return text
    .replace(/[^\w\s-àâäéèêëïîôùûüç]/gi, "")
    .trim()
    .toLowerCase()
    .replace(/\s+/g, "-");
}

function parseSections(body) {
  const sections = [];
  const parts = body.split(/^## /m).filter(Boolean);

  for (const part of parts) {
    const nl = part.indexOf("\n");
    const title = nl > -1 ? part.slice(0, nl).trim() : part.trim();
    const content = nl > -1 ? part.slice(nl + 1).trim() : "";
    const id = slugify(title);
    sections.push({ id, title, content, html: marked.parse(content) });
  }
  return sections;
}

function buildThemeButtons(sections) {
  const bar = document.getElementById("themes-bar");
  bar.innerHTML = "";

  const allBtn = document.createElement("button");
  allBtn.type = "button";
  allBtn.className = "theme-btn" + (activeThemes.includes("all") ? " active" : "");
  allBtn.dataset.theme = "all";
  allBtn.textContent = "Tout";
  allBtn.addEventListener("click", () => selectTheme("all"));
  bar.appendChild(allBtn);

  for (const section of sections) {
    const btn = document.createElement("button");
    btn.type = "button";
    const isActive = !activeThemes.includes("all") && activeThemes.includes(section.id);
    btn.className = "theme-btn" + (isActive ? " active" : "");
    btn.dataset.theme = section.id;
    btn.textContent = section.title;
    btn.addEventListener("click", () => selectTheme(section.id));
    bar.appendChild(btn);
  }
}

function selectTheme(themeId) {
  if (themeId === "all") {
    activeThemes = ["all"];
  } else {
    let current = activeThemes.filter((t) => t !== "all");
    if (current.includes(themeId)) {
      current = current.filter((t) => t !== themeId);
    } else {
      current.push(themeId);
    }
    activeThemes = current.length ? current : ["all"];
  }
  saveThemes(activeThemes);
  buildThemeButtons(parsedSections);
  renderSections();
}

function renderSections() {
  const container = document.getElementById("content");
  const showAll = activeThemes.includes("all");
  const visible = showAll
    ? parsedSections
    : parsedSections.filter((s) => activeThemes.includes(s.id));

  if (!visible.length) {
    container.innerHTML = '<p class="loading">Aucun thème sélectionné.</p>';
    return;
  }

  container.innerHTML = visible
    .map(
      (s) => `
      <section class="theme-section" data-theme="${s.id}">
        <h2>${s.title}</h2>
        <div class="article-body">${s.html}</div>
      </section>`
    )
    .join("");

  styleArticleCards();
}

function styleArticleCards() {
  document.querySelectorAll(".theme-section .article-body h3").forEach((h3) => {
    const card = document.createElement("div");
    card.className = "article-card";
    let sibling = h3.nextElementSibling;
    const toMove = [h3];
    while (sibling && sibling.tagName !== "H3" && sibling.tagName !== "HR") {
      toMove.push(sibling);
      sibling = sibling.nextElementSibling;
    }
    const parent = h3.parentElement;
    parent.insertBefore(card, h3);
    toMove.forEach((el) => card.appendChild(el));
  });

  document.querySelectorAll(".article-card").forEach((card) => {
    const meta = card.querySelector("p");
    if (meta) meta.className = "article-meta";
  });
}

async function loadBriefing() {
  const content = document.getElementById("content");
  content.innerHTML = '<p class="loading">Chargement du briefing…</p>';

  try {
    const resp = await fetch(BRIEFING_URL + "?t=" + Date.now());
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    const text = await resp.text();
    const { meta, body } = parseFrontmatter(text);

    document.getElementById("briefing-date").textContent =
      meta.date ? `Briefing du ${meta.date}` : "Briefing";

    if (meta.generated_at) {
      const d = new Date(meta.generated_at);
      document.getElementById("generated-at").textContent =
        "Généré le " + d.toLocaleString("fr-FR");
    }

    const titleMatch = body.match(/^# (.+)$/m);
    if (titleMatch) {
      document.getElementById("briefing-date").textContent = titleMatch[1];
    }

    parsedSections = parseSections(body);
    buildThemeButtons(parsedSections);
    renderSections();
  } catch (err) {
    content.innerHTML = `
      <div class="error">
        <p>Impossible de charger le briefing.</p>
        <p style="margin-top:0.5rem;font-size:0.85rem">${err.message}</p>
        <p style="margin-top:0.75rem;font-size:0.85rem">
          Lancez <code>python generate.py --no-ai</code> puis <code>python serve.py</code>
        </p>
      </div>`;
  }
}

document.getElementById("btn-refresh").addEventListener("click", loadBriefing);
loadBriefing();
