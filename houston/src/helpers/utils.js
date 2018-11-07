
function humanReadableBytes(sizeInBytes) {
  var i = -1;
  var byteUnits = ['KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
  do {
    sizeInBytes = sizeInBytes / 1024;
    i++;
  } while (sizeInBytes > 1024);
  return `${Math.max(sizeInBytes, 0.1).toFixed(1)} ${byteUnits[i]}`;
}

export {
  humanReadableBytes
};
