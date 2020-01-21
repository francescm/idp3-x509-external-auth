> This project is work in progress.

# Shibboleth Idp3 X509 as External Auth

## Notes
Works with Shibboleth Identity Provider from 3.3.0 to 3.4.5.

## Motivations
X509 auth requires the SSLContext to be available to 
the Auth engine. Unfortunately, if your setup includes 
jetty9.3 behind a apache2 reverse proxy, there is 
no simple way to have it, because jetty9.3 
deprecates `mod_ajp`.

The apache2 `+ExportCertData` won't work pass
X509 cert data to jetty on mod_http
 
    <Location /idp/Authn/X509>
    # this does not pass SSLContext via mod_http
     SSLVerifyClient require
     SSLVerifyDepth 5
     SSLOptions -StdEnvVars +ExportCertData
    </Location>

Following the Shibboleth documentation, where X509 
flow is defined as 
[from Shibboleth official documentation](https://wiki.shibboleth.net/confluence/display/IDP30/X509AuthnConfiguration)

    This flow is implemented as a special case of the External    

## Proposed workaround

1. have apache2 turn the X509 user cert data to header 
   via mod_headers:
   
   ```
   # initialize the special headers to a blank value to avoid 
   #  http header forgeries
   # in a Italian CNS/CIE
   # SSL_CLIENT_S_DN_G is givenName
   # SSL_CLIENT_S_DN_S is surname
   RequestHeader set SSL_CLIENT_S_DN     ""
   RequestHeader set SSL_CLIENT_S_DN_CN  ""
   RequestHeader set SSL_CLIENT_S_DN_G  ""
   RequestHeader set SSL_CLIENT_S_DN_S  ""
   <Location /idp/Authn/External>
     SSLVerifyClient require
     SSLVerifyDepth 5
     SSLUserName SSL_CLIENT_S_DN_CN #cosmetic only: 
                                    # apache2 logs 
                                    # X509 DN_CN 
                                    # as username 
     # SSLOptions -StdEnvVars +ExportCertData
     RequestHeader set SSL_CLIENT_S_DN "%{SSL_CLIENT_S_DN}s"
     RequestHeader set SSL_CLIENT_S_DN_CN "%{SSL_CLIENT_S_DN_CN}s"
     RequestHeader set SSL_CLIENT_S_DN_S "%{SSL_CLIENT_S_DN_S}s"
     RequestHeader set SSL_CLIENT_S_DN_G "%{SSL_CLIENT_S_DN_G}s"
   </Location>
   ``` 
2. write a specialized ExternalAuthnConfiguration to 
   deal with this case.

##Enable a new X509External auth method on IdPv3

1. Edit: `conf/authn/general-authn.xml` and create a bean:

   ```       
       <bean id="authn/X509External" parent="shibboleth.AuthenticationFlow"
                   p:nonBrowserSupported="false" />
          <property name="supportedPrincipals">
              <list>
                 <bean parent="shibboleth.SAML2AuthnContextClassRef"
                    c:classRef="urn:oasis:names:tc:SAML:2.0:ac:classes:X509" />
                 <bean parent="shibboleth.SAML2AuthnContextClassRef"
                    c:classRef="urn:oasis:names:tc:SAML:2.0:ac:classes:TLSClient" />
                 <bean parent="shibboleth.SAML1AuthenticationMethod"
                    c:method="urn:ietf:rfc:2246" />
              </list>
           </property>
        </bean>
   ```      
   after bean with id="authn/Password".
   
   The position in the beans definition is required to enable `X509External` 
   as a ExtendedFlow on Password.
   
   If X509External is defined before `Password`, it will be the choosen 
   method and `Password` will be skipped.
2. edit `conf/idp.properties` and enable External flow:

   ```
       # Regular expression matching login flows to enable, e.g. IPAddress|Password
       #idp.authn.flows= Password
       idp.authn.flows= Password|X509External
   ``` 
3. edit `conf/authn/password-authn-config.xml` to allow Password 
   flow to call X509External as ExtendedFlow:
   
   ```
       <bean id="shibboleth.authn.Password.ExtendedFlows"
         class="java.lang.String" c:_0="X509External" />
   ```
   
   [snapshot of resulting login form](ExtendedFlow.png) when an 
   ExtendedFlow is enables.
   
4. define the new flow. Create a dir
   ``
   $IDP_HOME/flows/authn/X509External
   ``
   and copy two files (beans and flow) from the same location on
   the src/main/resource tree.
   
   Copy the configuration file `x509-external-authn-config.xml` to
   `IDP_HOME/conf/authn`
   
4. copy web.xml to edit-webapp (to be safer 
   at [upgrade time](https://wiki.shibboleth.net/confluence/display/IDP30/Upgrading)):
   
   > cp -v webapp/WEB-INF/web.xml edit-webapp/WEB-INF/web.xml
     
   and add, after the X509 authentication stanza:
   ```
       <!-- Servlet protected by container used for External authentication -->
       <servlet>
         <servlet-name>X509ExternalAuthHandler</servlet-name>
         <servlet-class>it.unimore.shibboleth.idp.authn.impl.X509UnimoreAuthServlet</servlet-class>
         <init-param>
               <param-name>contextConfigLocation</param-name>
               <param-value>/opt/shibboleth-idp/conf/authn/X509External.properties</param-value>
         </init-param>
         <load-on-startup>4</load-on-startup>
       </servlet>
       <servlet-mapping>
         <servlet-name>X509ExternalAuthHandler</servlet-name>
         <url-pattern>/Authn/External</url-pattern>
       </servlet-mapping>
   ```
5. to deploy, build jar with ``gradle build``,
  
       cp -v ~/unimore-x509-0.1.0.jar ./edit-webapp/WEB-INF/lib/
  
   re-create the ``war`` with:
   
       sudo JAVA_HOME=/usr/lib/jvm/latest ./bin/build.sh
   
   and reload jetty
   
## Yet to be done

1. re-enable the courtesy page (the "please insert smart-card in reader" advice);

2. username extraction and handling. This is the hardest part. If the smart card is an Italian CNS/CIE, 
   `SSL_CLIENT_S_DN_CN` contains the Codice Fiscale (Italian taxpayer number). 
    Shorter path is to define a custom Principal 
    implementing `net.shibboleth.idp.authn.principal.CloneablePrincipal`
    and leveraging the `c14n/attribute` canonicalization flow using as activating 
    condition the custom Principal.
    
    But if a single Codice Fiscale is shared by more than one account (the same 
    person can have a teaching account and an administrative account) and you 
    need to allow user to choose among usernames, I think a follow-up of the 
    authentication flow is more appropriate.
