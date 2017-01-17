> This project is work in progress.

# Shibboleth Idp3 X509 as External Auth

## Notes
Works with Shibboleth Identity Provider 3.3.0.

## Motivations
X509 auth requires the SSLContext to be available to 
the Auth engine. Unfortunately, if your setup includes 
jetty9.3 behind a apache2 reverse proxy, there is 
no simple way to have it, because jetty9.3 
deprecates `mod_ajp`.

The apache2 `+ExportCertData` won't work pass
X509 cert data to jetty on mod_http
 
    <Location /idp/Authn/X509>
    # tihs does not pass SSLContext via mod_http
     SSLVerifyClient require
     SSLVerifyDepth 5
     SSLOptions -StdEnvVars +ExportCertData
    </Location>

Following the Shibboleth documentation, where X509 
flow is defined as 
[from Shibboleth official documentation](https://wiki.shibboleth.net/confluence/display/IDP30/X509AuthnConfiguration)

    This flow is implemented as a special case of the External    

The proposed workaround is:

1. have apache2 turn the X509 user cert data to header 
via mod_headers:

       <Location /idp/Authn/External>
         SSLVerifyClient require
         SSLVerifyDepth 5
         SSLOptions -StdEnvVars +ExportCertData
	     RequestHeader set SSL_CLIENT_S_DN "%{SSL_CLIENT_S_DN}s"
         RequestHeader set SSL_CLIENT_I_DN "%{SSL_CLIENT_I_DN}s"
         RequestHeader set SSL_CLIENT_M_SERIAL "%{SSL_CLIENT_M_SERIAL}s"
         RequestHeader set SSL_CLIENT_CERT "%{SSL_CLIENT_CERT}s"
         RequestHeader set SSL_CLIENT_VERIFY "%{SSL_CLIENT_VERIFY}s"
       </Location>
    
2. write a specialized ExternalAuthnConfiguration to 
deal with this case.

##Enable a External auth method on idp3

1. Edit: conf/authn/general-authn.xml and move bean:

       <bean id="authn/External" parent="shibboleth.AuthenticationFlow"
         p:nonBrowserSupported="false" />
   after bean with id="authn/Password".
   
   This is required to enable it as ExtendedFlow on Password
2. edit conf/idp.properties and enable External flow:

       # Regular expression matching login flows to enable, e.g. IPAddress|Password
       #idp.authn.flows= Password
       idp.authn.flows= Password|External
    
3. edit conf/authn/password-authn-config.xml to allow Password 
flow to call External as ExtendedFlow:

       <bean id="shibboleth.authn.Password.ExtendedFlows" 
         class="java.lang.String" c:_0="External" />
         
4. copy web.xml to edit-webapps:
    
       cp -v webapp/WEB-INF/web.xml edit-webapp/WEB-INF/web.xml
     
   and add, after the X509 authentication stanza:

       <!-- Servlet protected by container used for External authentication -->
       <servlet>
         <servlet-name>X509ExternalAuthHandler</servlet-name>
         <servlet-class>it.unimore.shibboleth.idp.authn.impl.X509UnimoreAuthServlet</servlet-class>
         <load-on-startup>4</load-on-startup>
       </servlet>
       <servlet-mapping>
         <servlet-name>X509ExternalAuthHandler</servlet-name>
         <url-pattern>/Authn/External</url-pattern>
       </servlet-mapping>
