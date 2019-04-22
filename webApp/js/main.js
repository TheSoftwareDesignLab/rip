(function($) {
	"use strict";
	$(function() {
		animatedScrolling();
		homeSliders();
		testimonial();
		fullscreen();
		navPosition();
		stickyHeader();
		closeMobileNav();
		logoCarousel();
		newsGallery();
		contactFormValidation();
		parallax();
		magnificAjaxPopup();
		previewPannel();
		$(".nav-list").singlePageNav({
			offset: $(".sticky-header").outerHeight(),
			currentClass: "active",
			updateHash: false,
			easing: "easeInOutExpo",
			speed: 900
		});
		$(".nav-list-item:not(.mp-nav)").singlePageNav({
			offset: $(".site-header").outerHeight(),
			currentClass: "active",
			updateHash: false,
			easing: "easeInOutExpo",
			speed: 900
		});
		$(".site-header-sticky").sticky({
			topSpacing: 0
		});
		if ($(window).width() < 768 ) {
			$(".sticky-header").sticky({
				topSpacing: 0
			});		
		}
        // portfolio hover fix for ios
        $(".portfolio-item").hover(function() {
            //On Hover - Works on ios
            $(this).addClass("active");
        }, function() {
            //Hover Off - Hover off doesn't seem to work on iOS
            $(this).removeClass("active");
        })
	});
	$(window).on("load", function() {
		portfolio();
		parallax();
	});
	$(window).on("resize", function() {
		fullscreen();
	});
	/*----------------------------------------
		Fullscreen
	------------------------------------------*/
	function fullscreen() {
		var windowHeight = $(window).height();
		var headerHeight = $(".site-header").height();
		var fitscreen = windowHeight-headerHeight;
		$(".fullscreen").css("height", windowHeight);
		$(".fitscreen").css("height", fitscreen);
	}
	/*----------------------------------------
		Nav Position
	------------------------------------------*/
	function navPosition(){
		if ( $(window).width() >= 768 ) {
			$(".sticky-header .primary-nav").css({
				top: "50px",
				transform: "translateY(0px)"
			});
			$(window).on("scroll", function() {
				if ( $(window).scrollTop() >= $(window).height() ) {
					$(".sticky-header .primary-nav").css({
						top: "50%",
						transform: "translateY(-50%)"
					});
				} else {
					$(".sticky-header .primary-nav").css({
						top: "50px",
						transform: "translateY(0px)"
					});
				}
			});
		}
	}
	function stickyHeader() {
		$(window).on("scroll", function() {
			if ( $(window).scrollTop() >= 1 ) {
				$(".site-header.fixed").addClass("is-sticky");
				$(".site-header.fixed").find(".nav-list-item.white").removeClass("white");
			} else {
				$(".site-header.fixed").removeClass("is-sticky");
				$(".site-header.fixed").find(".nav-list-item:not(.dark)").addClass("white");
			}
		});
		if ($(window).width() < 768) {
			$(".site-header.fixed").find(".nav-list-item").removeClass("white");
			$(window).on("scroll", function() {
				if ( $(window).scrollTop() >= 1 ) {
					$(".site-header.fixed").find(".nav-list-item").removeClass("white");
				} else {
					$(".site-header.fixed").find(".nav-list-item").removeClass("white");
				}
			});
		}
	}
	/*----------------------------------------
		Close opened nav in mobile
	------------------------------------------*/
	function closeMobileNav() {
		if ( $(window).width() < 768 ) {
			$(".nav-list > li > a, .nav-list-item > li > a").on("click", function(event) {
//				event.preventDefault();
				if (!$(this).next().is("ul")) {
					$('.navbar-toggle:visible').click();
				}
			});
		}
	}
	/*----------------------------------------
		Section Scrolling
	------------------------------------------*/
	function animatedScrolling() {
        $(".section-anchor, .animated-link").on('click', function(e) {
            e.preventDefault();

            var target = this.hash,
                $target = $(target),
                hHeight = $(".site-header").height();

            $('html, body').stop().animate({
                'scrollTop': $target.offset().top-hHeight
            }, 900, 'easeInOutExpo', function() {
                window.location.hash = target-hHeight;
            });
        });
	}
	/*----------------------------------------
		Home slider
	------------------------------------------*/
	function homeSliders() {
		$(".home-slider").slick({
			speed: 500,
			infinite: true,
			fade: true,
			cssEase: 'linear',
			prevArrow: '<div class="slick-arrow slick-prev"><i class="fa fa-angle-left"></i></div>',
			nextArrow: '<div class="slick-arrow slick-next"><i class="fa fa-angle-right"></i></div>',
			responsive: [
			   	{
			     	breakpoint: 800,
		     		settings: {
				       arrows: false,
				       dots: true
		     		}
			   	},
			]
		});
		$(".home-slider-h3").slick({
			speed: 800,
			infinite: true,
			fade: true,
			autoplay: true,
			cssEase: 'linear',
			prevArrow: '<div class="slick-arrow slick-prev"><i class="fa fa-angle-left"></i></div>',
			nextArrow: '<div class="slick-arrow slick-next"><i class="fa fa-angle-right"></i></div>',
			responsive: [
			   	{
			     	breakpoint: 800,
		     		settings: {
				       arrows: false,
				       dots: true
		     		}
			   	},
			]
		});
		$(".home-slider-h4").slick({
			speed: 800,
			infinite: true,
			fade: true,
			autoplay: true,
			cssEase: 'linear',
			prevArrow: '<div class="slick-arrow slick-prev"><i class="fa fa-angle-left"></i></div>',
			nextArrow: '<div class="slick-arrow slick-next"><i class="fa fa-angle-right"></i></div>',
			responsive: [
			   	{
			     	breakpoint: 800,
		     		settings: {
				       arrows: false,
				       dots: true
		     		}
			   	},
			]
		});
		$(".home-slider-h5").slick({
			speed: 800,
			infinite: true,
			fade: true,
			autoplay: true,
			cssEase: 'linear',
			prevArrow: '<div class="slick-arrow slick-prev"><i class="fa fa-angle-left"></i></div>',
			nextArrow: '<div class="slick-arrow slick-next"><i class="fa fa-angle-right"></i></div>',
			responsive: [
			   	{
			     	breakpoint: 800,
		     		settings: {
				       arrows: false,
				       dots: true
		     		}
			   	},
			]
		});
	}
	/*----------------------------------------
		Home slider
	------------------------------------------*/
	function testimonial() {
		$(".testimonial").slick({
			speed: 500,
			infinite: true,
			dots: true,
			arrows: false,
			//fade: true,
			//cssEase: 'linear'
		});
	}
	/*----------------------------------------
		Home slider
	------------------------------------------*/
	function logoCarousel() {
		$(".logo-carousel").slick({
			speed: 500,
			infinite: true,
			arrows: false,
			slidesToShow: 4,
			slidesToScroll: 1,
			autoplay: true,
			responsive: [
			   	{
			     	breakpoint: 1024,
		     		settings: {
				       slidesToShow: 3,
				       slidesToScroll: 3
		     		}
			   	},
			   	{
			     	breakpoint: 600,
				    settings: {
				    	slidesToShow: 2,
				       	slidesToScroll: 2
				    }
			   	},
			   	{
			     	breakpoint: 480,
			     	settings: {
			       		slidesToShow: 1,
			       		slidesToScroll: 1
			     	}
			   	}
			]
		});
	}
	/*----------------------------------------
		News Gallery
	------------------------------------------*/
    function newsGallery() {
        $(".news-slider-for").slick({
            slidesToShow: 1,
            slidesToScroll: 1,
            arrows: false,
            fade: true,
            asNavFor: ".news-slider-nav"
        });
        $(".news-slider-nav").slick({
        	centerPadding: 0,
            slidesToShow: 5,
            slidesToScroll: 1,
            asNavFor: ".news-slider-for",
            centerMode: true,
            focusOnSelect: true,
			prevArrow: '<div class="slick-arrow slick-prev mdl-js-button mdl-button--raised mdl-js-ripple-effect"><i class="material-icons">chevron_left</i></div>',
			nextArrow: '<div class="slick-arrow slick-next mdl-js-button mdl-button--raised mdl-js-ripple-effect"><i class="material-icons">chevron_right</i></div>',
			responsive: [
			  	{
			    	breakpoint: 1025,
				    settings: {
				      	slidesToShow: 3
				    }
			  	},
			  	{
			    	breakpoint: 480,
				    settings: {
				      	slidesToShow: 1
				    }
			  	},
			]
        });
    }
	/*----------------------------------------
		Portfolio
	------------------------------------------*/
	function portfolio() {
		var portfolio = $(".row.isotope");
		portfolio.isotope({
			itemSelector: '.row.isotope > [class^="col-"]',
			masonry: {
				columnWidth: 1
			}
		})
		$(".portfolio-filter a").on("click", function(){
		    $(".portfolio-filter a").removeClass("active");
		    $(this).addClass("active");
		   // portfolio fiter
		    var selector = $(this).attr("data-filter");
		    portfolio.isotope({
		        filter: selector
		    });
		    return false;
		});
	}
	/*----------------------------------------
		Parallax
	------------------------------------------*/
	function parallax() {
		$(".parallax1").parallax("50%", 0.5);
		$(".parallax2").parallax("50%", 0.5);
		$(".parallax3").parallax("50%", 0.5);
		$(".parallax4").parallax("50%", 0.5);
		$(".parallax5").parallax("50%", 0.5);
		$(".parallax6").parallax("50%", 0.5);
	}
	/*----------------------------------------
		Magnific Ajax Popup
	------------------------------------------*/
	function magnificAjaxPopup() {
		$(".magnific-ajax").magnificPopup({
		  	type: 'ajax'
		});
	}
	/*----------------------------------------
		contact form validation
	------------------------------------------*/
	function contactFormValidation() {
		$(".contact-form").validate({
		    rules: {
		        fullname: {
		            required: true
		        },
		        email: {
		            required: true,
		            email: true
		        },
		        message: {
		            required: true
		        }
		    },
		    messages: {
		        fullname: {
		            required: "Write your name here"
		        },
		        email: {
		            required: "No email, no support"
		        },
		        message: {
		            required: "You have to write something to send this form"
		        }
		    },
		    submitHandler: function(form) {
		        $(form).ajaxSubmit({
		            type: "POST",
		            data: $(form).serialize(),
		            url : "mail.php",
		            success: function() {
		                $(".contact-form").fadeTo( "slow", 1, function() {
		                    $(".contact-form .msg-success").slideDown();
		                });
		                $(".contact-form").resetForm();
		            },
		            error: function() {
		                $(".contact-form").fadeTo( "slow", 1, function() {
		                    $(".contact-form .msg-failed").slideDown();
		                });
		            }
		        });
		    },
		    errorPlacement: function(error, element) {
		        element.after(error);
		        error.hide().slideDown();
		    }
		});
	}
	/*=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	    Preview Pannel
	-=-=-=-=-=-=-=-=-=--=-=-=-=-=-*/
	function previewPannel() {
	    $(".switcher-trigger").on("click", function() {
	        $(".preview-wrapper").toggleClass("extend");
	        return false;
	    });
	    $(".color-options li").on("click", function(){
            $("#color-switcher").attr({
                "href":"css/colors/"+$(this).attr("data-color")+".css"
            });
            $(".color-options li").removeClass("fa fa-check");
            $(this).addClass("fa fa-check");
	        return false;
	    });
	    $(".color-options li").each(function() {
	    	$(this).css("background-color", $(this).attr("data-color-code"));
	    });
	}
	/*=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
	    Parallax Multilayer
	-=-=-=-=-=-=-=-=-=--=-=-=-=-=-*/
		
})(jQuery);