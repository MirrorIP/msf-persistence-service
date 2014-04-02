<%@page import="org.jivesoftware.util.JiveGlobals"%>
<%@page import="org.jivesoftware.util.ParamUtils"%>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />

<%
	// initialize openfire objects
	webManager.init(request, response, session, application, out);
	
	// parse parameters
	boolean save = ParamUtils.getBooleanParameter(request, "save");

	if (save) {
		boolean allowDelete = ParamUtils.getBooleanParameter(request, "allowPublishersToDelete", false);
		JiveGlobals.setProperty("msf.persistence.allowPublishersToDelete", Boolean.toString(allowDelete));
		response.sendRedirect("persistence-settings.jsp?settingsSaved=true");
	}

	boolean isDeletionAllowed = JiveGlobals.getBooleanProperty("msf.persistence.allowPublishersToDelete", false);
%>
<html>
<head>
	<title>Persistence Settings</title>
	<meta name="pageID" content="persistence-settings"/>
</head>
<body>

<% if (ParamUtils.getBooleanParameter(request, "settingsSaved")) { %>
   
<div class="jive-success">
	<table cellpadding="0" cellspacing="0" border="0">
		<tbody>
			<tr>
				<td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
				<td class="jive-icon-label">Settings saved successfully.</td>
			</tr>
		</tbody>
	</table>
</div>
   
<% } %>

<form action="persistence-settings.jsp?save=true" method="post">
<div class="jive-contentBoxHeader">Access Control</div>
<div class="jive-contentBox">
	<table cellpadding="3" cellspacing="0" border="0" width="100%">
		<tbody>
			<tr>
				<td width="1%" align="center" nowrap><input type="checkbox" name="allowPublishersToDelete" <%=isDeletionAllowed ? "checked=\"checked\"" : "" %>></td>
				<td width="99%" align="left">Allow publishers to delete their data, even if they are not moderators.</td>
			</tr>
		</tbody>
	</table>
</div>
<input type="submit" value="Save"/>
</form>


</body>
</html>