const usdFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatUsd(amount: number): string {
  if (!Number.isFinite(amount)) {
    throw new RangeError("USD amounts must be finite numbers.");
  }

  return usdFormatter.format(amount);
}
