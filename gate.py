import socket
import sys

class SocketWrapper:
    def __init__(self, sock=None):
        if sock is None:
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        else:
            self.sock = sock

    def bind(self, host, port):
    	self.sock.bind((host, port))
    	self.sock.listen(10)

    def connect(self, host, port):
        self.sock.connect((host, port))

    def send(self, msg):
        total_sent = 0
        while total_sent <= len(msg):
            sent = self.sock.send(msg[total_sent:])
            if sent == 0:
                raise RuntimeError("socket connection broken")
            total_sent += sent

    def receive(self):
        chunks = []
        bytes_recd = 0
        chunk = self.sock.recv(2048)
        while chunk != '':
            chunk = self.sock.recv(2048)
            chunks.append(chunk)
            bytes_recd += len(chunk)
        return ''.join(chunks)

    def close(self):
    	self.sock.close()


def server():
	sw = SocketWrapper()
	sw.bind('localhost', 8081)

	print 'Socket listens'

	while True:
		connect, address = sw.sock.accept()
		client = SocketWrapper(connect)
		print 'Client: %s' % address

		data = client.receive()
		if data:
			print data

	sw.close()


def client():
	sw = SocketWrapper()
	sw.connect('localhost', 8081)
	print 'Connecting ...'

	sw.send('Hello')
	print 'Sending ...'

	sw.close()


def main():
	if len(sys.argv) > 1:
		if sys.args[0] == 'server':
			server()
		elif sys.args[0] == 'client':
			client()
		else:
			print 'Unknown argument'

main()