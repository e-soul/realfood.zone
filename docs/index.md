---
title: Вкусна истинска храна!
---
## А Вие какво ще хапвате днес?

<div class="container-fluid">
{% for post in site.posts %}
    <div class="card" style="width: 18rem;">
        <img src="{{ post.imagePaths[0] }}" class="card-img-top">
        <div class="card-body">
            <h5 class="card-title">{{ post.title }}</h5>
            <p class="card-text">Some quick example text to build on the card title and make up the bulk of the card's content.</p>
            <a href="{{ post.url }}" class="card-link">Още</a>
        </div>
    </div>
{% endfor %}
</div>



Рецепти
{% for post in site.posts %}
- [{{ post.title }}]({{ post.url }})
{% endfor %}

Вижте повече [за мен](/about.html).