<%@ tag language="java" pageEncoding="UTF-8"%>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@attribute name="user" type="br.com.caelum.brutal.model.User" required="true" %>
<%@attribute name="date" type="org.joda.time.DateTime" required="true" %>
<%@attribute name="touchText" type="java.lang.String" required="false" %>

<div class="complete-user">
	<div class="when"><fmt:message key='${touchText}'/> <tags:prettyTime time="${date}"/></div>
	<img class="user-image" src="${user.photo}?s=32"/>
	<div class="user-info">
		<a class="user-name ellipsis" href="<c:url value="/users/${user.id}/${user.sluggedName}"/>">${user.name}</a>
		<div class="user-karma">${user.karma}</div>
	</div>
</div>