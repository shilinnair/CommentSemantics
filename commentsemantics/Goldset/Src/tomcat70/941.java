/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.coyote.http11.upgrade;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Allows data to be written to the upgraded connection.
 * 
 * @deprecated  Will be removed in Tomcat 8.0.x.
 */
@Deprecated
public class UpgradeOutbound extends OutputStream {

    @Override
    public void flush() throws IOException {
        processor.flush();
    }

    private final UpgradeProcessor<?> processor;

    public  UpgradeOutbound(UpgradeProcessor<?> processor) {
        this.processor = processor;
    }

    @Override
    public void write(int b) throws IOException {
        processor.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        processor.write(b, off, len);
    }
}
