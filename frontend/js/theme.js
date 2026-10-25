/* SmartFoodDeliverySystem — Theme Management */
const THEME_KEY = "sfds_theme";
const LIGHT = "light";
const DARK = "dark";

function getStoredTheme() {
    return localStorage.getItem(THEME_KEY);
}

function setStoredTheme(theme) {
    localStorage.setItem(THEME_KEY, theme);
}

function getPreferredTheme() {
    const stored = getStoredTheme();
    if (stored) return stored;
    if (window.matchMedia("(prefers-color-scheme: dark)").matches) return DARK;
    return LIGHT;
}

function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    updateToggleIcon(theme);
}

function updateToggleIcon(theme) {
    const toggle = document.getElementById("theme-toggle");
    if (!toggle) return;
    const sunIcon = toggle.querySelector(".theme-sun");
    const moonIcon = toggle.querySelector(".theme-moon");
    if (theme === DARK) {
        if (sunIcon) sunIcon.style.display = "inline-block";
        if (moonIcon) moonIcon.style.display = "none";
        toggle.setAttribute("aria-label", "Switch to Light Mode");
        toggle.title = "Switch to Light Mode";
    } else {
        if (sunIcon) sunIcon.style.display = "none";
        if (moonIcon) moonIcon.style.display = "inline-block";
        toggle.setAttribute("aria-label", "Switch to Dark Mode");
        toggle.title = "Switch to Dark Mode";
    }
}

function toggleTheme() {
    const current = document.documentElement.getAttribute("data-theme") || getPreferredTheme();
    const next = current === DARK ? LIGHT : DARK;
    setStoredTheme(next);
    applyTheme(next);
}

function initTheme() {
    const theme = getPreferredTheme();
    applyTheme(theme);

    const toggle = document.getElementById("theme-toggle");
    if (toggle) {
        toggle.addEventListener("click", toggleTheme);
    }

    window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", (e) => {
        if (!getStoredTheme()) {
            applyTheme(e.matches ? DARK : LIGHT);
        }
    });
}

export { initTheme, toggleTheme, getPreferredTheme, applyTheme };