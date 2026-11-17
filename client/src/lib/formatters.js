export function formatTime(time) {
  if (!time) return "—";
  const [h, m] = String(time).split(":").map(Number);
  if (Number.isNaN(h)) return time;
  const suffix = h >= 12 ? "PM" : "AM";
  const hour = h % 12 || 12;
  return `${hour}:${String(m || 0).padStart(2, "0")} ${suffix}`;
}

export function formatFullDate(date) {
  if (!date) return "Release date unavailable";
  const value = new Date(`${date}T00:00:00`);
  if (Number.isNaN(value.getTime())) return date;
  return value.toLocaleDateString("en-IN", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

const LANGUAGE_NAMES = {
  en: "English",
  hi: "Hindi",
  te: "Telugu",
  kn: "Kannada",
  ta: "Tamil",
  ml: "Malayalam",
  mr: "Marathi",
  bn: "Bengali",
  pa: "Punjabi",
  gu: "Gujarati",
  or: "Odia",
  ur: "Urdu",
  fr: "French",
  es: "Spanish",
  de: "German",
  ja: "Japanese",
  ko: "Korean",
  zh: "Chinese",
  it: "Italian",
  pt: "Portuguese",
  ru: "Russian",
  ar: "Arabic",
  tr: "Turkish",
  nl: "Dutch",
  sv: "Swedish",
  pl: "Polish",
  th: "Thai",
  id: "Indonesian",
  vi: "Vietnamese",
};

export function formatLanguage(code) {
  if (!code) return null;
  const trimmed = String(code).trim();
  if (!trimmed) return null;
  // Already a full name (not a 2-letter code)
  if (trimmed.length > 2) return trimmed;
  const lower = trimmed.toLowerCase();
  return LANGUAGE_NAMES[lower] || trimmed.toUpperCase();
}

export function formatDuration(minutes) {
  if (!minutes || Number.isNaN(Number(minutes))) return "Duration unavailable";
  const total = Number(minutes);
  if (total <= 0) return "Duration unavailable";
  const h = Math.floor(total / 60);
  const m = total % 60;
  if (h === 0) return `${m}m`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}m`;
}

export function formatDate(date) {
  if (!date) return "—";
  const value = new Date(`${date}T00:00:00`);
  if (Number.isNaN(value.getTime())) return date;
  return value.toLocaleDateString("en-IN", {
    weekday: "short", day: "2-digit", month: "short", year: "numeric"
  });
}

export function formatDateTime(date) {
  if (!date) return "—";
  const value = new Date(date);
  if (Number.isNaN(value.getTime())) return date;
  return value.toLocaleString("en-IN", {
    day: "2-digit", month: "short", year: "numeric",
    hour: "2-digit", minute: "2-digit"
  });
}

export function money(value) {
  return `₹${Number(value || 0).toLocaleString("en-IN")}`;
}
