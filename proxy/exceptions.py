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


class DeviceWriteError(Exception):
    def __init__(self, status, *args, **kwargs):
        super(Exception, self).__init__(*args, **kwargs)
        self.status = status

    def __str__(self):
        return 'Device can not be written (%s)' % self.status


class InsertModuleError(Exception):
    def __init__(self, result_code, *args, **kwargs):
        super(Exception, self).__init__(*args, **kwargs)
        self.result_code = result_code

    def __str__(self):
        return 'Error during inserting module (code=%s)' % self.result_code

class RemoveModuleError(Exception):
    def __init__(self, result_code, *args, **kwargs):
        super(Exception, self).__init__(*args, **kwargs)
        self.result_code = result_code

    def __str__(self):
        return 'Error during removing module (code=%s)' % self.result_code
