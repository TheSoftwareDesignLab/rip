//JQuery Twitter Feed. Coded by Tom Elliott @ www.webdevdoor.com (2013) based on https://twitter.com/javascripts/blogger.js
//Requires JSON output from authenticating script: http://www.webdevdoor.com/php/authenticating-twitter-feed-timeline-oauth/
(function($) {
    "use strict";
    $(function() {
        var displaylimit = 3;
        var twitterprofile = "PixelSpn1";
        var screenname = "PixelCoder";
        var showdirecttweets = false;
        var showretweets = true;
        var showtweetlinks = true;
        var showprofilepic = true;
        var showtweetactions = true;
        var showretweetindicator = true;

        var followaddres = "https://twitter.com/intent/follow?original_referer=&screen_name=OliaGozha&tw_p=followbutton&variant=2.0";

        var headerHTML = '';
        var loadingHTML = '';
        var loadingHTMLalt = '';

        headerHTML += '';
        loadingHTML += '<div class="loader" id="loading-container"></div>';
        loadingHTMLalt += '<div class="loader loader--dark" id="loading-container"></div>';
        // <img src="images/ajax-loader.gif" width="64" height="64"/>


        $('#twitter-feeds').html(headerHTML + loadingHTML);
        $('#twitter-feed-list').html(headerHTML + loadingHTMLalt);

        if (window.location.host) {
            getJsonCustom()
        } else {
            console.warn("Twitter is not working without PHP server");
        }

        function getJsonCustom() {
            $.getJSON('tweet.php',

            function(feeds) {
                var feedHTML = '';
                var displayCounter = 1;
                for (var i = 0; i < feeds.length; i++) {
                    var tweetscreenname = feeds[i].user.name;
                    var tweetusername = feeds[i].user.screen_name;
                    var profileimage = feeds[i].user.profile_image_url_https;
                    var status = feeds[i].text;
                    var isaretweet = false;
                    var isdirect = false;
                    var tweetid = feeds[i].id_str;

                    //If the tweet has been retweeted, get the profile pic of the tweeter
                    if (typeof feeds[i].retweeted_status != 'undefined') {
                        profileimage = feeds[i].retweeted_status.user.profile_image_url_https;
                        tweetscreenname = feeds[i].retweeted_status.user.name;
                        tweetusername = feeds[i].retweeted_status.user.screen_name;
                        tweetid = feeds[i].retweeted_status.id_str;
                        status = feeds[i].retweeted_status.text;
                        isaretweet = true;
                    };

                    //Check to see if the tweet is a direct message
                    if (feeds[i].text.substr(0, 1) == "@") {
                        isdirect = true;
                    }

                    //console.log(feeds[i]);

                    //Generate twitter feed HTML based on selected options
                    if (((showretweets == true) || ((isaretweet == false) && (showretweets == false))) && ((showdirecttweets == true) || ((showdirecttweets == false) && (isdirect == false)))) {
                        if ((feeds[i].text.length > 1) && (displayCounter <= displaylimit)) {
                            if (showtweetlinks == true) {
                                status = addlinks(status);
                            }

                            if (displayCounter == 1) {
                                feedHTML += headerHTML;
                            }

                            feedHTML += '<div class="twitter-item" id="tw' + displayCounter + '">';
                            feedHTML += '<div class="twitter-pic"><a href="https://twitter.com/' + tweetusername + '" target="_blank"><img src="' + profileimage + '" alt="' + tweetusername + '"></a></div>';

                            feedHTML += '<div class="tweet-content">';
                            feedHTML += '<span class="author-name">' + screenname + '</span>';

                            feedHTML += '<span class="author-username"><a href="https://twitter.com/' + tweetusername + '" target="_blank">@' + tweetusername + '</a></span>'

                            feedHTML += '<div class="twitter-text"><p>' + status + '</p>';
                            feedHTML += '<div class="tweet-meta">';
                            feedHTML += '<span  class="tweet-time"><a href="https://twitter.com/' + tweetusername + '/status/' + tweetid + '" target="_blank">' + relative_time(feeds[i].created_at) + '</a></span>';

                            if ((isaretweet == true) && (showretweetindicator == true)) {
                                feedHTML += '<div id="retweet-indicator"></div>';
                            }

                            if (showtweetactions == true) {
                                feedHTML += '<ul class="twitter-actions list-inline text-right"><li><a href="https://twitter.com/intent/tweet?in_reply_to=' + tweetid + '"><i class="material-icons">reply</i> Reply</a></li><li><a href="https://twitter.com/intent/retweet?tweet_id=' + tweetid + '"><i class="material-icons">cached</i> Retweet</a></li><li><a href="https://twitter.com/intent/favorite?tweet_id=' + tweetid + '"><i class="material-icons">star_border</i> Favourite</a></li></ul>';
                            }

                            feedHTML += '</div>';
                            feedHTML += '</div>';
                            feedHTML += '</div>';
                            feedHTML += '</div>';
                            displayCounter++;
                        }
                    }
                }

                $('#twitter-feeds').html(feedHTML).slick({
                    arrows: false,
                    vertical: true,
                    infinite: true,
                    autoplay: true,
                    speed: 900
                });

                $('#twitter-feed-list').html(feedHTML);

                //Add twitter action animation and rollovers
                $('.twitter-actions a').on("click", function() {
                    var url = $(this).attr('href');
                    window.open(url, 'tweet action window', 'width=580,height=500');
                    return false;
                });


            }).error(function(jqXHR, textStatus, errorThrown) {
                var error = "";
                if (jqXHR.status === 0) {
                    error = 'Connection problem. Check file path and www vs non-www in getJSON request';
                } else if (jqXHR.status == 404) {
                    error = 'Requested page not found. [404]';
                } else if (jqXHR.status == 500) {
                    error = 'Internal Server Error [500].';
                } else {
                    error = 'Uncaught Error.\n' + jqXHR.responseText;
                }
                console.warn("error: " + error);
            });

        }
        
        //Function modified from Stack Overflow
        function addlinks(data) {
            //Add link to all http:// links within tweets
            data = data.replace(/((https?|s?ftp|ssh)\:\/\/[^"\s\<\>]*[^.,;'">\:\s\<\>\)\]\!])/g, function(url) {
                return '<a href="' + url + '"  target="_blank">' + url + '</a>';
            });

            //Add link to @usernames used within tweets
            data = data.replace(/\B@([_a-z0-9]+)/ig, function(reply) {
                return '<a href="http://twitter.com/' + reply.substring(1) + '" style="font-weight:lighter;" target="_blank">' + reply.charAt(0) + reply.substring(1) + '</a>';
            });
            //Add link to #hastags used within tweets
            data = data.replace(/\B#([_a-z0-9]+)/ig, function(reply) {
                return '<a href="https://twitter.com/search?q=' + reply.substring(1) + '" style="font-weight:lighter;" target="_blank">' + reply.charAt(0) + reply.substring(1) + '</a>';
            });
            return data;
        }


        function relative_time(time_value) {
            var values = time_value.split(" ");
            time_value = values[1] + " " + values[2] + ", " + values[5] + " " + values[3];
            var parsed_date = Date.parse(time_value);
            var relative_to = (arguments.length > 1) ? arguments[1] : new Date();
            var delta = parseInt((relative_to.getTime() - parsed_date) / 1000);
            var shortdate = time_value.substr(4, 2) + " " + time_value.substr(0, 3);
            delta = delta + (relative_to.getTimezoneOffset() * 60);

            if (delta < 60) {
                return '1m';
            } else if (delta < 120) {
                return '1m';
            } else if (delta < (60 * 60)) {
                return (parseInt(delta / 60)).toString() + 'm';
            } else if (delta < (120 * 60)) {
                return '1h';
            } else if (delta < (24 * 60 * 60)) {
                return (parseInt(delta / 3600)).toString() + 'h';
            } else if (delta < (48 * 60 * 60)) {
                //return '1 day';
                return shortdate;
            } else {
                return shortdate;
            }
        }
    });
})(jQuery);