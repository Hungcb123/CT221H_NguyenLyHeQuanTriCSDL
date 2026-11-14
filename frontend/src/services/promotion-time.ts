// SQL Server stores campaign times in UTC without a JSON timezone suffix.
export function promotionDate(value: string): Date {
  return new Date(/[zZ]|[+-]\d{2}:\d{2}$/.test(value) ? value : value + "Z");
}
export function promotionInput(value: string): string {
  const date = promotionDate(value);
  const pad = (n: number) => String(n).padStart(2, "0");
  return date.getFullYear() + "-" + pad(date.getMonth()+1) + "-" + pad(date.getDate()) + "T" + pad(date.getHours()) + ":" + pad(date.getMinutes());
}
export function promotionUtc(value: string): string {
  return new Date(value).toISOString().slice(0,19);
}
export function promotionTime(value: string): string {
  return promotionDate(value).toLocaleString("vi-VN");
}
