// Loaded into the Firebase emulators by e2e.sh. Node closes a connection after 5 s idle, and when the device
// sends a request on it just as the close is on its way (likeliest through a slow tunnel to a remote
// emulator), Android fails the request with "unexpected end of stream" rather than retrying. Android drops
// idle connections after 5 minutes, so with a longer timeout here it is always the device that closes them.
const http = require("node:http");

const listen = http.Server.prototype.listen;
http.Server.prototype.listen = function (...args) {
  this.keepAliveTimeout = 10 * 60 * 1000;
  return listen.apply(this, args);
};
