package eventswipe.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Contains all event data.
 *
 * @author Matt Wildman http://bitbucket.com/mattwildman
 */
public class Event {

    /**
     * Default constructor for an event.
     */
    public Event() {
        this.unsavedList = new ArrayList<>();
        this.sessions = new ArrayList<>();
    }

    /**
     * Constructs an event with a title, start date and unique identifier.
     *
     * @param title The event title.
     * @param date  A String representing the start date and time of the event.
     * @param id    The unique identifier of the event in the booking system.
     */
    public Event(String title, String date, String id) {
        this.title = title;
        this.startDateString = date;
        this.id = id;
        this.unsavedList = new ArrayList<>();
        this.sessions = new ArrayList<>();
    }

    /**
     * @return The start date/time of the event as a readable String.
     */
    public String getStartDateString() {
        return startDateString;
    }

    /**
     * Set the start date/time of the event in a readable format.
     *
     * @param startDate The start date/time of the event in a readable format.
     */
    public void setStartDateString(String startDate) {
        this.startDateString = startDate;
    }

    /**
     * @return The event title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the event title.
     *
     * @param title The title of the event
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return The event's unique identifier in the booking system
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the event.
     *
     * @param id The event's unique identifier from the booking system
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return The event venue
     */
    public String getVenue() {
        return venue;
    }

    /**
     * Sets the event venue.
     *
     * @param venue The event venue
     */
    public void setVenue(String venue) {
        this.venue = venue;
    }

    /**
     * @return The event's start date/time
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Sets the event's start date/time.
     *
     * @param startDate The start date/time of the event
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * @return The earliest registration date/time for the event
     */
    public Date getRegStart() {
        return regStart;
    }

    /**
     * Sets the earliest registration date/time for the event.
     *
     * @param regStart The registration date/time
     */
    public void setRegStart(Date regStart) {
        this.regStart = regStart;
    }
    
    /**
     * @return A List of Bookings for the event
     * @see Booking
     */
    public List<Booking> getBookingList() {
        return bookingList;
    }

    /**
     * Sets the booking list for the event.
     *
     * @param bookingList A List of Bookings
     * @see Booking
     */
    public void setBookingList(List<Booking> bookingList) {
        this.bookingList = bookingList;
    }

    /**
     * @return A List of Students on the waiting list for the event
     * @see Student
     */
    public List<Student> getWaitingList() {
        return waitingList;
    }

    /**
     * Sets the waiting list for the event.
     *
     * @param waitingList A List of Students
     * @see Student
     */
    public void setWaitingList(List<Student> waitingList) {
        this.waitingList = waitingList;
    }

    /**
     * @return A List of student numbers which could not be recorded by the booking system
     */
    public List<String> getUnsavedList() {
        return unsavedList;
    }

    /**
     * Sets the List of student numbers which could not be recorded by the booking system.
     *
     * @param unsavedList A List of student number Strings
     */
    public void setUnsavedList(List<String> unsavedList) {
        this.unsavedList = unsavedList;
    }

    /**
     * @return The booking limit for the event
     */
    public int getBookingLimit() {
        return bookingLimit;
    }

    /**
     * Sets the booking limit for the event.
     *
     * @param bookingLimit The booking limit
     */
    public void setBookingLimit(int bookingLimit) {
        this.bookingLimit = bookingLimit;
    }

    /**
     * @return The number of people who have attended the event
     */
    public int getAttendeeCount() {
        return attendeeCount;
    }

    /**
     * Sets the number of people who have attended the event.
     *
     * @param attendeeCount The number attendees
     */
    public void setAttendeeCount(int attendeeCount) {
        this.attendeeCount = attendeeCount;
    }
    
    /**
     * @return The number of unspecified users booked onto the event
     */
    public int getUnspecifiedCount() {
        return unspecifiedCount;
    }

    /**
     * Sets the number of unspecified users booked onto the event
     *
     * @param unspecifiedCount The number bookings
     */
    public void setUnspecifiedCount(int unspecifiedCount) {
        this.unspecifiedCount = unspecifiedCount;
    }
    
    /**
     * @return The number of people who have booked onto the event
     */
    public int getBookingCount() {
        return bookingCount;
    }

    /**
     * Sets the number of people who have booked onto the event.
     *
     * @param bookingCount The number bookings
     */
    public void setBookingCount(int bookingCount) {
        this.bookingCount = bookingCount;
    }

    /**
     * @return True/false depending on whether the event has unlimited spaces or not
     */
    public boolean isUnlimited() {
        return unlimited;
    }

    /**
     * Sets whether or not the event has unlimited spaces.
     *
     * @param unlimited True/false
     */
    public void setUnlimited(boolean unlimited) {
        this.unlimited = unlimited;
    }

    /**
     * @return True/false depending on whether the event has a booking list
     */
    public boolean isDropIn() {
        return dropIn;
    }

    /**
     * Sets whether or not the event uses a booking list.
     *
     * @param dropIn True if the event doesn't use a booking list, false if it does
     */
    public void setDropIn(boolean dropIn) {
        this.dropIn = dropIn;
    }
    
    /**
     * @return the event timeslot sessions
     * @see Session
     */
    public List<Session> getSessions() {
        return sessions;
    }

    /**
     * @param sessions the event timeslot sessions to set
     * @See session
     */
    public void setSessions(List<Session> sessions) {
        this.sessions = sessions;
    }

    private String startDateString;
    private String title;
    private String id;
    private String venue;

    private Date startDate;
    private Date regStart;

    private List<Student> waitingList;
    private List<String> unsavedList;
    private List<Booking> bookingList;

    private int bookingLimit;
    private int attendeeCount;
    private int bookingCount;
    private int unspecifiedCount;

    private boolean unlimited = false;
    private boolean dropIn = false;
    
    private List<Session> sessions;
    
}
