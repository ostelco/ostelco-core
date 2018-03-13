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

export function splitPathComponents(urlPath: string) {
  if (!isString(urlPath)) {
    return [];
  }
  if (urlPath.charAt(0) === "/") {
    urlPath = urlPath.slice(1);
  }
  return urlPath.split("/");
}
