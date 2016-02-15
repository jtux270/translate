package org.ovirt.engine.core.aaa.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EnforceAuthFilter implements Filter {

    private final List<String> additionalSchemes = new ArrayList<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        for (String paramName : Collections.list(filterConfig.getInitParameterNames())) {
            if (paramName.startsWith("scheme")) {
                additionalSchemes.add(filterConfig.getInitParameter(paramName));
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse)response;

        if (FiltersHelper.isAuthenticated(req)) {
            chain.doFilter(request, response);
        } else {
            @SuppressWarnings("unchecked")
            List<String> schemes = (List<String>) req.getAttribute(FiltersHelper.Constants.REQUEST_SCHEMES_KEY);
            if (schemes == null) {
                schemes = Collections.emptyList();
            }
            Set<String> allSchemes = new HashSet<String>(schemes);
            if (additionalSchemes != null) {
                allSchemes.addAll(additionalSchemes);
            }
            for (String scheme: allSchemes) {
                res.setHeader(FiltersHelper.Constants.HEADER_WWW_AUTHENTICATE, scheme);
            }
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }

    }

    @Override
    public void destroy() {
    }

}
