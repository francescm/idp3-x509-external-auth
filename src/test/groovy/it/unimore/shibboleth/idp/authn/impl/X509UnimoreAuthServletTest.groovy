package it.unimore.shibboleth.idp.authn.impl

/**
 * Created by francesco on 18/01/17.
 */

//import it.unimore.shibboleth.idp.authn.impl.X509UnimoreAuthServlet

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.mockito.Mockito.when

import java.security.Principal

import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

import org.junit.Before

import org.mockito.Mockito

//import net.shibboleth.idp.authn.context.AuthenticationContext
//import net.shibboleth.idp.authn.context.RequestedPrincipalContext
import net.shibboleth.idp.authn.ExternalAuthentication

import javax.servlet.ServletConfig
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RunWith(PowerMockRunner.class)
@PrepareForTest(ExternalAuthentication.class)
class X509UnimoreAuthServletTest {

    X509UnimoreAuthServlet servlet
    String KEY = "JUST_A_KEY" //key of EXTERNAL Auth

    @Before
    void setUp() {
        ServletConfig servletConfig = Mockito.mock(ServletConfig.class)
        when(servletConfig.getInitParameter("contextConfigLocation")).thenReturn("src/main/resources/config.slurp")
        servlet = new X509UnimoreAuthServlet()
        servlet.init(servletConfig)
    }


    @Test
    void testPlainService()  {
        List<String> headerNames = new ArrayList<>()
        headerNames.add("SSL_CLIENT_S_DN")
        Enumeration<String> headerNamesEnumerator = Collections.enumeration(headerNames)
        List<String> headerValues = new ArrayList<>()
        headerValues.add("MLVFNC69H12B819Z/7430035000001454.Caud0cp/FVmUXl/uO8quWcFGzOQ=")
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class)
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class)
        PowerMockito.mockStatic(ExternalAuthentication.class)

        when(request.getHeaderNames()).thenReturn(headerNamesEnumerator)
        when(request.getHeader(headerNames.first())).thenReturn(headerValues.first())
        when(ExternalAuthentication.startExternalAuthentication(request)).thenReturn(KEY)
        servlet.service(request, response)

    }


}
