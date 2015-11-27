# encoding: utf-8

import socket
import os
from exceptions import *

MSG_LEN = 1024
DEFAULT_PORT = 9090
DEFAULT_DEV_PATH = '/sys/devices/platform/virtual_mouse/coordinates'
DEFAULT_LOG_PATH = './log.txt'


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
        while total_sent < MSG_LEN:
            sent = self.sock.send(msg[total_sent:])
            if sent == 0:
                break
            total_sent += sent

    def receive(self):
        chunks = []
        bytes_recd = 0
        while bytes_recd < MSG_LEN:
            chunk = self.sock.recv(min(MSG_LEN - bytes_recd, 2048)).decode()
            if chunk == '':
                break
            chunks.append(chunk)
            bytes_recd += len(chunk)
        return ''.join(chunks)

    def close(self):
        if self.sock:
            self.sock.close()


class MouseDriver:
    CommandMap = {
        'click': {'LEFT': 1, 'RIGHT': 2, 'MIDDLE': 3, 'EXTRA': 4},
        'gyro': 5,
        'scroll': 6
    }

    def __init__(self, device_name=DEFAULT_DEV_PATH):
        self.device = os.open(device_name, os.O_RDWR)
        self.X = 100
        self.Y = 100

    def _gyro_to_coords(self, args):
        if len(args) != 3:
            raise InvalidGyroArgs(args)
        dX = float(args[0])
        dY = float(args[1])

        self.X += dX
        self.Y += dY
        return self.X, self.Y

    def write(self, command, args):
        if command not in self.CommandMap:
            raise UnknownCommand(command)

        codes = self.CommandMap[command]
        if command == 'click':
            if len(args) != 1:
                raise InvalidMouseArgs(args)
            button = args[0]

            code = codes[button]
            record = '%s:0,0' % code
        else:
            code = codes
            if command == 'scroll':
                if len(args) != 2:
                    raise InvalidScrollArgs(args)

                record = '%s:%s,%s' % (code, args[0], args[1])
            else:
                X, Y = self._gyro_to_coords(args)
                record = '%s:%s,%s' % (code, X, Y)

        written_bytes = os.write(self.device, record)
        if written_bytes <= 0:
        	raise DeviceWriteError(written_bytes)

        # os.flush(self.device)

    def __del__(self):
        if self.device:
            os.close(self.device)


class Server:
    def __init__(self, driver, port=DEFAULT_PORT, log_name=DEFAULT_LOG_PATH):
        self.driver = driver
        self.log_file = open(log_name, 'wb')
        self.sw = SocketWrapper()
        self.sw.bind('', port)

    def handle_data(self, data):
        data_parts = data.split(':')
        if len(data_parts) == 2:
            command, args_str = data_parts
            command = command.strip()
            args_str = args_str.strip()
            args = args_str.split(',')
            self.driver.write(command, args)
        else:
            raise InvalidCommand(data)

    def start(self):
        while True:
            connect, address = self.sw.sock.accept()
            client = SocketWrapper(connect)
            data = client.receive()
            if data:
                self.log_file.write(data)
                self.log_file.flush()

                try:
                    self.handle_data(data)
                except Exception as e:
                    self.log_file.write('Exception: %s\n' % str(e))
                    self.log_file.flush()

    def __del__(self):
        self.sw.close()
        if self.log_file:
            self.log_file.close()


class Client:
    def __init__(self, port=DEFAULT_PORT):
        self.sw = SocketWrapper()
        self.sw.connect('localhost', port)

    def send(self, message):
        self.sw.send(message)

    def __del__(self):
        self.sw.close()