# EventSwipe

*Current version: Falcon*

EventSwipe is a Java application for recording attendance at events.

It can be used as a standalone desktop application or connected to the careers CRM system, [CareerHub](http://careerhub.info/).

It works by reading some kind of ID string (a student number, for example), indicating the booking status of that person and either updating CareerHub or recording their details locally to be processed later.

The ID can be entered from a keyboard or any other external input device, such as an RFID reader.

## Updates

### Falcon

*Updated July 2016*

* Entry slot time awareness (warnings for early/late attendees)
* Custom settings screen for use with any installation of CareerHub
* Improved performance through use of CareerHub Integrations API
* Improved performance through better API token management
* A simple counting mode for when recording the IDs isn't important

### Eagle

*Updated July 2015*

* Attendance recording runs as a background task to increase performance
* Improved error messages
* Maximum entry slots increased from three to five

### Dunlin

*Updated November 2014*

* Option to mark all unspecified student as absent when you have finished taking attendance
* Detects early registration with the option to permit entry and record attendance later
* Refresh attendees button (or pressing f5) updates display without needing to scan a card
* Keyboard shortcut (Ctrl+t) toggles online mode (Ctrl+r now toggles booking mode)
* Network requests won't timeout

### Condor

*Updated June 2014*

* EventSwipe can now search and update the careers CRM system CareerHub remotely (with an internet connection). New functionality includes:
	* Loading event details (title, start date/time, booking list and waiting list)
	* Listing forthcoming events
	* Checking the status of a booking
	* Marking attendance in CareerHub directly
	* Booking in non-booked students directly
	* Searching for students by name
	* Displaying attendance for the whole event, not just the attendance recorded by the machine running EventSwipe
* All the previous functionality of Buzzard remains in an 'offline mode'
* EventSwipe will automatically switch to offline mode if there are any errors connecting to the internet or CareerHub

### Buzzard

*Updated January 2014*

* EventSwipe automatically handles multiple text file encodings (ANSI, UTF-8, Unicode big endian and Unicode little endian)
* Automatic logging of each session to /My Documents/EventSwipeLogs/ (in case of application crashing or not saving attendance data)
* Yes/no dialogue for non-booked attendees with Y/N keyboard shortcuts to allow for quick recording of non-booked attendees
* Stronger visual cue when ID has been entered (booking status colour flashes)
* Keyboard shortcut (Ctrl+t) for booking mode toggling
* Keyboard shortcut (Ctrl+s) for saving
* Exit confirmation dialogue if there are unsaved records

## Installation

### Requirements

* Any device capable of running a Java application.
* A way of distinguishing event attendees with unique and easily accessible IDs. For example, student numbers which can be read from student cards via a magnetic strip or RFID chip.

### Instructions

* [Download EventSwipe.jar and lib.zip](https://bitbucket.org/mattwildman/eventswipe/downloads)
* Extract the contents of lib.zip and save in the same folder as EventSwipe.jar
* Open EventSwipe.jar and got to file > settings and enter your installation specific settings

## Usage

### Preparing the booking lists

If the event didn't require students to book in advance then ignore this section.

EventSwipe can read ID strings from a .txt file separated by line breaks. Make sure your booking lists are saved in this format and accessible from the device running EventSwipe. 

You can upload up to three booking list files (for different timeslots within the same event) and a waiting list.

###Running the application

When you start up EventSwipe, you'll be asked to log in using your CareerHub details, or to start offline mode.

In online mode you will need to enter the CareerHub IDs of all your entry slots (or choose the slots by listing forthcoming events) and then select whether or not you want to load waiting lists.

In offline mode you will be asked to enter some details about the event. You need to enter a title and indicate whether or not you want to use a booking list and a waiting list. You can then upload all the necessary lists of IDs.

Hit ok to start recording attendance. Depending on the event settings and the attendee's booking status, when you enter an ID EventSwipe will display the following messages:

#### 'Success' messages

* __Booked__ - ID is on the booking list and entry has been recorded. EventSwipe will also display what entry slot the attendee has been booked for (if applicable)
* __Recorded__ - ID has been recorded (displayed when a booking list is not used) 

#### Other messages

* __Already recorded__ - ID has already been entered and recorded
* __Not booked__ - ID is not on the booking list and is not recorded in the attendees list. EventSwipe will also open a dialogue box asking you whether or not to let the attendee into the event. Selecting 'Yes' records the student number.
* __Waiting list__ - ID is on the waiting list. EventSwipe will also open a dialogue box asking you whether or not to let the attendee into the event. Selecting 'Yes' records the student number and takes the student off the waiting list, 'No' keeps the student on the waiting list.
* __EVENT FULL__ - ID could not be booked onto the event because there are no more places (online mode only).

You can also record an ID which was not on the booking list by switching EventSwipe to 'record all mode' with the button in the top right. You can then enter a non-booked ID and record it. This mode is useful if you want to let in a batch of non-booked attendees in one go and can be used as a fall back to guarantee you record entry to your event, even if something goes wrong with your booking list.

### Saving the attendance list

#### Offline mode

When you have finished recording entry to your event, hit 'finish', choose a file name and location and EventSwipe will save the list of recorded IDs (along with the entry slot they were booked for, if applicable) as a text file separated by line breaks.

#### Online mode

Hit 'Finish' and you'll be taken to the finish screen where you can choose whether or not to mark and notify the absentees. 

If something went wrong during your attendance recording or you let in early arrivals then EventSwipe might have saved some of the attendance records offline. In this case you will get a prompt when you try to close EventSwipe indicating that there are unsaved records. Follow the offline mode saving instructions in this scenario.

## Future features

### Goshawk

*Expected release: January 2018*

Features

* Booking list auto-refreshing
* Batch processing to make EventSwipe faster
* Improved logging

### Hen Harrier

*Expected release: June 2018*

Features

* Better UX feedback (loading bars etc.)
* Ability to handle multiple booking file types (like .csv)
* Duplicate slot booking report