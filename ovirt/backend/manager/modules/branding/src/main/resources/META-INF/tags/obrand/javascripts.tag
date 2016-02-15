<%@ tag language="java" pageEncoding="UTF-8"%>
<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="baseTheme" value="${requestScope['brandingStyle'][0]}" />
<c:choose>
    <c:when test="${fn:containsIgnoreCase(header['User-Agent'],'MSIE 8.0')}">
        <script type="text/javascript" src="${pageContext.request.contextPath}${initParam['obrandThemePath']}${baseTheme.path}/patternfly/components/respond/dest/respond.min.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}${initParam['obrandThemePath']}${baseTheme.path}/patternfly/components/html5shiv/dist/html5shiv.min.js"></script>
    </c:when>
</c:choose>
