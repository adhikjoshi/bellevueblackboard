# DISCLAIMER #
> This application is in no way shape or form supported by Bellevue University, this application was developed by me, a student, because I wanted there to be a way to access the discussion board via Android. At no time should support related questions be sent to Bellevue University, instead contact me directly or submit an issue through Google Code's Issue Tracker. As for anything in BlackBoards EULA that this **might** violate I am exempt from because I have never accepted a EULA from BlackBoard, like wise Bellevue University cannot be held responsible for any possible violation since they have had no hand in the development of this software. By using this application you acknowledge the above statements and understand that while this application allows you to access Bellevue University's online discussion boards that they are not responsible for this application!

# Introduction #
> This application is designed to provide a native Android interface to Bellevue University's Online Discussion board. Now you can take it wherever you go :)

## Latest ##
> BeYouPosts enters has entered its Second BETA on 4/26/11, please download the APK file from the downloads section and install on your device. Please report any issues you find here: [Issue Reporter](http://code.google.com/p/bellevueblackboard/issues/list)

## What about Licensing Fees?! ##

> I have found NO reason that I need to pay a licensing fee to Blackboard since I am not using any proprietary tools/services. The following arguments are based off of the assumption that by the university paying for a blackboard license this allows 1.) the university to install the blackboard software and 2.) the students to access the blackboard system via the web server.

### Safe Assumptions ###

> I believe the following assumptions are quite safe to make.

  * It would be foolish to think that Blackboard only allows students to access the software with certain browsers.

  * It would also be silly to think that browsers like IE,Firefox,Chrome pay blackboard a fee for blackboard to allow the use of their software on that browser.

  * It would be crazy to think that Blackboard only allows web access to the software on certain Operating Systems? Do you think people would tolerate it if you HAD to use Internet Explorer ONLY! or how about ONLY from a windows based PC? of course not, because some prefer OSX/Linux.

### Conclusion of Assumptions ###

> So from the above I feel it would be safe to say that a user should have the right to access Blackboard from a Mobile Device's web browser, since the student's right shouldn't hinge on what browser they use nor their choice of Operating System. As it turns out a student is more than capable of browsing the blackboard site from the Browser application included by default with all android devices.

### Here are FACTS about my application ###

  * Data is retrieved from the Blackboard system via Apache's HttpClient library, which is responsible for managing http/https requests and things like cookies. This ability is required for any web browser to do the same!

  * The resulting HTML pages are parsed to extract relevant information via HtmlParser (http://htmlparser.sourceforge.net/) This also is a requirement of web browsers (the ability to parse the page)!

  * The relevant information is then rendered onto the screen, which of course Web Browsers also do.

  * My application uses NO propriety software/tools/APIs from blackboard, much like a web browser doesn't.

### So My Application is a Web Browser?! ###

> Yes and No... at the core it acts much like a browser as noted above,   Where mine deviates is that it does NOT obey the HTML/CSS code as to HOW the page is rendered on the screen. My application goes about rendering it in a format that is familiar to Android users. So one could consider my application a highly-specific web browser with a customized way of rendering the pages.

> Because my application only allows you to 'browse' blackboard-specific web pages it's not a full web browser like IE/FireFox/Chrome are. So in the sense of being a generic multipurpose application, no it is not a web browser.

### What about security? After all it's not 'official' ###

> First let's discuss the "it's not official" part.

> Let me ask you this... Is Internet Explorer/Firefox/Chrome 'official' Bellevue University applications? I think not. Did they need to ask Bellevue University if they could allow students to navigate blackboard? Hardly... So why should my application have to be deemed 'official' or need to ask permission?

> Now let's address security.

> Perhaps the greatest concern is the username and password you use to initially log in, how well do you trust that browsers like IE, FireFox, and Chrome don't send your credentials to 3rd parties? No this isn't a conspiracy theory I'm saying that even though you've never SEEN the code to some of these browsers you trust that they do not. How much more should you trust an application that allows you FULL access to it's Source Code? Browse the code and you will see that the credential information is used for only 1 thing, logging you in through the HttpClient.The application does provide you the option to to store your credentials on the phone, and use at a later date automatically, but that is a personal choice.

> There is also no fear of the password being intercepted over the air for the same reason there is no fear of it on a computer, HTTPS! all traffic between the phone and blackboard is encrypted with 1024-bit RSA just like it is on the computer. So even here there is no concern!


---

## Main Features ##
> BeYouPosts offers many great features that will ease the use of the Discussion Board for Bellevue University students.

### Simple Navigation ###
> Navigation of Courses/Forums/Threads/Messages are Android native and very simple

### Making Posts ###
> BeYouPosts allows students to create their own threads and reply to other students, the Create Message screen is very simple to use and supports Bold Italic and Underline, also students can attach files from their phone

### Login Simplicity ###
> BeYouPosts supports an Auto-login system, while this is NOT required the Thread Watching ability will be unavailable if it is not set up. Auto-Login can be setup in the Settings menu.

### Network Rules ###
> BeYouPosts allows users to dictate when BeYouPosts can access the internet, this is mainly because BeYouPosts can at times require large amounts of data (depends on the course and how active the discussion board is.

### Network Optimizing ###
> Because BeYouPosts can require large amounts of data steps have been taken to reduce the amount of internet access required. BeYouPosts has the option of storing downloaded messages to the SDCARD (option is unavailable without an SDCARD). Subsequent requests for this message will be pulled from the database instead of the internet.

### Thread Notification ###
> BeYouPosts uniquely allows a student to set specific threads to be 'watched', there is now a button to set watch/unwatch. Every 15 minutes (provided BeYouPosts is allowed to access the internet) BeYouPosts will check for any new posts to the selected threads and alert you through a standard Android Notification. (Clicking on this notification will take you to the Thread that was updated). There is a known issue with multiple notifications referring you to the same thread (see issues list).


## Screen Shots ##
All available screenshots can be found here: [BeYouPosts Pictures](http://s1181.photobucket.com/albums/x440/BeYouPosts/?start=all)

| ![http://i1181.photobucket.com/albums/x440/BeYouPosts/MainScreen.png](http://i1181.photobucket.com/albums/x440/BeYouPosts/MainScreen.png) | | ![http://i1181.photobucket.com/albums/x440/BeYouPosts/Classes.png](http://i1181.photobucket.com/albums/x440/BeYouPosts/Classes.png) | ![http://i1181.photobucket.com/albums/x440/BeYouPosts/Forums.png](http://i1181.photobucket.com/albums/x440/BeYouPosts/Forums.png) | |
|:------------------------------------------------------------------------------------------------------------------------------------------|:|:------------------------------------------------------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------|:|

| ![http://i1181.photobucket.com/albums/x440/BeYouPosts/Threads.png](http://i1181.photobucket.com/albums/x440/BeYouPosts/Threads.png) | ![http://i1181.photobucket.com/albums/x440/BeYouPosts/NewThread_Reply.png](http://i1181.photobucket.com/albums/x440/BeYouPosts/NewThread_Reply.png) | | | ![http://i1181.photobucket.com/albums/x440/BeYouPosts/LoadingMsgs.png](http://i1181.photobucket.com/albums/x440/BeYouPosts/LoadingMsgs.png) |
|:------------------------------------------------------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------|:|:|:--------------------------------------------------------------------------------------------------------------------------------------------|