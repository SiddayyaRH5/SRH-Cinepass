/**
 * Convert the backend seat number (S77) to the label shown on the existing
 * SRH CinePass seat map (H7). Keep this in one place so booking cards, digital
 * tickets and staff verification all display the same seat label.
 */
export function seatLabel(seatNumber) {
  const value = String(seatNumber ?? "").trim();
  const match = /^[Ss](\d+)$/.exec(value);

  if (!match) return value;

  const number = Number(match[1]);

  if (!Number.isInteger(number) || number < 1) return value;

  const row = Math.floor((number - 1) / 10);
  const seat = ((number - 1) % 10) + 1;

  return `${String.fromCharCode(65 + row)}${seat}`;
}

export function seatLabels(seats) {
  return Array.isArray(seats) ? seats.map(seatLabel).filter(Boolean) : [];
}
