@import zone.realfood.LoginModel
@param LoginModel model

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
    <h3>Login</h3>
    <p>
      <a href="${model.getGoogleAuthPath()}">Sign in with Google</a>
    </p>
  </body>
</html>
