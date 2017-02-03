/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.unimore.shibboleth.idp.authn.impl

import it.unimore.util.Slurper

import groovy.util.logging.Slf4j
import net.shibboleth.idp.authn.AbstractValidationAction
import net.shibboleth.idp.authn.AuthnEventIds
import net.shibboleth.idp.authn.context.AuthenticationContext
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty
import org.opensaml.profile.context.ProfileRequestContext

import javax.annotation.Nonnull
import javax.security.auth.Subject

import net.shibboleth.idp.authn.AuthnEventIds
import net.shibboleth.idp.authn.ExternalAuthentication
import net.shibboleth.idp.authn.ExternalAuthenticationException
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty
import net.shibboleth.utilities.java.support.resolver.CriteriaSet
import net.shibboleth.idp.authn.principal.UsernamePrincipal

import javax.security.auth.Subject
import javax.servlet.ServletConfig
import javax.servlet.ServletException
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.security.Principal


@Slf4j
class X509UnimoreAuthServlet extends HttpServlet {


// Checkstyle: CyclomaticComplexity|ReturnCount OFF
    /** {@inheritDoc} */
    @Override
    protected void service(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse)
            throws ServletException, IOException {


        /** Parameter/cookie for bypassing prompt page. */
        @Nonnull @NotEmpty final String PASSTHROUGH_PARAM = "x509passthrough";

        Slurper slurper

        for (header in httpRequest.getHeaderNames() ) {
            log.info("header: {}", header)
            String value = httpRequest.getHeader(header)
            log.info("with value: {}", value)
            String configLocation = getInitParameter("configLocation")
            log.info("configLocation: {}", configLocation)
            slurper = new Slurper(configLocation)
        }

        try {
            final String key = ExternalAuthentication.startExternalAuthentication(httpRequest)
            
            final String passthrough = httpRequest.getParameter(PASSTHROUGH_PARAM);
            if (passthrough != null && Boolean.parseBoolean(passthrough)) {
                log.debug("Setting UI passthrough cookie");
                final Cookie cookie = new Cookie(PASSTHROUGH_PARAM, "1");
                cookie.setPath(httpRequest.getContextPath());
                cookie.setMaxAge(60 * 60 * 24 * 365);
                cookie.setSecure(true);
                httpResponse.addCookie(cookie);
            }
            
            final Subject subject = new Subject()
            String rawDn = httpRequest.getHeader("SSL_CLIENT_S_DN")
            def pattern = slurper.fetch("X509External.cnTransform.regex")
            def matcher = (rawDn =~ pattern)
            def dn = ""
            if ( matcher[0] ) {
                dn = matcher[0][1]
            }
            Principal principal = new UsernamePrincipal(dn)
            subject.getPrincipals().add(principal);
            log.info("subject principal: {}", principal)
            httpRequest.setAttribute(ExternalAuthentication.SUBJECT_KEY, subject);

            final String revokeConsent =
                    httpRequest.getParameter(ExternalAuthentication.REVOKECONSENT_KEY);
            if (revokeConsent != null && ("1".equals(revokeConsent) || "true".equals(revokeConsent))) {
                httpRequest.setAttribute(ExternalAuthentication.REVOKECONSENT_KEY, Boolean.TRUE);
            }

            ExternalAuthentication.finishExternalAuthentication(key, httpRequest, httpResponse);
            
        } catch (final ExternalAuthenticationException e) {
            throw new ServletException("Error processing external authentication request", e);
        }
    }
// Checkstyle: CyclomaticComplexity|ReturnCount ON
    
}
