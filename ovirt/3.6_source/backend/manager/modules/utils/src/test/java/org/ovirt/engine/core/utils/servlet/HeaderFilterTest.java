package org.ovirt.engine.core.utils.servlet;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HeaderFilterTest {
    @Mock
    HttpServletRequest mockRequest;
    @Mock
    HttpServletResponse mockResponse;
    @Mock
    FilterChain mockChain;
    @Mock
    FilterConfig mockConfig;

    HeaderFilter testFilter;

    @Before
    public void setUp() throws Exception {
        when(mockConfig.getInitParameterNames()).thenReturn(Collections.enumeration(new ArrayList<String>()));
        testFilter = new HeaderFilter();
    }

    @Test
    public void testFilter() throws Exception {
        testFilter.init(mockConfig);
        testFilter.doFilter(mockRequest, mockResponse, mockChain);
        verify(mockResponse).addHeader("X-FRAME-OPTIONS", "SAMEORIGIN");
        verify(mockResponse).addHeader("X-CONTENT-TYPE-OPTIONS", "NOSNIFF");
        verify(mockResponse).addHeader("X-XSS-PROTECTION", "1; MODE=BLOCK");
    }

    @Test
    public void testFilterOverride() throws Exception {
        List<String> initParams = new ArrayList<String>();
        initParams.add("X-FRAME-OPTIONS");
        when(mockConfig.getInitParameter("X-FRAME-OPTIONS")).thenReturn("DENY");
        when(mockConfig.getInitParameterNames()).thenReturn(Collections.enumeration(initParams));
        testFilter.init(mockConfig);
        testFilter.doFilter(mockRequest, mockResponse, mockChain);
        verify(mockResponse).addHeader("X-FRAME-OPTIONS", "DENY");
        verify(mockResponse).addHeader("X-CONTENT-TYPE-OPTIONS", "NOSNIFF");
        verify(mockResponse).addHeader("X-XSS-PROTECTION", "1; MODE=BLOCK");
    }

    @Test
    public void testFilterOverrideDifferentCase() throws Exception {
        List<String> initParams = new ArrayList<String>();
        initParams.add("X-FRAME-options");
        when(mockConfig.getInitParameter("X-FRAME-options")).thenReturn("DENY");
        when(mockConfig.getInitParameterNames()).thenReturn(Collections.enumeration(initParams));
        testFilter.init(mockConfig);
        testFilter.doFilter(mockRequest, mockResponse, mockChain);
        verify(mockResponse).addHeader("X-FRAME-OPTIONS", "DENY");
        verify(mockResponse).addHeader("X-CONTENT-TYPE-OPTIONS", "NOSNIFF");
        verify(mockResponse).addHeader("X-XSS-PROTECTION", "1; MODE=BLOCK");
    }

    @Test
    public void testFilterAddNew() throws Exception {
        List<String> initParams = new ArrayList<String>();
        initParams.add("X-FRAME-TEST");
        when(mockConfig.getInitParameter("X-FRAME-TEST")).thenReturn("TEST");
        when(mockConfig.getInitParameterNames()).thenReturn(Collections.enumeration(initParams));
        testFilter.init(mockConfig);
        testFilter.doFilter(mockRequest, mockResponse, mockChain);
        verify(mockResponse).addHeader("X-FRAME-OPTIONS", "SAMEORIGIN");
        verify(mockResponse).addHeader("X-CONTENT-TYPE-OPTIONS", "NOSNIFF");
        verify(mockResponse).addHeader("X-XSS-PROTECTION", "1; MODE=BLOCK");
        verify(mockResponse).addHeader("X-FRAME-TEST", "TEST");
    }
}
