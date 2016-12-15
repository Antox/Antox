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

package org.libsodium.jni.keys;

import static org.libsodium.jni.SodiumConstants.SECRETKEY_BYTES;
import static org.libsodium.jni.crypto.Util.checkLength;
import static org.libsodium.jni.encoders.Encoder.HEX;

public class PrivateKey {

    private final byte[] secretKey;

    public PrivateKey(byte[] secretKey) {
        this.secretKey = secretKey;
        checkLength(secretKey, SECRETKEY_BYTES);
    }

    public PrivateKey(String secretKey) {
        this.secretKey = HEX.decode(secretKey);
        checkLength(this.secretKey, SECRETKEY_BYTES);
    }

    public byte[] toBytes() {
        return secretKey;
    }

    @Override
    public String toString() {
        return HEX.encode(secretKey);
    }
}
