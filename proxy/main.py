#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
from components import MouseDriver, Server, Client

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
