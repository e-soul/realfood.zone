---
title: Вкусна истинска храна!
---
<div class="text-center">А Вие какво ще хапвате днес?</div>

<div class="container">
{% for post in site.posts %}
    <div class="card mb-3" style="max-width: 540px;">
    <div class="row g-0">
        <div class="col-md-4">
        <img src="{{ post.imagePaths[0] }}" class="img-fluid rounded">
        </div>
        <div class="col-md-8">
        <div class="card-body">
            <h5 class="card-title">{{ post.title }}</h5>
{% if post.description %}
            <p class="card-text">{{ post.description }}</p>
{% endif %}
            <a href="{{ post.url }}" class="card-link">Още</a>
        </div>
        </div>
    </div>
    </div>
{% endfor %}
</div>

Вижте повече [за мен](/about.html).
