<%@ page contentType="text/html; charset=UTF-8" %>
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <title>COMP4321 Lab 4 Output</title>
  </head>
  <body>
    <h1>Output</h1>
    <%
      String q = request.getParameter("q");
      if (q == null) q = "";
      String[] parts = q.trim().isEmpty() ? new String[0] : q.trim().split("\\\\s+");
      for (String w : parts) {
    %>
      <div><%= w %></div>
    <%
      }
      if (parts.length == 0) {
    %>
      <div>(no input)</div>
    <%
      }
    %>
  </body>
</html>

