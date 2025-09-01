@import zone.realfood.UserProfile

@param String title
@param String greeting
@param String name
@param String cssUrl
@param UserProfile profile
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
        @if(profile.getPictureUrl() != null && !profile.getPictureUrl().isBlank())
          <img alt="avatar" style="width:64px;height:64px;border-radius:50%" src="${profile.getPictureUrl()}"/>
        @endif
        <p><strong>Name:</strong> ${profile.getName()}</p>
        <p><strong>Email:</strong> ${profile.getEmail()}</p>
        <p><strong>Scopes:</strong>
          @if(profile.getScopes() == null || profile.getScopes().isEmpty())
            <em>none</em>
          @else
            @for(int i = 0; i < profile.getScopes().size(); ++i)
              ${profile.getScopes().get(i)}@if(i < profile.getScopes().size() - 1), @endif
            @endfor
          @endif
        </p>
      @endif
    </section>
  </body>
</html>
