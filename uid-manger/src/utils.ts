export function isString(x): boolean {
  return typeof x === "string";
}
export function isValidMsisdn(msisdn: string) {
  if (!isString(msisdn)) {
    return false;
  }
  const msisdnPattern = /^[1-9]\d*$/;
  return msisdnPattern.test(msisdn);
}
