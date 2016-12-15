/**
 *
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

package org.libsodium.jni;


import java.util.logging.Level;
import java.util.logging.Logger;

public class NaCl {
    private static final Logger LOGGER=Logger.getLogger(NaCl.class.getName());

    static {
        String librarypath=System.getProperty("java.library.path");
        LOGGER.log(Level.INFO,"librarypath="+librarypath);
        System.loadLibrary("sodiumjni");
    }

    public static Sodium sodium() {
        Sodium.sodium_init();
        return SingletonHolder.SODIUM_INSTANCE;
    }
    
    private static final class SingletonHolder {
        public static final Sodium SODIUM_INSTANCE = new Sodium();
    }
    
    private NaCl() {
    }
}
