# encoding: utf-8

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
