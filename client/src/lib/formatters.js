export function formatTime(time) {
  if (!time) return "—";
  const [h, m] = String(time).split(":").map(Number);
  if (Number.isNaN(h)) return time;
  const suffix = h >= 12 ? "PM" : "AM";
  const hour = h % 12 || 12;
  return `${hour}:${String(m || 0).padStart(2, "0")} ${suffix}`;
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
