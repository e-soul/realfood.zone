@import zone.realfood.LoginModel
@param LoginModel model

@param gg.jte.Content content =
  <div id="main-wrapper">
    <div class="container">
      <div class="row gtr-200">
        <div class="col-12 col-12-medium">
          <div id="content">
            <section class="last">
              <h2>Login</h2>
              <p>
                <a class="button large icon brands fa-google" href="${model.getGoogleAuthPath()}">Sign in with Google</a>
              </p>
            </section>
          </div>
        </div>
      </div>
    </div>
  </div>

@param zone.realfood.LayoutModel layout = new zone.realfood.LayoutModel(model.getTitle(), model.getCssUrl() != null ? model.getCssUrl().replace("/styles/style.css", "") : null, model.getCssUrl(), null)

@render("layout.jte", layout, content)
