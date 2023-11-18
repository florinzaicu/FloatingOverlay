**Change Log**
* Update app to use Material 3 dynamic colors
* Add flick gesture support to overlay to move quickly to edges (issue #5)
* Fix move coordinates on rotate screen or fold (issue #6)


**Previous Changes for Major Version**
*V0.5-RC:*
* Add circular version of app logo for notification (fix scaling)
* Add collapsing functionality to overlay
* Add gestures to overlay to allow changing volume and expand overlay
* Increased size of overlay buttons
* General code improvements and clean-up of logging tags

*V0.4-RC:*
* Fixed back button press on permission check activity no closing app
* Resolved deprecation warning and other compilation warnings
* Refactored start activity for result to new contract (handler) API

*V0.3-RC:*
* Fixed notification permission check for Android 33 and below

*V0.2-RC:*
* Add basic layout for main activity
* Create activity to check for and request permissions needed by app to function
* Add foreground service that displays basic overlay on screen
* Add controls to overlay service to allow changing system volume and moving overlay
