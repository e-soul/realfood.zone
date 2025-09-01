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
    <section>
      <h4>UserProfile sample</h4>
      @if(model.getError() != null && !model.getError().isBlank())
        <p>Error reading from DynamoDB: ${model.getError()}</p>
      @elseif(model.getProfile() == null || model.getProfile().isBlank())
        <p>No profile found for userId '<code>${model.getUserId()}</code>'.</p>
      @else
        <p>profile email ${model.getProfile()}</p>
      @endif
    </section>
  </body>
</html>
