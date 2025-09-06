@param String title
@param String staticBaseUrl

@param gg.jte.Content content = @`
  <div id="main-wrapper">
    <div class="container">
      <div class="row gtr-200">
        <div class="col-12 col-12-medium">
          <div id="content">
            <article>
              <h2>Privacy Policy</h2>
              <p>Your privacy matters. This is a placeholder privacy policy page based on the Verti template. Replace with your actual policy.</p>
              <h3>Data we collect</h3>
              <p>We may store your email address after you sign in with Google to personalize your experience.</p>
              <h3>Contact</h3>
              <p>Questions? Contact support at support AT realfood.zone.</p>
            </article>
          </div>
        </div>
      </div>
    </div>
  </div>
`

@template.layout(title, "no-sidebar", "privacy", staticBaseUrl, null, content)
