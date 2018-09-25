package eventswipe.APIs;

import eventswipe.models.*;
import java.io.IOException;
import java.util.List;

/**
 * Abstract class containing the methods for interacting with an event booking system.
 *
 * @author Matt Wildman http://bitbucket.com/mattwildman
 */
public abstract class BookingSystemAPI {

    /**
     * Default constructor for the BookingSystemAPI
     */
    public BookingSystemAPI() {}

    /**
     * Logs in to the booking system.
     *
     * @param username Booking system username
     * @param password Booking system password
     * @return         True/false depending on log in success
     * @throws IOException
     */
    public abstract boolean logIn(String username, char[] password) throws IOException;

    /**
     * Gets an API token from the booking system to access protected data.
     *
     * @param scope The scope you want access to (eg. "Public.Events")
     * @return      The token to use in API calls
     * @throws IOException
     */
    public abstract String getAPIToken(String scope) throws IOException;
    
    /**
     * Gets the list of bookings for an event.
     *
     * @param eventKey The unique identifier for the event in the booking system
     * @return         A list of Booking objects for that event
     * @throws IOException
     * @see Booking
     */
    public abstract List<Booking> getBookingList(String eventKey) throws IOException;

    /**
     * Gets the waiting list of students for an event.
     *
     * @param eventKey The unique identifier for the event in the booking system
     * @return         A list of Student objects on the event waiting list
     * @throws IOException
     * @see Student
     */
    public abstract List<Student> getWaitingList(String eventKey) throws IOException;
    
    /**
     * Gets all unspecified bookings for an event.
     *
     * @param eventKey The unique identifier for the event in the booking system
     * @return         A list of String objects corresponding to unspecified student numbers
     * @throws IOException
     */
    public abstract List<String> getUnspecified(String eventKey) throws IOException;
    
    /**
     * Gets the number of attendees for an event.
     *
     * @param eventKey The unique identifier for the event in the booking system
     * @return         The number of recorded attendees
     * @throws IOException
     */
    public abstract int getAttendeeCount(String eventKey) throws IOException;
    
    /**
     * Gets the booking status for a student and an event.
     * 
     * @param studentKey The unique identifier for the attendee in the booking system
     * @param eventKey   The unique identifier for the event in the booking system
     * @return           A Booking if the user is booked, null if not
     * @throws IOException 
     * @see Booking
     */
    public abstract Booking getBooking(String studentKey, String eventKey) throws IOException;
    
    /**
     * Records an attendee with a particular booking status for an event.
     *
     * @param status     The STATUS enum value to record
     * @param studentKey The unique identifier for the attendee in the booking system
     * @param eventKey   The unique identifier for the event in the booking system
     * @throws IOException
     * @see STATUS
     */
    public abstract void markStatus(STATUS status, String studentKey, String eventKey) throws IOException;

    /**
     * Records a group of attendees with a particular booking status for an event.
     *
     * @param status      The STATUS enum value to record
     * @param studentKeys A List of unique identifiers for the attendees in the booking system
     * @param eventKey    The unique identifier for the event in the booking system
     * @throws IOException
     * @see STATUS
     */
    public abstract void markStatus(STATUS status, List<String> studentKeys, String eventKey) throws IOException;

    /**
     * Records a group of attendees as 'absent' in the booking system and
     * optionally notifies them.
     *
     * @param studentKeys A List of unique identifiers for the attendees in the booking system
     * @param eventKey    The unique identifier for the event in the booking system
     * @param notify      Whether or not to notify the attendees that they have been marked absent
     * @throws IOException
     */
    public abstract void markAbsent(List<String> studentKeys, String eventKey, Boolean notify) throws IOException;

    /**
     * Marks all unspecified bookings as 'absent' in the booking system and
     * optionally notifies the attendees.
     *
     * @param eventKey The unique identifier for the event in the booking system
     * @param notify   Whether or not to notify the attendees that they have been marked absent
     * @throws IOException
     */
    public abstract void markAllUnspecifiedAbsent(String eventKey, Boolean notify) throws IOException;

    /**
     * Cancels a booking.
     *
     * @param studentKey The unique identifier for the attendee in the booking system
     * @param eventKey   The unique identifier for the event in the booking system
     * @throws IOException
     */
    public abstract void cancelBooking(String studentKey, String eventKey) throws IOException;
    
    /**
     * Adds a booking to an event.
     *
     * @param studentKey The unique identifier for the attendee in the booking system
     * @param eventKey   The unique identifier for the event in the booking system
     * @return           The new Booking object added to the event.
     * @throws IOException
     * @see Booking
     */
    public abstract Booking bookStudent(String studentKey, String eventKey) throws IOException;
    
    /**
     * Returns the Student corresponding to the student number in the booking system.
     *
     * @param stuNumber The student number in the booking system (note: this is not the same as the unique identifier)
     * @return          The corresponding Student object
     * @throws IOException
     * @see Student
     */
    public abstract Student getStudent(String stuNumber) throws IOException;
    
    /**
     * Returns a List of students matching a search term.
     *
     * @param search The term to search (a name or a student number)
     * @return       A List of matching Student objects
     * @throws IOException
     * @see Student
     */
    public abstract List<Student> getStudents(String search) throws IOException ;

    /**
     * Returns a List of events taking place in the next day.
     *
     * @return           A List of Event objects
     * @throws IOException
     * @see Event
     */
    public abstract List<Event> getEventsList() throws IOException;
    
    /**
     * Returns the Event corresponding to the identifier in the booking system.
     *
     * @param eventKey The unique identifier for the event in the booking system
     * @return         The corresponding Event object
     * @throws IOException
     * @see Event
     */
    public abstract Event getEvent(String eventKey) throws IOException;
    
    /**
     * Returns the booking system admin URL for the event corresponding to the identifier
     * in the booking system
     *
     * @param eventKey The unique identifier for the event in the booking system
     * @return         The URL for administrators to access the event in the booking system
     */
    public abstract String getAdminEventURL(String eventKey);

    /**
     *
     * @return The charset used by the booking system API
     */
    public abstract String getCharset();

    /**
     *
     * @return The default string to replace empty student numbers
     */
    public abstract String getEmptyStuNumString();

    /**
     *
     * @return the number corresponding with a booking status of 'attending' in the booking system
     */
    public abstract int getATTENDED_STATUS();

    /**
     *
     * @return the number corresponding with the event being full in the booking system
     */
    public abstract int getEVENT_FULL_STATUS();
    
    /**
     *
     * @return the number corresponding with a booking status of 'absent' in the booking system
     */
    public abstract int getABSENT_STATUS();

    /**
     *
     * @return the number corresponding with a booking status of 'unspecified' in the booking system
     */
    public abstract int getUNSPECIFIED_STATUS();
    
        /**
     *
     * @return the number corresponding with a booking status of 'not booked' in the booking system
     */
    public abstract int getNOT_BOOKED_STATUS();

    /**
     * Returns true/false to indicate whether a student number is a valid format or not.
     *
     * @param stuNum The student number to test (note: this is not the same as the unique identifier which is always numeric)
     * @return       True/false depending on whether the student number is valid
     */
    public abstract boolean isValidStuNum(String stuNum);

   /**
     *
     * Called when a valid properties file exists. Initialises all booking system variables from property values.
     * @throws java.io.IOException
     */
    public abstract void init() throws IOException;

    /**
     * All possible booking statuses in the booking system.
     */
    public enum STATUS {

        /**
         * Status indicating that the student is booked but their attendance is unknown.
         */
        UNSPECIFIED,

        /**
         * Status indicating that the booker attended the event.
         */
        ATTENDED,

        /**
         * Status indicating that the booker did not attend the event.
         */
        ABSENT,

        /**
         * Status indicating that a person did not have a booking for the event.
         */
        NOT_BOOKED,

        /**
         * Status indicating that the booking could not be recorded because the event is full.
         */
        EVENT_FULL
    }

}
