import { apiFetch, apiUrl } from "./api";

const TOKEN_PATTERN = /^[A-Za-z0-9_-]{43}$/;

export function isWellFormedTicketToken(value) {
  return TOKEN_PATTERN.test(String(value ?? "").trim());
}

// CUSTOMER TICKETS
export const getMyTickets = () => apiFetch("/api/tickets/me");

export const getTicket = (ticketId) =>
  apiFetch(`/api/tickets/${encodeURIComponent(ticketId)}`);

/**
 * QR images require the customer's JWT. The token is never placed in the URL
 * or rendered as text; the authenticated response is converted to a short-lived
 * object URL for the image tag.
 */
export async function fetchTicketQrUrl(ticketId) {
  const token = localStorage.getItem("cinepassToken");

  const response = await fetch(
    apiUrl(`/api/tickets/${encodeURIComponent(ticketId)}/qr`),
    {
      headers: {
        Accept: "image/png",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }
  );

  if (!response.ok) {
    let message = `Unable to load the ticket QR code (${response.status})`;

    try {
      const data = await response.json();
      message = data?.message || message;
    } catch {
      // Keep the safe fallback message.
    }

    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  const blob = await response.blob();

  if (!blob.size) {
    throw new Error("The ticket QR code is empty");
  }

  return URL.createObjectURL(blob);
}

// STAFF VERIFICATION
export async function verifyTicket(verificationToken) {
  const token = localStorage.getItem("cinepassToken");

  const response = await fetch(apiUrl("/api/staff/tickets/verify"), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ verificationToken }),
  });

  let data = null;

  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (response.status === 400 && data?.result) {
    return data;
  }

  if (!response.ok) {
    const error = new Error(
      data?.message || `Ticket verification failed (${response.status})`
    );
    error.status = response.status;
    throw error;
  }

  return data;
}

// ADMIN STAFF MANAGEMENT
export const getStaffAccounts = () => apiFetch("/api/admin/staff");

export const createStaffAccount = (payload) =>
  apiFetch("/api/admin/staff", {
    method: "POST",
    body: JSON.stringify(payload),
  });
