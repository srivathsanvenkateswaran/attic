/* Attic site: carries the Theme choice to every screenshot and to the page's own colours.

   Every screenshot is a <picture> whose dark sources have media="(prefers-color-scheme: dark)", so
   with no script the page follows the device's theme. Picking Light or Dark rewrites that query on
   every element that has it, to one that never or always matches, and the browser then picks the
   other file itself: srcset, sizes and lazy loading work as before, and only one theme's files load.
   The same rewrite moves the theme-color meta tags, and data-theme on <html> switches the colours in
   style.css. Style needs no script: it runs on :has() in style.css. */

// Whether the page remembers the visitor's Style and Theme across visits, in localStorage under
// STORAGE_KEY. While this is false, nothing is stored and every visit starts at Expressive and
// Match device.
const REMEMBER_CHOICES = false;
const STORAGE_KEY = "attic-site-choices";

const DARK = "(prefers-color-scheme: dark)";
const LIGHT = "(prefers-color-scheme: light)";
const NEVER = "not all";
const ALWAYS = "all";

const darkMedia = document.querySelectorAll(`[media="${DARK}"]`);
const lightMedia = document.querySelectorAll(`[media="${LIGHT}"]`);

// theme is "auto", "light" or "dark".
function applyTheme(theme) {
  const forced = theme === "light" || theme === "dark";
  for (const el of darkMedia) el.setAttribute("media", forced ? (theme === "dark" ? ALWAYS : NEVER) : DARK);
  for (const el of lightMedia) el.setAttribute("media", forced ? (theme === "light" ? ALWAYS : NEVER) : LIGHT);
  if (forced) document.documentElement.dataset.theme = theme;
  else delete document.documentElement.dataset.theme;
}

function checked(name) {
  return document.querySelector(`input[name="${name}"]:checked`)?.value;
}

function pick(name, value) {
  const input = document.querySelector(`input[name="${name}"][value="${value}"]`);
  if (input) input.checked = true;
}

if (REMEMBER_CHOICES) {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY)) || {};
    if (saved.style) pick("style", saved.style);
    if (saved.theme) pick("theme", saved.theme);
  } catch {
    // Storage blocked or the entry unreadable: start from the defaults.
  }
}

// The Theme buttons, and the words that mention them, stay hidden until this script can act on them.
for (const el of document.querySelectorAll("[data-needs-script]")) el.hidden = false;

// Applied even when nothing was saved: a browser can bring back the last pick on reload.
applyTheme(checked("theme") || "auto");

document.addEventListener("change", (event) => {
  const name = event.target.name;
  if (name !== "style" && name !== "theme") return;
  if (name === "theme") applyTheme(event.target.value);
  if (REMEMBER_CHOICES) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ style: checked("style"), theme: checked("theme") }));
    } catch {
      // Storage blocked: the choice lasts for this visit only.
    }
  }
});
