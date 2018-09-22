package eventswipe;

import eventswipe.models.Booking;
import eventswipe.models.Event;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains all the data for an attendance recording session.
 *
 * @author Matt Wildman http://bitbucket.com/mattwildman
 */
public class EventSwipeData {

    /**
     * The maximum entry slots of a multi-slot event.
     */
    public static final int MAX_ENTRY_SLOTS = 5;

    public final int ATTENDED_STATUS = 1;
    public final int UNSPECIFIED_STATUS = 0;
    public final int EVENT_FULL_STATUS = -1;

    public static final String API_PROPERITES_PATH = "BookingSystem.properties";
    public static final String HOST_KEY = "host";
    public static final String API_ID_KEY = "id";
    public static final String API_SECRET_KEY = "secret";
    public static final String STUDENT_ID_PATTERN_KEY = "studentIdPattern";
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";
    public static final String STATUS_KEY = "status";

    public static final Map<String,String> DEFAULT_PROPS;
    static {
        DEFAULT_PROPS = new HashMap<>();
        DEFAULT_PROPS.put(STATUS_KEY, "default");
        DEFAULT_PROPS.put(STUDENT_ID_PATTERN_KEY, "^.+$");
        DEFAULT_PROPS.put(API_SECRET_KEY, "");
        DEFAULT_PROPS.put(HOST_KEY, "");
        DEFAULT_PROPS.put(API_ID_KEY, "");
        DEFAULT_PROPS.put(USERNAME_KEY, "");
        DEFAULT_PROPS.put(PASSWORD_KEY, "");
    }

   /**
     * Singleton constructor for EventSwipeData
     * @return the instance of EventSwipeData
    */
    public static EventSwipeData getInstance() {
        if (instance == null) {
            instance = new EventSwipeData();
        }
        return instance;
    }

    private EventSwipeData() {
        allFileBookedList = new ArrayList<>();
    }

    /**
     * @return The List of Events running (multiple when recording attendance for a multi-slot event)
     * @see Event
     */
    public List<Event> getEvents() {
        return events;
    }

    /**
     * Sets the List of Events running (multiple when recording attendance for a multi-slot event)
     *
     * @param events A List of Events
     * @see Event
     */
    public void setEvents(List<Event> events) {
        this.events = events;
    }

    /**
     * Adds an Event (or entry slot in a multi-slot event) to start recording attendance
     *
     * @param event An Event
     * @see Event
     */
    public void addEvent(Event event) {
        this.initialiseAttendees(event);
        events.add(event);
    }

    /**
     * @return a Map<String, String> of the custom booking system properties
     * @see Map
     */
    public Map<String, String> getCustomProperties() {
        return customProperties;
    }

    /**
     * Sets a Map of the custom booking system properties which the BookingSystemAPI uses
     *
     * @param customProperties a Map<String, String> the custom booking system properties to set
     */
    public void setCustomProperties(Map<String, String> customProperties) {
        this.customProperties = customProperties;
    }

    /**
     * @return the default username to use when logging in to the booking system
     */
    public String getDefaultUsername() {
        return defaultUsername;
    }

    /**
     * Set a default username to use when logging in to the booking system
     *
     * @param defaultUsername the default username to set
     */
    public void setDefaultUsername(String defaultUsername) {
        this.defaultUsername = defaultUsername;
    }

    /**
     * @return the default password to use when logging in to the booking system
     */
    public char[] getDefaultPassword() {
        return defaultPassword;
    }

    /**
     * Set a default password to use when logging in to the booking system
     *
     * @param defaultPassword the defaultPassword to set
     */
    public void setDefaultPassword(char[] defaultPassword) {
        this.defaultPassword = defaultPassword;
    }

    /**
     * Clears all data in current attendance recording session.
     */
    public void clearData() {
        setLocalAttendeeCount(0);
        setWaitingListFlag(false);
        setSlots(0);
        setEventTitle("");
        clearList(allFileBookedList);
        events.clear();
    }

    /**
     * Adds 1 to the number of attendees recorded on the
     * machine where EventSwipe is running then returns this number.
     *
     * @return The number of locally recorded attendees, after incrementation
     */
    public int incrementAttendeesCount() {
        setLocalAttendeeCount(getLocalAttendeeCount() + 1);
        return getLocalAttendeeCount();
    }

    /**
     * @return The number of attendees recorded on the machine where EventSwipe is running
     */
    public int getLocalAttendeeCount() {
        return localAttendeeCount;
    }

    /**
     * Sets the number of attendees recorded on the machine where EventSwipe is running.
     *
     * @param count The number of locally recorded attendees
     */
    public void setLocalAttendeeCount(int count) {
        this.localAttendeeCount = count;
    }

    /**
     * @return The total number of attendees recorded for the Event(s)
     */
    public int getGlobalAttendeeCount() {
        return localAttendeeCount;
    }

    /**
     * Sets the total number of attendees recorded for the Event(s)
     *
     * @param count The total number of attendees recorded
     */
    public void setGlobalAttendeeCount(int count) {
        this.localAttendeeCount = count;
    }

    /**
     * @return A List of all the student number Strings that have been recorded
     */
    public List<String> getAllRecordedList() {
        return allFileBookedList;
    }

    /**
     * Sets the List of everyone who has booked for the Event(s)
     *
     * @param allBookedList A List of student number Strings
     */
    public void setAllBookedList(ArrayList<String> allBookedList) {
        this.allFileBookedList = allBookedList;
    }

    /**
     * @return True if connected to the internet, false if not
     */
    public boolean getNetFlag() {
        return netFlag;
    }
    
    /**
     * Sets whether or not EventSwipe can access the internet.
     *
     * @param b True/false depending on whether internet access is possible
     */
    public void setNetFlag(boolean b) {
        this.netFlag = b;
    }

    /**
     * @return True if EventSwipe is in 'online mode', false if not
     */
    public boolean isOnlineMode() {
        return onlineMode;
    }

    /**
     * Sets whether EventSwipe is in online mode or not.
     *
     * @param onlineMode True/false to set whether or not EventSwipe is in online mode
     */
    public void setOnlineMode(boolean onlineMode) {
        this.onlineMode = onlineMode;
    }

    /**
     * @return The number of entry slots of the event
     */
    public int getSlots() {
        return slots;
    }

    /**
     * Sets the number of entry slots of the event.
     *
     * @param slots The number of entry slots
     */
    public void setSlots(int slots) {
        this.slots = slots;
    }

    /**
     * Sets whether or not the event is a single entry slot event.
     *
     * @param b True/false depending on whether the event is a single slot event or not
     */
    public void setSingleSlot(boolean b) {
        this.singleSlotFlag = b;
    }

    /**
     * @return True/false depending on whether the event has a single entry slot or not
     */
    public boolean isSingleSlot() {
        return singleSlotFlag;
    }

    /**
     * @return True/false depending on whether EventSwipe is checking waiting lists or not
     */
    public boolean isWaitingListFlag() {
        return waitingListFlag;
    }

    /**
     * Sets whether or not EventSwipe is checking waiting lists.
     *
     * @param waitingListFlag True/false to check waiting lists or not
     */
    public void setWaitingListFlag(boolean waitingListFlag) {
        this.waitingListFlag = waitingListFlag;
    }

    /**
     * @return True/false depending on whether EventSwipe is checking booking lists
     */
    public boolean isCheckingBookingLists() {
        return bookingFlag;
    }

    /**
     * Sets whether EventSwipe checks booking lists during attendance recording or not.
     *
     * @param bookingFlag True/false to checking booking lists or not
     */
    public void setBookingFlag(boolean bookingFlag) {
        this.bookingFlag = bookingFlag;
    }

    /**
     * @return True/false depending on whether all attendance data has been saved.
     */
    public boolean getSavedFlag() {
        return savedFlag;
    }

    /**
     * Sets the save flag according to whether all attendance data has been saved.
     *
     * @param savedFlag True/false depending on whether saved attendance is up to date
     */
    public void setSavedFlag(boolean savedFlag) {
        this.savedFlag = savedFlag;
    }

    /**
     * @return True/false according to whether there is an authorised user logged into EventSwipe
     */
    public boolean isLoggedInFlag() {
        return loggedInFlag;
    }

    /**
     * Sets the logged in flag according to whether there is an authorised user logged in.
     *
     * @param success True/false depending on the success of the login attempt
     */
    public void setLoggedInFlag(boolean success) {
        this.loggedInFlag = success;
    }

    /**
     * @return True/false depending on whether the properties file exists
     */
    public boolean isPropertiesFlag() {
        return propertiesFlag;
    }

    /**
     * @param propertiesFlag True/false depending on whether the properties file exists
     */
    public void setPropertiesFlag(boolean propertiesFlag) {
        this.propertiesFlag = propertiesFlag;
    }

    /**
     * @return The overall event title
     */
    public String getEventTitle() {
        return eventTitle;
    }

    /**
     * Sets the overall event title.
     *
     * @param eventTitle The event title to set
     */
    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    /**
     * @return The number of bookings for all event entry slots
     */
    public Integer getBookingCount() {
        int c = 0;
        for (Event e : events) {
            c += e.getBookingList().size();
        }
        return c;
    }

    /**
     * @return The number of attendees for all event entry slots
     */
    public Integer getAttendeeCount() {
        return this.getAllRecordedList().size();
    }

    /**
     * @return The number of saved attendees for all event entry slots
     */
    public Integer getSavedCount() {
        int c = 0;
        for (Event e : events) {
            c += e.getAttendeeCount();
        }
        return c;
    }

    /**
     * @return The number of unsaved attendees for all event entry slots
     */
    public Integer getUnsavedCount() {
        int c = 0;
        for (Event e : events) {
            c += e.getUnsavedList().size();
        }
        return c;
    }

    /**
     * @return The number of people yet to attend all event entry slots
     */
    public Integer getNotAttendedCount() {
        int diff = this.getBookingCount() - this.getAttendeeCount();
        return diff < 0 ? 0 : diff;
    }

    /**
     * @return The number of free places in all event entry slots
     */
    public Integer getCurrentNumberOfPlaces() {
        int limit = 0;
        for (Event e : events) {
            if (e.isUnlimited() || e.isDropIn()) {
                return -1;
            }
            limit += e.getBookingLimit();
        }
        return limit - this.getAttendeeCount();
    }

    /**
     * @return The current value of the counter in counter mode
     */
    public Integer getCount() {
        return count;
    }

    /**
     * Increases the value of the counter by one and returns the current value in counter mode.
     *
     * @return The current value of the counter after incrementation
     */
    public int incrementCount() {
        count++;
        return count;
    }

    /**
     * Sets the value of the counter in counter mode.
     *
     * @param c The value to set the counter to
     */
    public void setCount(int c) {
        count = c;
    }

    private List<Event> events = new ArrayList<>();

    private List<String> allFileBookedList;

    private String eventTitle;
    private int localAttendeeCount = 0;
    private int slots;
    private boolean netFlag;
    private boolean onlineMode;
    private boolean waitingListFlag;
    private boolean bookingFlag;
    private boolean savedFlag = true;
    private boolean singleSlotFlag = false;
    private boolean loggedInFlag = false;
    private boolean propertiesFlag = false;
    private String defaultUsername = "";
    private char[] defaultPassword = {};

    private Map<String,String> customProperties = null;
    
    private int count = 0;

    private static EventSwipeData instance = null;

    private void initialiseAttendees(Event event) {
        if (this.isOnlineMode() && !event.isDropIn() && event.getBookingList().size() > 0) {
            for (Booking b : event.getBookingList()) {
                if (b.getStatus() == ATTENDED_STATUS) {
                    this.getAllRecordedList().add(b.getStuNumber());
                }
            }
        }
    }
    
    private void clearList(List<String> list) {
        if (!list.isEmpty()) {
            list.clear();
        }
    }

}
