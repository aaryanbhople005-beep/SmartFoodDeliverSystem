/* SmartFoodDeliverySystem — API client */
const API_BASE = "http://localhost:8080/api";

let sessionId = null;

function ensureSession() {
    if (!sessionId) {
        const stored = localStorage.getItem("sfds_session");
        if (stored) {
            sessionId = stored;
        } else {
            sessionId = "sess_" + Math.random().toString(36).slice(2, 15) + "_" + Date.now();
            localStorage.setItem("sfds_session", sessionId);
        }
    }
    return sessionId;
}

function getSession() {
    ensureSession();
    return sessionId;
}

async function apiGet(path, params = {}) {
    ensureSession();
    let url = API_BASE + path;
    const qs = new URLSearchParams(params);
    if (qs.toString()) url += "?" + qs.toString();
    const res = await fetch(url, { credentials: "omit" });
    return parseJson(res);
}

async function apiPost(path, body) {
    ensureSession();
    const res = await fetch(API_BASE + path, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
        body: toFormBody(body),
        credentials: "omit"
    });
    return parseJson(res);
}

function toFormBody(obj) {
    return Object.entries(obj || {})
        .map(([k, v]) => encodeURIComponent(k) + "=" + encodeURIComponent(v))
        .join("&");
}

async function parseJson(res) {
    const text = await res.text();
    let data = null;
    try { data = JSON.parse(text || "null"); } catch (e) { data = { raw: text }; }
    if (!res.ok) {
        const err = new Error(data && data.error ? data.error : "Request failed (" + res.status + ")");
        err.status = res.status;
        err.data = data;
        throw err;
    }
    return data;
}

function toast(message, type = "info") {
    let el = document.getElementById("sfds-toast");
    if (!el) {
        el = document.createElement("div");
        el.id = "sfds-toast";
        el.className = "toast";
        document.body.appendChild(el);
    }
    el.className = "toast " + (type === "success" ? "success" : type === "error" ? "error" : "");
    el.textContent = message;
    el.classList.add("show");
    setTimeout(() => el.classList.remove("show"), 2500);
}

export { apiGet, apiPost, getSession, ensureSession, toast };
