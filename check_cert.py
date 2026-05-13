import ssl, socket, pprint
host='bankaiscl.top'
ctx=ssl.create_default_context()
ctx.check_hostname=False
ctx.verify_mode=ssl.CERT_NONE
with socket.create_connection((host,443),timeout=10) as sock:
    with ctx.wrap_socket(sock, server_hostname=host) as ssock:
        print(ssock.version())
        pprint.pp(ssock.getpeercert(binary_form=False))
