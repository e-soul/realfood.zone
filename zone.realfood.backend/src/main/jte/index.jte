@import zone.realfood.IndexModel
@param IndexModel model

<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    @if(model.getCssUrl() != null && !model.getCssUrl().isBlank())
      <link rel="stylesheet" href="${model.getCssUrl()}">
    @endif
    <title>${model.getTitle()}</title>
  </head>
  <body>
    <h3>${model.getGreeting()}, ${model.getName()}!</h3>
    <nav>
      @if(model.getProfile() != null && !model.getProfile().isBlank())
        <span>Signed in as ${model.getProfile()}</span>
        <a href="/logout" style="margin-left:1rem;">Logout</a>
      @else
        <a href="/login">Login</a>
      @endif
    </nav>
    <section>
      <h4>UserProfile sample</h4>
      @if(model.getError() != null && !model.getError().isBlank())
        <p>Error reading from DynamoDB: ${model.getError()}</p>
      @elseif(model.getProfile() == null || model.getProfile().isBlank())
        <p>You're not logged in. No profile associated.</p>
      @else
        <p>Profile email: ${model.getProfile()}</p>
      @endif
    </section>
  </body>
</html>
