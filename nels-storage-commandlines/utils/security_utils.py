import base64
from Crypto.Cipher import AES
import md5

BS = 16
pad = lambda s: s + (BS - len(s) % BS) * chr(BS - len(s) % BS) 
unpad = lambda s : s[:-ord(s[len(s)-1:])]

def transform_key(key):
    return md5.new(key).hexdigest()[:16]

def encrypt(key, plain_text ):
    #0. transform key 1. pad text,  2. encrypt 3. hex encode
    cipher = AES.new( transform_key(key), AES.MODE_ECB)
    return base64.b64encode( cipher.encrypt( pad(plain_text) )).encode("hex")

def decrypt(key, cipher_text ):
    #0. transform key 1. hex decode  2. decrypt  3. unpad
    cipher = AES.new(transform_key(key), AES.MODE_ECB)
    return unpad( cipher.decrypt(base64.b64decode(cipher_text.decode("hex"))))

    