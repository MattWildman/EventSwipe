import java.util.Map;
import java.util.HashMap;
import eventswipe.EventSwipeApp;
import eventswipe.EventSwipeData;
import java.util.Properties;
import eventswipe.APIs.BookingSystemAPI;
import java.util.List;
import java.util.ArrayList;
import eventswipe.APIs.BookingSystemAPI.STATUS;
import java.util.Date;
import eventswipe.models.Booking;
import eventswipe.models.Event;
import eventswipe.APIs.CareerHubAPI;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class NewAPITest {
    //Start tests with Emma as unspecified as only booking
    private static BookingSystemAPI api;
    private static EventSwipeApp app;
    private static EventSwipeData data;
    private static final String USERNAME = "eventswipe";
    private static final char[] PASSWORD = {'m','i','k','e','t','i','l','e','y'};
    
    public NewAPITest() {}

    @BeforeClass
    public static void setUpClass() throws Exception {
        app = new EventSwipeApp();
        api = CareerHubAPI.getInstance();
        data = EventSwipeData.getInstance();
        
        Properties p = app.getProperties(EventSwipeData.API_PROPERITES_PATH);

        @SuppressWarnings({"unchecked", "rawtypes"})
        Map<String, String> pMap = new HashMap(p);

        data.setCustomProperties(pMap);
        api.init();
        api.logIn(USERNAME, PASSWORD);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {}

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    /*
    @Test
    public void apiLoginTest() throws Exception {
        try {
            assert(api.logIn(USERNAME, PASSWORD));
        } catch (IOException ex) {
            Logger.getLogger(CareerHubAPITest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    */
    @Test
    public void getTokenTest() {
        String token= "";
        try {
            token = api.getAPIToken("Integrations.Events");
        } catch (MalformedURLException ex) {
            Logger.getLogger(NewAPITest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(NewAPITest.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Token: " + token);
        assertFalse(token.isEmpty());
    }

    @Test
    public void getEventsListTest() {
        List<Event> events = new ArrayList<>();
        try {
            events = api.getEventsList();
        } catch (MalformedURLException ex) {
            Logger.getLogger(NewAPITest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(NewAPITest.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Number of events: " + events.size());
        assertFalse(events.isEmpty());
    }

    @Test
    public void getEvent() {
        Event event = new Event();
        try {
            event = api.getEvent("203802", true);
        } catch (IOException ex) {
            Logger.getLogger(NewAPITest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertEquals("Incorrect title", "Test event", event.getTitle());
    }

    @Test
    public void eventDateTest() {
        Event aEvent = new Event();
        Event iEvent = new Event();
        try {
            aEvent = api.getEvent("260975", false);
            iEvent = api.getEvent("260972", false);
        } catch (IOException ex) {
            Logger.getLogger(NewAPITest.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Active date: " + aEvent.getStartDate());
        System.out.println("Active regs: " + aEvent.getRegStart());
        System.out.println("Inactive date: " + iEvent.getStartDate());
        System.out.println("Inactive regs: " + iEvent.getRegStart());
    }

    @Test
    public void earlyEntryTest() {
        try {
            api.logIn(USERNAME, PASSWORD);
       } catch (IOException ex) {
            Logger.getLogger(NewAPITest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
        Event event;
        Booking booking = new Booking("349154");
            try {
                event = api.getEvent("203802", true);
                System.out.println("Bookings: " + event.getBookingList().size());
                Date now = new Date();
                System.out.println("Now: " + now);
                System.out.println("Reg start: " + event.getRegStart());
                System.out.println("Event start: " + event.getStartDateString());
                assert(now.after(event.getRegStart()));
                if (now.after(event.getRegStart())) {
                    System.out.println("Not too early");
                    for (Booking b : event.getBookingList()) {
                        if (b.getStuNumber().equals("123456")) {
                            System.out.println("Booking id: " + b.getBookingId().toString());
                            String bookingId = b.getBookingId().toString();
                            api.markStatus(STATUS.ATTENDED, bookingId, event.getId());
                            b.setStatus(api.getATTENDED_STATUS());
                            booking = b;
                        }
                    }
                    assertEquals("Status is incorrect", (Integer)api.getATTENDED_STATUS(), booking.getStatus());
                }
                else {
                    System.out.println("Too early");
                    fail("Student shouldn't be marked early.");
                    booking.setStatus(Booking.EARLY_STATUS);
                }
            } catch (IOException ex) {
                Logger.getLogger(NewAPITest.class.getName()).log(Level.SEVERE, null, ex);
                fail("Error setting up the event");
            }
    }
    
    @Test
    public void checkStatusTest() {
        String eventKey = "203802";
        Booking unspecified = null, attended = null, absent = null, not_booked = null;
        try {
            Event event = api.getEvent(eventKey, true);
            attended = api.getBooking("123456", eventKey);
            not_booked = api.getBooking("349154", eventKey);
            Booking booking = event.getBookingList().get(0);
            String bookingId = String.valueOf(booking.getBookingId());
            String externalId = booking.getStuNumber();
            api.markStatus(STATUS.ABSENT, bookingId, eventKey);
            absent = api.getBooking(externalId, eventKey);
            api.markStatus(STATUS.UNSPECIFIED, bookingId, eventKey);
            unspecified = api.getBooking(externalId, eventKey);
        } catch (IOException ex) {
            Logger.getLogger(NewAPITest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
        assert(attended.getStatus() == api.getATTENDED_STATUS());
        assert(not_booked.getStatus() == api.getNOT_BOOKED_STATUS());
        assert(absent.getStatus() == api.getABSENT_STATUS());
        assert(unspecified.getStatus() == api.getUNSPECIFIED_STATUS());
    }

}