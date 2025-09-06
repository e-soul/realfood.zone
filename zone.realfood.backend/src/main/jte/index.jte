@param String title
@param String staticBaseUrl
@param String userEmail

@param gg.jte.Content content = @`
				<div id="banner-wrapper">
        INDEX ${userEmail}
        </div>
`

@template.layout(title, "homepage", "index", staticBaseUrl, userEmail, content)
