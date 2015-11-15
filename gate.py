#!/usr/bin/env python
# -*- coding: utf-8 -*-

import socket
import sys

MSGLEN = 1024


class SocketWrapper:
    def __init__(self, sock=None):
        if sock is None:
            self.sock = socket.socket()
        else:
            self.sock = sock

    def bind(self, host, port):
    	self.sock.bind((host, port))
    	self.sock.listen(10)

    def connect(self, host, port):
        self.sock.connect((host, port))

    def send(self, msg):
        total_sent = 0
        while total_sent < MSGLEN:
            sent = self.sock.send(msg[total_sent:])
            if sent == 0:
                break
            total_sent += sent

    def receive(self):
        chunks = []
        bytes_recd = 0
        while bytes_recd < MSGLEN:
            chunk = self.sock.recv(min(MSGLEN - bytes_recd, 2048)).decode()
            if chunk == '':
            	break
            chunks.append(chunk)
            bytes_recd += len(chunk)
        return ''.join(chunks)

    def close(self):
    	self.sock.close()


def server(log_file):
	sw = SocketWrapper()
	sw.bind('', 9090)

	print 'Server started'

	while True:
		connect, address = sw.sock.accept()
		client = SocketWrapper(connect)
		data = client.receive()
		if data:
			log_file.write(data + '\n')

	sw.close()


def client():
	sw = SocketWrapper()
	sw.connect('localhost', 9090)
	sw.send('Hello server')
	sw.close()


def main():
	if len(sys.argv) > 1:
		if sys.argv[1] == 'server':
			with open('log.txt') as log_file:
				server(log_file)
		elif sys.argv[1] == 'client':
			client()
		else:
			print 'Unknown argument (%s)' % sys.argv[1]

if __name__ == '__main__':
	main()