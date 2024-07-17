---
title: Вкусна истинска храна!
---
## А Вие какво ще хапвате днес?

<div class="d-flex flex-lg-wrap">
{% for post in site.posts %}
    <div class="card m-2" style="width: 18rem;">
        <img src="{{ post.imagePaths[0] }}" class="card-img-top">
        <div class="card-body">
            <h5 class="card-title">{{ post.title }}</h5>
{% if post.description %}
            <p class="card-text">{{ post.description }}</p>
{% endif %}
            <a href="{{ post.url }}" class="card-link">Още</a>
        </div>
    </div>
{% endfor %}
</div>

Вижте повече [за мен](/about.html).
