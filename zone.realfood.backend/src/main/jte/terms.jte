@import zone.realfood.LayoutModel
@param LayoutModel layout

@param gg.jte.Content content =
  <div id="main-wrapper">
    <div class="container">
      <div class="row gtr-200">
        <div class="col-12 col-12-medium">
          <div id="content">
            <article>
              <h2>Terms of Service</h2>
              <p>These are sample terms based on the Verti template page structure. Replace with your real terms.</p>
              <h3>Use of Service</h3>
              <p>By accessing this site you agree to comply with these terms.</p>
              <h3>Accounts</h3>
              <p>Logging in via Google creates a user profile associated with your email.</p>
            </article>
          </div>
        </div>
      </div>
    </div>
  </div>

@render("layout.jte", layout, content)
