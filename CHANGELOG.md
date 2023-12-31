**Change Log**

Initial stable release of the application that contains several features:
* Activity to show justification and prompt user for required permissions
* Add Foreground service that shows a floating overlay with following capability:
  * Collapse and expand overlay UI
  * Quick gestures to decrease volume (single tap), increase volume (double tap), and expand overlay (hold)
  * Flick gesture to move overlay to the corners of screen (with animation)
  * Drag icon to allow moving overlay on screen (available in expanded mode)
* Basic layout for main activity with preferences that allow setting overlay:
  * Scale factor
  * Collapse timeout timer value
  * Transparency level of overlay
* Material 3 dynamic colour support
* Dynamic color support for icon
* Show persistent notification with close action when overlay is visible


----


**Major Release Candidate Change Log (included in release)**

*V0.9-RC:*
* Add vertical flicking support for overlay
* Add M3 colour support to overlay
* Finalize release pipeline

*V0.8-RC:*
* Update name of activity package
* Add preference storage and support for configuration attributes
* Add UI elements that allow setting custom scale, collapse timeout and transparency of overlay
* General UI improvements

*V0.7-RC:*
* Add animation to flick of overlay
* Fixed deprecation warning on build
* Published signed version of RC APK

*V0.6-RC:*
* Update app to use Material 3 dynamic colors
* Add flick gesture support to overlay to move quickly to edges (issue #5)
* Fix move coordinates on rotate screen or fold (issue #6)

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
