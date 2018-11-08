import { deflateSync } from "zlib";

export const getTextType = (text)  => {
  const isPhoneNumber = /^[+]?\d+$/g.test(text)
  const isEmail = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/.test(text)
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
  return `${Math.max(sizeInBytes, 0.1).toFixed(1)} ${byteUnits[i]}`;
}

export const convertTimestampToDate = (timestamp) => {
  const date = new Date(timestamp);
  return date.toLocaleString('en-GB', { timeZone: 'UTC' });
}
