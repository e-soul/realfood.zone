---
title: Вкусна истинска храна!
---
<h2 class="text-center">А Вие какво ще хапвате днес?</h2>

<div class="container text-center">
{% for post in site.posts %}
    <div class="card mb-3" style="max-width: 640px;">
    <div class="row g-0">
        <div class="col-lg-4">
        <img src="{{ post.imagePaths[0] }}" class="img-fluid rounded">
        </div>
        <div class="col-lg-8">
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
