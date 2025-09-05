@param String title
@param String greeting
@param String name
@param String cssUrl
@param String profile
@param String userId
@param String error

@param gg.jte.Content content = @`
  <div id="banner-wrapper">
    <div id="banner" class="box container">
      <div class="row">
        <div class="col-7 col-12-medium">
          <h2>${greeting} ${name}.</h2>
          <p>Welcome to RealFood Zone.</p>
        </div>
        <div class="col-5 col-12-medium">
          <ul>
            <li><a href="/" class="button large icon solid fa-arrow-circle-right">Explore</a></li>
            <li><a href="/privacy" class="button alt large icon solid fa-question-circle">Privacy</a></li>
          </ul>
        </div>
      </div>
    </div>
  </div>

  <div id="main-wrapper">
    <div class="container">
      <div class="row gtr-200">
        <div class="col-12 col-12-medium">
          <div id="content">
            <section class="last">
              <h2>UserProfile sample</h2>
              @if(error != null && !error.isBlank())
                <p>Error reading from DynamoDB: ${error}</p>
              @elseif(profile == null || profile.isBlank())
                <p>You're not logged in. No profile associated.</p>
              @else
                <p>Profile email: ${profile}</p>
              @endif
            </section>
          </div>
        </div>
      </div>
    </div>
  </div>
`

@param String staticBase = cssUrl != null ? cssUrl.replace("/styles/style.css", "") : null

@template.layout(title, staticBase, cssUrl, profile, content)
