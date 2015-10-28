/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
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

package org.wildfly.security.auth.provider;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.auth.server.SupportLevel;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.evidence.Evidence;

/**
 * A realm which directs authentication to one realm and authorization to another.  The authentication realm need
 * not provide any authorization information.  Likewise the authorization realm need not provide any authentication
 * credential acquisition or verification capabilities.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AggregateSecurityRealm implements SecurityRealm {
    private final SecurityRealm authenticationRealm;
    private final SecurityRealm authorizationRealm;

    /**
     * Construct a new instance.
     *
     * @param authenticationRealm the realm to use for authentication
     * @param authorizationRealm the realm to use for authorization
     */
    public AggregateSecurityRealm(final SecurityRealm authenticationRealm, final SecurityRealm authorizationRealm) {
        this.authenticationRealm = authenticationRealm;
        this.authorizationRealm = authorizationRealm;
    }

    public RealmIdentity createRealmIdentity(final String name) throws RealmUnavailableException {
        boolean ok = false;
        final RealmIdentity authenticationIdentity = authenticationRealm.createRealmIdentity(name);
        try {
            final RealmIdentity authorizationIdentity = authorizationRealm.createRealmIdentity(name);
            try {
                final Identity identity = new Identity(authenticationIdentity, authorizationIdentity);
                ok = true;
                return identity;
            } finally {
                if (! ok) authorizationIdentity.dispose();
            }
        } finally {
            if (! ok) authenticationIdentity.dispose();
        }
    }

    public SupportLevel getCredentialAcquireSupport(final String credentialName) throws RealmUnavailableException {
        return authenticationRealm.getCredentialAcquireSupport(credentialName);
    }

    @Override
    public SupportLevel getEvidenceVerifySupport(String credentialName) throws RealmUnavailableException {
        return authenticationRealm.getEvidenceVerifySupport(credentialName);
    }



    static final class Identity implements RealmIdentity {

        private final RealmIdentity authenticationIdentity;
        private final RealmIdentity authorizationIdentity;

        Identity(final RealmIdentity authenticationIdentity, final RealmIdentity authorizationIdentity) {
            this.authenticationIdentity = authenticationIdentity;
            this.authorizationIdentity = authorizationIdentity;
        }

        public SupportLevel getCredentialAcquireSupport(final String credentialName) throws RealmUnavailableException {
            return authenticationIdentity.getCredentialAcquireSupport(credentialName);
        }

        @Override
        public Credential getCredential(String credentialName) throws RealmUnavailableException {
            return authenticationIdentity.getCredential(credentialName);
        }

        @Override
        public <C extends Credential> C getCredential(String credentialName, Class<C> credentialType, Set<String> supportedAlgorithms) throws RealmUnavailableException {
            return authenticationIdentity.getCredential(credentialName, credentialType, supportedAlgorithms);
        }

        @Override
        public Credential getCredential(List<String> credentialNames, Map<Class<? extends Credential>, Set<String>> supportedTypesWithAlgorithms) throws RealmUnavailableException {
            return authenticationIdentity.getCredential(credentialNames, supportedTypesWithAlgorithms);
        }

        public <C extends Credential> C getCredential(final String credentialName, final Class<C> credentialType) throws RealmUnavailableException {
            return authenticationIdentity.getCredential(credentialName, credentialType);
        }

        @Override
        public SupportLevel getEvidenceVerifySupport(String credentialName) throws RealmUnavailableException {
            return authenticationIdentity.getEvidenceVerifySupport(credentialName);
        }

        public boolean verifyEvidence(final String credentialName, final Evidence credential) throws RealmUnavailableException {
            return authenticationIdentity.verifyEvidence(credentialName, credential);
        }

        public boolean exists() throws RealmUnavailableException {
            return authenticationIdentity.exists();
        }

        public AuthorizationIdentity getAuthorizationIdentity() throws RealmUnavailableException {
            return authorizationIdentity.exists() ? authorizationIdentity.getAuthorizationIdentity() : AuthorizationIdentity.EMPTY;
        }

        public void dispose() {
            authenticationIdentity.dispose();
            authorizationIdentity.dispose();
        }
    }
}