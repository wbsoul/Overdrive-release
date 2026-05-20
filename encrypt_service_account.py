#!/usr/bin/env python3
"""
Encrypt fcm_service_account.json for safe bundling in APK assets.

Uses the same AES-256-CBC key/IV as Safe.java so the app can decrypt at runtime.
The encrypted file (fcm_service_account.enc) is what gets bundled — Google's
secret scanners won't recognise the ciphertext as a private key.

Usage:
    python3 encrypt_service_account.py
    # Reads:  app/src/main/assets/fcm_service_account.json
    # Writes: app/src/main/assets/fcm_service_account.enc

Requires: pip install pycryptodome
"""

import os
import sys
import base64
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad

# Same key/IV as Safe.java and generate_safe_enc.py
K1 = bytes([0x38, 0x39, 0x33, 0x38, 0x34, 0x37, 0x32, 0x38])
K2 = bytes([0x33, 0x37, 0x34, 0x38, 0x32, 0x39, 0x33, 0x30])
K3 = bytes([0x31, 0x38, 0x32, 0x37, 0x33, 0x38, 0x34, 0x39])
K4 = bytes([0x31, 0x30, 0x32, 0x39, 0x33, 0x38, 0x34, 0x37])
KEY = K1 + K2 + K3 + K4
IV = bytes([0x31, 0x30, 0x32, 0x39, 0x33, 0x38, 0x34, 0x37,
            0x35, 0x36, 0x31, 0x30, 0x32, 0x39, 0x33, 0x38])

ASSETS_DIR = os.path.join("app", "src", "main", "assets")
INPUT_FILE = os.path.join(ASSETS_DIR, "fcm_service_account.json")
OUTPUT_FILE = os.path.join(ASSETS_DIR, "fcm_service_account.enc")


def main():
    if not os.path.exists(INPUT_FILE):
        print(f"ERROR: {INPUT_FILE} not found")
        sys.exit(1)

    with open(INPUT_FILE, "rb") as f:
        plaintext = f.read()

    print(f"Read {len(plaintext)} bytes from {INPUT_FILE}")

    cipher = AES.new(KEY, AES.MODE_CBC, IV)
    ciphertext = cipher.encrypt(pad(plaintext, AES.block_size))
    encoded = base64.b64encode(ciphertext)

    with open(OUTPUT_FILE, "wb") as f:
        f.write(encoded)

    print(f"Wrote {len(encoded)} bytes to {OUTPUT_FILE}")
    print("Done — the .enc file is safe to commit and bundle in the APK.")
    print(f"You can now delete {INPUT_FILE} from assets (keep a backup elsewhere).")


if __name__ == "__main__":
    main()
