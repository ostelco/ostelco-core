export const getTextType = (text) => {
  const isPhoneNumber = /^[+]?\d+$/g.test(text);
  const isEmail = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/.test(text);
  if (isPhoneNumber) return 'phonenumber';
  if (isEmail) return 'email';
  return 'unknown';
}

export const humanReadableBytes = (sizeInBytes) => {
  var i = -1;
  var byteUnits = ['KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
  do {
    sizeInBytes = sizeInBytes / 1024;
    i++;
  } while (sizeInBytes > 1024);
  return `${Math.max(sizeInBytes, 0.1).toFixed(2)} ${byteUnits[i]}`;
}

export const convertTimestampToDate = (timestamp) => {
  const date = new Date(timestamp);
  return date.toLocaleString();
}

export const isChrome = () => {
  // please note,
  // that IE11 now returns undefined again for window.chrome
  // and new Opera 30 outputs true for window.chrome
  // but needs to check if window.opr is not undefined
  // and new IE Edge outputs to true now for window.chrome
  // and if not iOS Chrome check
  // so use the below updated condition
  var isChromium = window.chrome;
  var winNav = window.navigator;
  var vendorName = winNav.vendor;
  var isOpera = typeof window.opr !== "undefined";
  var isIEedge = winNav.userAgent.indexOf("Edge") > -1;
  var isIOSChrome = winNav.userAgent.match("CriOS");

  if (isIOSChrome) {
    // is Google Chrome on IOS
    return false;
  } else if (
    isChromium !== null &&
    typeof isChromium !== "undefined" &&
    vendorName === "Google Inc." &&
    isOpera === false &&
    isIEedge === false
  ) {
    // is Google Chrome
    return true;
  } else {
    // not Google Chrome
    return false;
  }
}

export const encodeEmail = (email) => (email ? encodeURIComponent(email) : email);
