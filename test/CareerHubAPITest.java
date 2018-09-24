import java.util.Map;
import java.util.HashMap;
import eventswipe.EventSwipeData;
import eventswipe.EventSwipeApp;
import java.util.Properties;
import eventswipe.exceptions.NoStudentFoundException;
import eventswipe.models.Event;
import eventswipe.models.Student;
import eventswipe.models.Booking;
import java.util.ArrayList;
import eventswipe.APIs.BookingSystemAPI.STATUS;
import java.util.List;
import eventswipe.APIs.CareerHubAPI;
import eventswipe.APIs.BookingSystemAPI;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CareerHubAPITest {
    
    private static final Logger LOG = Logger.getLogger(CareerHubAPITest.class.getName());

    //Start tests with Emma booked into the event as unspecified only
    private static BookingSystemAPI api;
    private static EventSwipeApp app;
    private static List<Booking>  bookings;
    private static EventSwipeData data;
    private static Student student;
    private static final String EVENT_KEY = "203802";
    private static final String USERNAME = "eventswipe";
    private static final char[] PASSWORD = {'m','i','k','e','t','i','l','e','y'};

    public CareerHubAPITest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        Logger.getLogger(CareerHubAPI.class.getName()).setLevel(Level.FINEST);
        api = CareerHubAPI.getInstance();
        app = new EventSwipeApp();
        data = EventSwipeData.getInstance();

        Properties p = app.getProperties(EventSwipeData.API_PROPERITES_PATH);

        @SuppressWarnings({"unchecked", "rawtypes"})
        Map<String, String> pMap = new HashMap(p);

        data.setCustomProperties(pMap);
        api.init();
        api.logIn(USERNAME, PASSWORD);
        bookings = new ArrayList<>();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void t02_apiGetStudentTest() {
        try {
            student = api.getStudent("349154");
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        assertEquals("Incorrect name", "Matt", student.getFirstName());
        assert(student.getId()== 38);
    }

    @Test
    public void t01_apiBookTest() {
        try {
            student = api.getStudent("349154");
            Booking booking = api.bookStudent(student.getId().toString(), EVENT_KEY);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            fail();
        }
    }

    @Test
    public void t11_apiGetTest() {
        try {
            bookings = api.getBookingList(EVENT_KEY);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        assertEquals("Bookings ArrayList is the wrong size", 2, bookings.size());
        assertEquals("Incorrect name", "Emma", bookings.get(0).getFirstName());
    }
 
    @Test
    public void t03_apiGetNumberOfAttendees1() {
        int count = 0;
        try {
            count = api.getAttendeeCount(EVENT_KEY);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        assertEquals("Incorrect attendee count", 0, count);
    }

    @Test
    public void t12_apiMarkMultipleAttendedTest() {
        List<String> keys = new ArrayList<>();
        for (int i=0; i < bookings.size(); i++) {
            keys.add(bookings.get(i).getBookingId().toString());
        }
        try {
            api.markStatus(STATUS.ATTENDED, keys, EVENT_KEY);
        } catch (IOException ex) {
           LOG.log(Level.SEVERE, null, ex);
        }
    }
    
    @Test
    public void t13_apiGetNumberOfAttendees2() {
        int count = 0;
        try {
            count = api.getAttendeeCount(EVENT_KEY);
        } catch (IOException ex) {
           LOG.log(Level.SEVERE, null, ex);
            fail();
        }
        assertEquals("Incorrect attendee count", 2, count);
    }

    @Test
    public void t14_apiMarkMultipleUnspecifiedTest() {
        try {
            api.markStatus(STATUS.UNSPECIFIED, bookings.get(0).getBookingId().toString(), EVENT_KEY);
            api.markStatus(STATUS.UNSPECIFIED, bookings.get(1).getBookingId().toString(), EVENT_KEY);
        } catch (IOException ex) {
           LOG.log(Level.SEVERE, null, ex);
            fail();
        }
    }

    @Test
    public void t04_apiGetEventTitleTest() {
        String title = "";
        try {
            title = api.getEvent(EVENT_KEY).getTitle();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            fail();
        }
        assertEquals("Incorrect title", "Test event", title);
    }

    @Test
    public void t05_apiGetWaitingListTest() {
        List<Student> waitingList = new ArrayList<>();
        try {
            waitingList = api.getWaitingList(EVENT_KEY);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            fail();
        }
        Student waitingStudent = waitingList.get(0);
        assertEquals("Incorrect id", 32, (int)waitingStudent.getId());
        waitingStudent = waitingList.get(1);
        assertEquals("Incorrect id", 80514, (int)waitingStudent.getId());
    }

    @Test
    public void t06_getStudentsTest() {
        List<Student> students = new ArrayList<>();
        try {
            students = api.getStudents("wildman");
        } catch (IOException ex) {
           LOG.log(Level.SEVERE, null, ex);
        }
        Student me = students.get(0);
        Student otherStudent = students.get(1);
        assertEquals("Result set is the wrong size", 3, students.size());
        assertEquals("My details incorrect", "Matt", me.getFirstName());
        assertEquals("Other student's details incorrect", "ANIKE", otherStudent.getFirstName());
    }

    @Test
    public void t07_getMoreStudentsTest() {
        List <Student> students = new ArrayList<>();
        try {
            students = api.getStudents("emma");
        } catch (IOException ex) {
          LOG.log(Level.SEVERE, null, ex);
        }
        assertFalse("No students retrieved", students.isEmpty());
    }

    @Test
    public void t08_getStudentTest() {
        Student me = new Student(), notFound = new Student();
        try {
            me = api.getStudent("349154");
            notFound = api.getStudent("999999999999999");
        } catch (NoStudentFoundException nsf) {
            assertEquals("Error catch failed", "999999999999999", nsf.getStuNum());
        } catch (IOException ex) {
           LOG.log(Level.SEVERE, null, ex);
        }
        assertEquals("Student should not have been found", notFound.getFirstName(), null);
        assertEquals("Incorrect student", "Matt", me.getFirstName());
    }

    @Test
    public void t09_getBookingLimitTest() {
        Event event = new Event();
        Event event2 = new Event();
        Event event3 = new Event();
        Event event4 = new Event();
        try {
            event = api.getEvent(EVENT_KEY); //regular CareerHub booking
            event2 = api.getEvent("212627"); //no booking
            event3 = api.getEvent("240665"); //custom booking limit
            event4 = api.getEvent("240667"); //external booking
        }
        catch (IOException ex) {
         LOG.log(Level.SEVERE, null, ex);
        }
        assertEquals("Incorrect booking limit", 5, event.getBookingLimit());
        assert(event2.isDropIn());
        assert(event4.isDropIn());
        assertFalse(event2.isUnlimited());
        assertEquals("Incorrect booking limit", 120, event3.getBookingLimit());
    }

    @Test
    public void t15_getUnspecifiedTest() {
        List<String> unspecifieds = new ArrayList<>();
        try {
            unspecifieds = api.getUnspecified(EVENT_KEY);
        } catch (IOException ex) {
           LOG.log(Level.SEVERE, null, ex);
        }
        assertEquals("Incorrect number of unspecified students", 2, unspecifieds.size());
    }

    @Test
    public void t16_markAbsentTest() {
        try {
            api.markAllUnspecifiedAbsent(EVENT_KEY, false);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            fail();
        }
    }
    /*
    @Test
    public void t10_getStudentWithNoLastName() {
        try {
            Student stu = api.getStudent("201610493");
            System.out.println(stu.getFirstName() + " " + stu.getLastName());
        } catch (IOException ex) {
            System.out.println("Fetching student failed");
            LOG.log(Level.SEVERE, null, ex);
            fail();
        }
    }
    */
    @Test
    public void t17_apiCancelBookingTest() {
        try {
            api.cancelBooking(bookings.get(1).getBookingId().toString(), EVENT_KEY);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            fail();
        }
    }
    
    @Test
    public void t18_markUnspecifiedTest() {
        try {
            Event event = api.getEvent(EVENT_KEY);
            event.setBookingList(api.getBookingList(EVENT_KEY));
            Booking booking = event.getBookingList().get(0);
            api.markStatus(STATUS.UNSPECIFIED, String.valueOf(booking.getBookingId()), EVENT_KEY);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }
}