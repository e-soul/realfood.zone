@import zone.realfood.UserProfile

@param String title
@param String staticBaseUrl
@param UserProfile userProfile
@param String userEmail

@param gg.jte.Content content = @`
			<!-- Banner -->
				<div id="banner-wrapper">
					<div id="banner" class="box container">
						<div class="row">
							<div class="col-7 col-12-medium">
							@if(userProfile != null)
								<h2>Hi, ${userProfile.getName()}!</h2>
							@else
								<h2>Hi, Stranger!</h2>
							@endif
								<p>Your menu for today is ready, just look below.</p>
							</div>
							<div class="col-5 col-12-medium">
								<ul>
									<li><a href="#" class="button large icon solid fa-arrow-circle-right">Ok let's go</a></li>
									<li><a href="#" class="button alt large icon solid fa-question-circle">More info</a></li>
								</ul>
							</div>
						</div>
					</div>
				</div>

			<!-- Features -->
				<div id="features-wrapper">
					<div class="container">
						<div class="row">
							<div class="col-4 col-12-medium">

								<!-- Box -->
									<section class="box feature">
										<a href="#" class="image featured"><img src="${staticBaseUrl}/images/breakfast.png" alt="breakfast" /></a>
										<div class="inner">
											<header>
												<h2>Eggs and avocado toast</h2>
												<p>Strong and healthy</p>
											</header>
											<p>364 calories, 18g protein, 25g carbs, 22g fat, 8g fiber</p>
										</div>
									</section>

							</div>
							<div class="col-4 col-12-medium">

								<!-- Box -->
									<section class="box feature">
										<a href="#" class="image featured"><img src="${staticBaseUrl}/images/lunch.png" alt="lunch" /></a>
										<div class="inner">
											<header>
												<h2>Grilled salmon, black rice and greens</h2>
												<p>Irresistable</p>
											</header>
											<p>580 calories, 44g protein, 52g carsb, 21g fat, 4g fiber</p>
										</div>
									</section>

							</div>
							<div class="col-4 col-12-medium">

								<!-- Box -->
									<section class="box feature">
										<a href="#" class="image featured"><img src="${staticBaseUrl}/images/dinner.png" alt="dinner" /></a>
										<div class="inner">
											<header>
												<h2>Greek salad with feta and olives</h2>
												<p>You deserve it</p>
											</header>
											<p>449 calories, 12g protein, 19g carbs, 36g fat, 5g fiber</p>
										</div>
									</section>

							</div>
						</div>
					</div>
				</div>

			<!-- Main -->
				<div id="main-wrapper">
					<div class="container">
						<div class="row gtr-200">
							<div class="col-4 col-12-medium">

								<!-- Sidebar -->
									<div id="sidebar">
										<section class="widget thumbnails">
											<h3>Interesting stuff</h3>
											<div class="grid">
												<div class="row gtr-50">
													<div class="col-6"><a href="#" class="image fit"><img src="${staticBaseUrl}/images/pic04.jpg" alt="" /></a></div>
													<div class="col-6"><a href="#" class="image fit"><img src="${staticBaseUrl}/images/pic05.jpg" alt="" /></a></div>
													<div class="col-6"><a href="#" class="image fit"><img src="${staticBaseUrl}/images/pic06.jpg" alt="" /></a></div>
													<div class="col-6"><a href="#" class="image fit"><img src="${staticBaseUrl}/images/pic07.jpg" alt="" /></a></div>
												</div>
											</div>
											<a href="#" class="button icon fa-file-alt">More</a>
										</section>
									</div>

							</div>
							<div class="col-8 col-12-medium imp-medium">

								<!-- Content -->
									<div id="content">
										<section class="last">
											<h2>${userEmail}</h2>
											<p>This is <strong>Verti</strong>, a free and fully responsive HTML5 site template by <a href="http://html5up.net">HTML5 UP</a>.
											Verti is released under the <a href="http://html5up.net/license">Creative Commons Attribution license</a>, so feel free to use it for any personal or commercial project you might have going on (just don't forget to credit us for the design!)</p>
											<p>Phasellus quam turpis, feugiat sit amet ornare in, hendrerit in lectus. Praesent semper bibendum ipsum, et tristique augue fringilla eu. Vivamus id risus vel dolor auctor euismod quis eget mi. Etiam eu ante risus. Aliquam erat volutpat. Aliquam luctus mattis lectus sit amet phasellus quam turpis.</p>
											<a href="#" class="button icon solid fa-arrow-circle-right">Continue Reading</a>
										</section>
									</div>

							</div>
						</div>
					</div>
				</div>
`

@template.layout(title, "homepage", "index", staticBaseUrl, userEmail, content)
