/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat, Inc., and individual contributors
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

package org.wildfly.security.sasl.digest;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;

import org.kohsuke.MetaInfServices;
import org.wildfly.common.Assert;
import org.wildfly.security._private.ElytronMessages;
import org.wildfly.security.auth.callback.AvailableRealmsCallback;
import org.wildfly.security.sasl.WildFlySasl;

/**
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 *
 */
@MetaInfServices(value = SaslServerFactory.class)
public class DigestServerFactory extends AbstractDigestFactory implements SaslServerFactory {

    private final Supplier<Provider[]> providers;

    public DigestServerFactory() {
        providers = Security::getProviders;
    }

    public DigestServerFactory(final Provider provider) {
        providers = () -> new Provider[] { provider };
    }

    /* (non-Javadoc)
     * @see javax.security.sasl.SaslServerFactory#createSaslServer(java.lang.String, java.lang.String, java.lang.String, java.util.Map, javax.security.auth.callback.CallbackHandler)
     */
    @Override
    public SaslServer createSaslServer(String mechanism, String protocol, String serverName, Map<String, ?> props,
            CallbackHandler cbh) throws SaslException {
        if (! matches(props) || ! matchesMech(mechanism)) {
            return null;
        }
        Assert.checkNotNullParam("cbh", cbh);
        if (props == null) props = Collections.emptyMap();

        String[] realms = null;
        final AvailableRealmsCallback availableRealmsCallback = new AvailableRealmsCallback();
        try {
            cbh.handle(new Callback[] { availableRealmsCallback });
            realms = availableRealmsCallback.getRealmNames();
        } catch (UnsupportedCallbackException ignored) {
        } catch (SaslException e) {
            throw e;
        } catch (IOException e) {
            throw ElytronMessages.log.mechCallbackHandlerFailedForUnknownReason(mechanism, e).toSaslException();
        }
        if (realms == null) {
            realms = new String[] { serverName };
        }

        Boolean utf8 = (Boolean)props.get(WildFlySasl.USE_UTF8);
        Charset charset = (utf8==null || utf8.booleanValue()) ? StandardCharsets.UTF_8 : StandardCharsets.ISO_8859_1;

        String qopsString = (String)props.get(Sasl.QOP);
        String[] qops = qopsString==null ? null : qopsString.split(",");

        String supportedCipherOpts = (String)props.get(WildFlySasl.SUPPORTED_CIPHER_NAMES);
        String[] cipherOpts = (supportedCipherOpts == null ? null : supportedCipherOpts.split(","));

        final String defaultDigestUri = (protocol + "/" + serverName).toLowerCase(Locale.ROOT);
        Predicate<String> digestUriTest = defaultDigestUri::equals;

        String alternativeProtocols = (String)props.get(WildFlySasl.ALTERNATIVE_PROTOCOLS);
        if (alternativeProtocols == null) alternativeProtocols = (String)props.get(WildFlySasl.ALTERNATIVE_PROTOCOLS);
        if (alternativeProtocols != null) {
            final Set<String> acceptableUris = new HashSet<>();
            StringTokenizer parser = new StringTokenizer(alternativeProtocols, ", \t\n");
            while (parser.hasMoreTokens()) {
                String digestUri = (parser.nextToken().trim() + "/" + serverName).toLowerCase(Locale.ROOT);
                acceptableUris.add(digestUri);
            }
            digestUriTest = digestUriTest.or(acceptableUris::contains);
        }

        final DigestSaslServer server = new DigestSaslServer(realms, mechanism, protocol, serverName, cbh, charset, qops, cipherOpts, digestUriTest, providers);
        server.init();
        return server;
    }
}
