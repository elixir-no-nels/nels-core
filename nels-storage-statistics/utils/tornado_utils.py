from tornado import gen

def coroutine_return():
    raise gen.Return()

def return_with_status(hndlr, status_code, response_key=None, response_val=None):
    hndlr.set_header('Content-Type', 'application/json')
    hndlr.set_status(status_code)
    if response_key and response_val:
        hndlr.write({response_key:response_val})
    coroutine_return()

def return_201(hndlr, new_id):
    return_with_status(hndlr, 201, 'id', new_id)

def return_400(hndlr, msgs):
    return_with_status(hndlr, 400, "msgs", msgs)

def return_404(hndlr):
    return_with_status(hndlr, 404)

def return_500(hndlr):
    return_with_status(hndlr, 500)

def return_501(hndlr):
    return_with_status(hndlr, 500)

def return_data(hndlr, data):
    return_with_status(hndlr, 200, "data", data)

