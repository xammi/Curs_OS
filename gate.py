#!/usr/bin/env python
# -*- coding: utf-8 -*-

import socket
import sys
import signal

MSGLEN = 1024
DEFAULT_PORT = 9090
DEFAULT_DEV_PATH = '/dev/input/mouse1'
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
        if self.sock:
    	   self.sock.close()


#--------------------------------------------------------------------------------------------------

class InvalidCommand(Exception):
    def __init__(self, command, *args, **kwargs):
        super(Exception, self).__init__(*args, **kwargs)
        self.command = command

    def __str__(self):
        return 'Invalid command (%s)' % self.command


class UnknownCommand(Exception):
    def __init__(self, command, *args, **kwargs):
        super(Exception, self).__init__(*args, **kwargs)
        self.command = command

    def __str__(self):
        return 'Unknown command (%s)' % self.command


class InvalidMouseArgs(Exception):
    def __init__(self, params, *args, **kwargs):
        super(Exception, self).__init__(*args, **kwargs)
        self.params = params

    def __str__(self):
        return 'Invalid mouse args (%s). Expected 1 string' % self.params



class InvalidGyroArgs(Exception):
    def __init__(self, params, *args, **kwargs):
        super(Exception, self).__init__(*args, **kwargs)
        self.params = params

    def __str__(self):
        return 'Invalid gyro args (%s). Expected 3 floats' % self.params


class InvalidScrollArgs(Exception):
    def __init__(self, params, *args, **kwargs):
        super(Exception, self).__init__(*args, **kwargs)
        self.params = params

    def __str__(self):
        return 'Invalid scroll args (%s). Expected 2 floats' % self.params

#--------------------------------------------------------------------------------------------------


class MouseDriver:
    CommandMap = {
        'click': {'LEFT': 1, 'RIGHT': 2, 'MIDDLE': 3, 'EXTRA': 4},
        'gyro': 5,
        'scroll': 6
    }

    def __init__(self, device_name=DEFAULT_DEV_PATH):
        self.device = open(device_name, 'w')

    def _gyro_to_coords(self, args):
        if len(args) != 3:
            raise InvalidGyroArgs(args)
        # interpret gyro
        return 0, 0

    def write(self, command, args):
        if command not in self.CommandMap:
            raise UnknownCommand(command)

        codes = self.CommandMap[command]
        if command == 'click':
            if len(args) != 1:
                raise InvalidMouseArgs(args)

            code = codes[args]
            record = '%s:0,0' % code
        else:
            code = codes
            if command == 'scroll':
                if len(args) != 2:
                    raise InvalidScrollArgs(args)

                record = '%s:%s,%s' % (code, args[0], args[1])
            else:
                record = '%s:%s,%s' % (code, self._gyro_to_coords(args))

        self.device.write(record)

    def __del__(self):
        if self.device:
            self.device.close()


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
            args = args_str.split(',')
            driver.write(command, args)
        else:
            raise InvalidCommand(data)


    def start(self):
        while True:
            connect, address = self.sw.sock.accept()
            client = SocketWrapper(connect)
            data = client.receive()
            if data:
                log_file.write(data)
                log_file.flush()

                try:
                    self.handle_data(data)
                except Exception as e:
                    log_file.write('Exception: %s' % str(e))
                    log_file.flush()


    def __del__(self):
        self.sw.close()
        if self.log_file:
            self.log_file.close()


class Client:
    def __init__(self, port=DEFAULT_PORT):
        self.sw = SocketWrapper()
        self.sw.connect('localhost', port)

    def send(self, message):
        self.sw.send('Hello server')

    def __del__(self):
        self.sw.close()


def main():
    if len(sys.argv) > 1:
        if sys.argv[1] == 'server':
            try:
                driver = MouseDriver()

                server = Server(driver)
                print 'Server started'
                server.start()

            except IOError as e:
                print str(e)

        elif sys.argv[1] == 'client':
            client = Client()
            client.send('Hello world')

        else:
            print 'Unknown argument (%s)' % sys.argv[1]
    else:
        print 'This script needs one parameter (server|client)'


if __name__ == '__main__':
    main()
