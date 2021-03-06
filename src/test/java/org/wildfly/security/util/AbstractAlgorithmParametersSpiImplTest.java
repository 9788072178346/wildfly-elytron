/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.security.util;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;

import org.junit.Test;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.password.spec.OneTimePasswordAlgorithmSpec;
import org.wildfly.security.password.util.PasswordUtil;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class AbstractAlgorithmParametersSpiImplTest {

    @Test
    public void shouldRoundTripParameterSpecs() throws GeneralSecurityException, IOException {
        final OneTimePasswordAlgorithmSpec start = new OneTimePasswordAlgorithmSpec("otp-sha1",
                PasswordUtil.generateRandomSalt(16), 14);

        final AlgorithmParameters oneWay = AlgorithmParameters.getInstance("otp-sha1", new WildFlyElytronProvider());
        oneWay.init(start);

        final byte[] encoded = oneWay.getEncoded();

        final AlgorithmParameters orAnother = AlgorithmParameters.getInstance("otp-sha1", new WildFlyElytronProvider());

        orAnother.init(encoded);

        final OneTimePasswordAlgorithmSpec end = orAnother.getParameterSpec(OneTimePasswordAlgorithmSpec.class);

        assertEquals(start.getAlgorithm(), end.getAlgorithm());
        assertArrayEquals(start.getSeed(), end.getSeed());
        assertEquals(start.getSequenceNumber(), end.getSequenceNumber());
    }
}
