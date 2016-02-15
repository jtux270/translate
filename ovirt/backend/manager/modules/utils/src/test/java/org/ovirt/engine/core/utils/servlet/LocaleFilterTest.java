package org.ovirt.engine.core.utils.servlet;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocaleFilterTest {
    LocaleFilter testFilter;

    @Mock
    HttpServletRequest mockRequest;
    @Mock
    HttpServletResponse mockResponse;
    @Mock
    FilterChain mockChain;
    @Mock
    ServletContext mockServletContext;

    @Before
    public void setUp() throws Exception {
        when(mockRequest.getServletContext()).thenReturn(mockServletContext);
        when(mockServletContext.getContextPath()).thenReturn("");
        testFilter = new LocaleFilter();
    }

    @Test
    public void testDoFilterFromParameter() throws IOException, ServletException {
        Locale testLocale = Locale.GERMANY;
        when(mockRequest.getParameter(LocaleFilter.LOCALE)).thenReturn(testLocale.toString());
        testFilter.doFilter(mockRequest, mockResponse, mockChain);
        verify(mockChain).doFilter(mockRequest, mockResponse);
        verify(mockRequest).setAttribute(LocaleFilter.LOCALE, testLocale);
        Cookie cookie = new Cookie(LocaleFilter.LOCALE, testLocale.toString());
        cookie.setMaxAge(Integer.MAX_VALUE); //Doesn't expire.
        verify(mockResponse, times(1)).addCookie((Cookie) any());
    }

    @Test
    public void testDoFilterFromParameterWithCookie() throws IOException, ServletException {
        Locale testLocale = Locale.JAPAN;
        Cookie[] cookies = createCookies(Locale.GERMAN);
        when(mockRequest.getParameter(LocaleFilter.LOCALE)).thenReturn(testLocale.toString());
        when(mockRequest.getCookies()).thenReturn(cookies);
        testFilter.doFilter(mockRequest, mockResponse, mockChain);
        verify(mockChain).doFilter(mockRequest, mockResponse);
        verify(mockRequest).setAttribute(LocaleFilter.LOCALE, testLocale);
        verify(mockResponse, times(1)).addCookie((Cookie) any());
    }

    @Test
    public void testDoFilterFromCookie() throws IOException, ServletException {
        Locale testLocale = Locale.GERMANY;
        Cookie[] cookies = createCookies(testLocale);
        when(mockRequest.getCookies()).thenReturn(cookies);
        testFilter.doFilter(mockRequest, mockResponse, mockChain);
        verify(mockChain).doFilter(mockRequest, mockResponse);
        verify(mockRequest).setAttribute(LocaleFilter.LOCALE, testLocale);
        verify(mockResponse, times(1)).addCookie((Cookie) any());
    }

    @Test
    public void testDoFilterFromCookieNull() throws IOException, ServletException {
        when(mockRequest.getCookies()).thenReturn(null);
        testFilter.doFilter(mockRequest, mockResponse, mockChain);
        verify(mockChain).doFilter(mockRequest, mockResponse);
        //Verify that it defaulted to the US locale
        verify(mockRequest).setAttribute(LocaleFilter.LOCALE, Locale.US);
        verify(mockResponse, times(1)).addCookie((Cookie) any());
    }

    @Test
    public void testDoFilterFromCookieDifferent() throws IOException, ServletException {
        Cookie[] cookies = new Cookie[2];
        cookies[0] = new Cookie("name", "value");
        cookies[1] = new Cookie("name2", "value2");
        when(mockRequest.getCookies()).thenReturn(cookies);
        testFilter.doFilter(mockRequest, mockResponse, mockChain);
        verify(mockChain).doFilter(mockRequest, mockResponse);
        //Verify that it defaulted to the US locale
        verify(mockRequest).setAttribute(LocaleFilter.LOCALE, Locale.US);
        verify(mockResponse, times(1)).addCookie((Cookie) any());
    }

    @Test
    public void testDoFilterFromRequest() throws IOException, ServletException {
        when(mockRequest.getLocale()).thenReturn(Locale.JAPANESE);
        testFilter.doFilter(mockRequest, mockResponse, mockChain);
        verify(mockChain).doFilter(mockRequest, mockResponse);
        verify(mockResponse, times(1)).addCookie((Cookie) any());
        verify(mockRequest).setAttribute(LocaleFilter.LOCALE, Locale.JAPAN);
    }

    @Test
    public void testInit() throws ServletException {
        testFilter.init(null);
        //Should not throw an exception.
    }

    @Test
    public void testDestroy() {
        testFilter.destroy();
        //Should not throw an exception.
    }

    /*
     * Helper methods.
     */
    private Cookie[] createCookies(Locale... locales) {
        List<Cookie> cookieList = new ArrayList<Cookie>();
        for(Locale locale: locales) {
            cookieList.add(new Cookie(LocaleFilter.LOCALE, locale.toString()));
        }
        return cookieList.toArray(new Cookie[cookieList.size()]);
    }

}
