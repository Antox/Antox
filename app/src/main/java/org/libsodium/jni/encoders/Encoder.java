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

package org.libsodium.jni.encoders;

import java.nio.charset.Charset;

public interface Encoder {

    public static final Charset CHARSET = Charset.forName("US-ASCII");

    public static final Hex HEX = new Hex();
    public static final Raw RAW = new Raw();

    public byte[] decode(String data);

    public String encode(final byte[] data);
}
