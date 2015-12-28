#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
from signal import signal, SIGINT
from components import MouseDriver, Server, Client, ins_kernel_module, rm_kernel_module


driver = None
server = None


def stop_server(concrete_signal, frame):
    print '\n'
    try:
        if server:
            server.stop()
            print 'Server stopped'
    except IOError as e:
        print str(e)

    try:
        if driver:
            driver.close()
            print 'Driver stopped'
    except IOError as e:
        print str(e)

    try:
        rm_kernel_module()
        print 'Kernel module unloaded'
    except Exception as e:
        print str(e)


def start_server():
    global driver
    global server

    try:
        ins_kernel_module()
        print 'Kernel module loaded'

        driver = MouseDriver()
        print 'Driver ready'

        server = Server(driver)
        print 'Server started'
        server.start()

    except Exception as e:
        print str(e)


def start_client():
    client = Client()
    client.send('Hello world')
    client.close()


def main():
    if len(sys.argv) > 1:
        if sys.argv[1] == 'server':
            signal(SIGINT, stop_server)
            start_server()
        elif sys.argv[1] == 'client':
            start_client()
        else:
            print 'Unknown argument (%s)' % sys.argv[1]
    else:
        print 'This script needs one parameter (server|client)'


if __name__ == '__main__':
    main()
