@param String title
@param String cssUrl
@param String userEmail

@param gg.jte.Content content =
  <div id="main-wrapper">
    <div class="container">
      <div class="row gtr-200">
        <div class="col-12 col-12-medium">
          <div id="content">
            <section class="last">
              <h2>Login</h2>
              <p>
                <a class="button large icon brands fa-google" href="/auth/google">Sign in with Google</a>
              </p>
            </section>
          </div>
        </div>
      </div>
    </div>
  </div>

@param String staticBase = cssUrl != null ? cssUrl.replace("/styles/style.css", "") : null

@render("layout.jte", title, staticBase, cssUrl, userEmail, content)
