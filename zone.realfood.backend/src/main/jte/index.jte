@param String title
@param String greeting
@param String name
@param String cssUrl
@param String profile
@param String userId
@param String error

<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    @if(cssUrl != null && !cssUrl.isBlank())
      <link rel="stylesheet" href="${cssUrl}">
    @endif
    <title>${title}</title>
  </head>
  <body>
    <h3>${greeting}, ${name}!</h3>
    <section>
      <h4>UserProfile sample</h4>
      @if(error != null && !error.isBlank())
        <p>Error reading from DynamoDB: ${error}</p>
      @elseif(profile == null)
        <p>No profile found for userId '<code>${userId}</code>'.</p>
      @else
        <p>No profile found for userId '<code>${userId}</code>'.</p>
      @endif
    </section>
  </body>
</html>
