@import zone.realfood.IndexModel
@param IndexModel model

@param gg.jte.Content content =
  <div id="banner-wrapper">
    <div id="banner" class="box container">
      <div class="row">
        <div class="col-7 col-12-medium">
          <h2>${model.getGreeting()} ${model.getName()}.</h2>
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
              @if(model.getError() != null && !model.getError().isBlank())
                <p>Error reading from DynamoDB: ${model.getError()}</p>
              @elseif(model.getProfile() == null || model.getProfile().isBlank())
                <p>You're not logged in. No profile associated.</p>
              @else
                <p>Profile email: ${model.getProfile()}</p>
              @endif
            </section>
          </div>
        </div>
      </div>
    </div>
  </div>

@param zone.realfood.LayoutModel layout = new zone.realfood.LayoutModel(model.getTitle(), model.getCssUrl() != null ? model.getCssUrl().replace("/styles/style.css", "") : null, model.getCssUrl(), model.getProfile())

@render("layout.jte", layout, content)
