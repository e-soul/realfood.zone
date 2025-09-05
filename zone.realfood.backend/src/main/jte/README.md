JTE templates for RealFood backend

- Use `_layout_start.jte` and `_layout_end.jte` for common header/footer based on the HTML5UP Verti template.
- Page templates: `index.jte`, `login.jte`, `privacy.jte`, `terms.jte`.
- Templates assume environment variable `STATIC_BASE_URL` is provided (via CDK) to link CSS/JS/assets hosted in the static S3 bucket. `style.css` is an optional project CSS loaded before template CSS.
