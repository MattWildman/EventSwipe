package eventswipe.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Matt Wildman
 */
public class Session {
    
    public Session() {
        this.earlyList = new ArrayList<>();
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the start date and time
     */
    public Date getStart() {
        return start;
    }

    /**
     * @param start the start date and time
     */
    public void setStart(Date start) {
        this.start = start;
    }
    
    /**
     * @return the end date and time
     */
    public Date getEnd() {
        return end;
    }

    /**
     * @param end the end date and time
     */
    public void setEnd(Date end) {
        this.end = end;
    }

    /**
     * @return the bookingCount
     */
    public int getBookingCount() {
        return bookingCount;
    }

    /**
     * @param bookingCount the bookingCount to set
     */
    public void setBookingCount(int bookingCount) {
        this.bookingCount = bookingCount;
    }

    /**
     * @return A List of student numbers of attendees who were too early to register for the event
     */
    public List<String> getEarlyList() {
        return earlyList;
    }

    /**
     * Sets the List of student numbers of attendees
     * who were too early to register for the event.
     *
     * @param earlyList A List of student number Strings
     */
    public void setEarlyList(List<String> earlyList) {
        this.earlyList = earlyList;
    }
    
    private String id;
    private Date start;
    private Date end;
    
    private int bookingCount;

    private List<String> earlyList;
    
}
