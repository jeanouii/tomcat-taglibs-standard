<%@ taglib prefix="c" uri="http://java.sun.com/jstl/ea/core" %>

<html>
<head>
  <title>JSTL: I/O Support -- Context-relative URL example</title>
</head>
<body bgcolor="#FFFFFF">
Assuming you have the "examples" webapp installed, here's a file from it...

<blockquote>
 <pre>
  <c:import url="$_contextUrl" context="$_contextName"/>
 </pre>
</blockquote>

</body>
</html>
