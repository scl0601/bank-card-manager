import ssl, tempfile, pprint
pem = ssl.get_server_certificate(('bankaiscl.top', 443))
print(pem.split('\n')[0:2])
with tempfile.NamedTemporaryFile('w', delete=False, suffix='.pem') as f:
    f.write(pem)
    path=f.name
pprint.pp(ssl._ssl._test_decode_cert(path))
