package eventswipe.models;

import java.util.Objects;

/**
 * Contains the student and event data for a particular booking.
 * <p>
 * Bookings are also used as messages passed from the application to the view
 * in order to display the booking status to the user.
 *
 * @author Matt Wildman http://bitbucket.org/mattwildman
 */
public class Booking {

    /**
     * A status indicating that the attendee is too early to register for the event.
     */
    public static final Integer EARLY_STATUS = 3;

    /**
     * Constructs a booking with a student number.
     *
     * @param stuNumber A student number String
     */
    public Booking(String stuNumber) {
        setStuNumber(stuNumber);
    }
    
    @Override
    public boolean equals(Object other) {
        return super.equals(other) || 
               ((other.getClass().getName().equals(this.getClass().getName())) && 
                this.getBookingId().equals(((Booking)other).getBookingId()));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.bookingId);
        return hash;
    }

    /**
     * @return The student number String of the attendee with the booking
     */
    public String getStuNumber() {
        return stuNumber;
    }

    /**
     * @return The number of the entry slot the booking is for (when recording attendance for a multi-slot event)
     */
    public Integer getEntrySlot() {
        return entrySlot;
    }

    /**
     * Sets the entry slot the booking is for
     * (when recording attendance for a multi-slot event).
     *
     * @param entrySlot The number of the event entry slot
     */
    public void setEntrySlot(Integer entrySlot) {
        this.entrySlot = entrySlot;
    }

    /**
     * @return True/false depending on whether the attendee with the booking has booked for the event
     */
    public boolean isBooked() {
        return booked;
    }

    /**
     * Sets whether the booking is a valid booking (ie whether the attendee has booked).
     *
     * @param booked True if the attendee has booked, false if not
     */
    public void setBooked(boolean booked) {
        this.booked = booked;
    }

    /**
     * @return True/false depending on whether the attendee with the booking has already been recorded for the event
     */
    public boolean isAlreadyRecorded() {
        return alreadyRecorded;
    }

    /**
     * Sets whether the attendee with the booking
     * has already been recorded for the event.
     *
     * @param alreadyRecorded True if the attendee has already been recorded, false if not
     */
    public void setAlreadyRecorded(boolean alreadyRecorded) {
        this.alreadyRecorded = alreadyRecorded;
    }

    /**
     * @return True/false depending on whether the attendee is on the waiting list for the event
     */
    public boolean isOnWaitingList() {
        return onWaitingList;
    }

    /**
     * Sets whether the attendee is on the waiting list for the event.
     *
     * @param waitingList True is the attendee is on the waiting list, false if not
     */
    public void setOnWaitingList(boolean waitingList) {
        this.onWaitingList = waitingList;
    }

    /**
     * @return The unique identifier of the attendee in the booking system
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the attendee in the booking system.
     *
     * @param id The unique id of the attendee
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return The first name of the attendee with the Booking
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the first name of the attendee with the Booking.
     *
     * @param firstName The first name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * @return The last name of the attendee with the Booking
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the last name of the attendee with the Booking.
     *
     * @param lastName The last name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * @return The unique identifier of the Booking in the booking system
     */
    public Integer getBookingId() {
        return bookingId;
    }

    /**
     * Sets the unique identifier of the Booking in the booking system.
     *
     * @param bookingId The unique id of the Booking
     */
    public void setBookingId(Integer bookingId) {
        this.bookingId = bookingId;
    }

    /**
     * @return The status of the booking
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * Sets the status of the booking.
     *
     * @param status A number representing the booking status
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    private void setStuNumber(String stuNumber) {
        this.stuNumber = stuNumber;
    }

    private String stuNumber = "";
    private Integer bookingId = 0;
    private Integer id;
    private String firstName;
    private String lastName = "";
    private Integer status;
    
    private Integer entrySlot;

    private boolean booked;
    private boolean alreadyRecorded;
    private boolean onWaitingList;

}
