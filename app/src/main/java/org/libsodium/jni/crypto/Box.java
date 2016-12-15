/**
 * Copyright 2013 Bruno Oliveira, and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.libsodium.jni.crypto;

import org.libsodium.jni.encoders.Encoder;
import org.libsodium.jni.keys.PrivateKey;
import org.libsodium.jni.keys.PublicKey;

import static org.libsodium.jni.SodiumConstants.BOXZERO_BYTES;
import static org.libsodium.jni.SodiumConstants.NONCE_BYTES;
import static org.libsodium.jni.SodiumConstants.PUBLICKEY_BYTES;
import static org.libsodium.jni.SodiumConstants.SECRETKEY_BYTES;
import static org.libsodium.jni.SodiumConstants.ZERO_BYTES;
import static org.libsodium.jni.NaCl.sodium;
import static org.libsodium.jni.crypto.Util.checkLength;
import static org.libsodium.jni.crypto.Util.isValid;
import static org.libsodium.jni.crypto.Util.prependZeros;
import static org.libsodium.jni.crypto.Util.removeZeros;

/**
 * Based on Curve25519XSalsa20Poly1305 and Box classes from rbnacl
 */
public class Box {

    private final byte[] privateKey;
    private final byte[] publicKey;

    public Box(byte[] publicKey, byte[] privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        checkLength(publicKey, PUBLICKEY_BYTES);
        checkLength(privateKey, SECRETKEY_BYTES);
    }

    public Box(PublicKey publicKey, PrivateKey privateKey) {
        this(publicKey.toBytes(), privateKey.toBytes());
    }

    public Box(String publicKey, String privateKey, Encoder encoder) {
        this(encoder.decode(publicKey), encoder.decode(privateKey));
    }

    public byte[] encrypt(byte[] nonce, byte[] message) {
        checkLength(nonce, NONCE_BYTES);
        byte[] msg = prependZeros(ZERO_BYTES, message);
        byte[] ct = new byte[msg.length];
        isValid(sodium().crypto_box_curve25519xsalsa20poly1305(ct, msg,
                msg.length, nonce, publicKey, privateKey), "Encryption failed");
        return removeZeros(BOXZERO_BYTES, ct);
    }

    public byte[] encrypt(String nonce, String message, Encoder encoder) {
        return encrypt(encoder.decode(nonce), encoder.decode(message));
    }

    public byte[] decrypt(byte[] nonce, byte[] ciphertext) {
        checkLength(nonce, NONCE_BYTES);
        byte[] ct = prependZeros(BOXZERO_BYTES, ciphertext);
        byte[] message = new byte[ct.length];
        isValid(sodium().crypto_box_curve25519xsalsa20poly1305_open(message, ct,
                message.length, nonce, publicKey, privateKey), "Decryption failed. Ciphertext failed verification.");
        return removeZeros(ZERO_BYTES, message);
    }

    public byte[] decrypt(String nonce, String ciphertext, Encoder encoder) {
        return decrypt(encoder.decode(nonce), encoder.decode(ciphertext));
    }
}
