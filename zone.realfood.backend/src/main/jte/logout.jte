@param String title
@param String staticBaseUrl
@param String userEmail
@param String csrfToken

@param gg.jte.Content content = @`
  <div id="main-wrapper">
    <div class="container">
      <div class="row gtr-200">
        <div class="col-12 col-12-medium">
          <div id="content">
            <section class="last">
              <h2>Confirm Logout</h2>
              <p>You are signed in as ${userEmail}. To end your session, confirm below.</p>
              <form method="post" action="/logout">
                <input type="hidden" name="csrfToken" value="${csrfToken}" />
                <button type="submit" class="button large icon solid fa-sign-out-alt">Log out</button>
              </form>
            </section>
          </div>
        </div>
      </div>
    </div>
  </div>
`

@template.layout(title, "no-sidebar", "logout", staticBaseUrl, userEmail, csrfToken, content)
