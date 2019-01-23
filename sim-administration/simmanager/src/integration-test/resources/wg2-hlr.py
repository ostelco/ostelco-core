#! /usr/bin/python

import os
from http.server import BaseHTTPRequestHandler, HTTPServer

PORT = 9081

class handler(BaseHTTPRequestHandler):

    def do_GET(self):
        paths = [
            '/ping',
        ]
        if self.path in paths:
            self.send_response(200)
            self.send_header('Content-Type', 'text/plain')
            self.end_headers()
            self.wfile.write(bytes('pong', 'UTF-8'))
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        paths = [
            '/default/provision/activate',
        ]
        if self.path in paths:
            self.send_response(201)
            self.end_headers()
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == '__main__':
    server_class = HTTPServer
    httpd = server_class(("", PORT), handler)
    httpd.serve_forever()
