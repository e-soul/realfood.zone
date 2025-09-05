@param String title
@param String staticBaseUrl
@param String cssUrl
@param String userEmail
@param gg.jte.Content content

<!DOCTYPE HTML>
<html>
  <head>
  <title>${title}</title>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no" />
    @if(cssUrl != null && !cssUrl.isBlank())
      <link rel="stylesheet" href="${cssUrl}">
    @endif
    @if(staticBaseUrl != null && !staticBaseUrl.isBlank())
      <link rel="stylesheet" href="${staticBaseUrl}/assets/css/main.css" />
    @endif
  </head>
  <body class="is-preload">
    <div id="page-wrapper">
      <div id="header-wrapper">
        <header id="header" class="container">
          <div id="logo">
            <h1><a href="/">RealFood</a></h1>
            <span>beta</span>
          </div>
          <nav id="nav">
            <ul>
              <li><a href="/">Home</a></li>
              <li><a href="/privacy">Privacy</a></li>
              <li><a href="/terms">Terms</a></li>
              @if(userEmail != null && !userEmail.isBlank())
                <li><a href="/logout">Logout</a></li>
              @else
                <li><a href="/login">Login</a></li>
              @endif
            </ul>
          </nav>
        </header>
      </div>

      ${content}

      <div id="footer-wrapper">
        <footer id="footer" class="container">
          <div class="row">
            <div class="col-12">
              <div id="copyright">
                <ul class="menu">
                  <li>&copy; RealFood Zone</li><li>Design adapted from <a href="http://html5up.net">HTML5 UP</a></li>
                </ul>
              </div>
            </div>
          </div>
        </footer>
      </div>
    </div>

    @if(staticBaseUrl != null && !staticBaseUrl.isBlank())
      <script src="${staticBaseUrl}/assets/js/jquery.min.js"></script>
      <script src="${staticBaseUrl}/assets/js/jquery.dropotron.min.js"></script>
      <script src="${staticBaseUrl}/assets/js/browser.min.js"></script>
      <script src="${staticBaseUrl}/assets/js/breakpoints.min.js"></script>
      <script src="${staticBaseUrl}/assets/js/util.js"></script>
      <script src="${staticBaseUrl}/assets/js/main.js"></script>
    @endif
  </body>
</html>
