package eventswipe.APIs;

import eventswipe.EventSwipeData;
import eventswipe.exceptions.*;
import eventswipe.utils.*;
import eventswipe.models.*;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CareerHubAPI extends BookingSystemAPI {

    public static BookingSystemAPI getInstance() {
        if (instance == null) {
            instance = new CareerHubAPI();
        }
        return instance;
    }
    
    protected CareerHubAPI() {}

    @Override
    public void init() {
        Map<String, String> p = EventSwipeData.getInstance().getCustomProperties();

        HOST = p.get(EventSwipeData.HOST_KEY);
        API_ID = p.get(EventSwipeData.API_ID_KEY);
        SECRET = p.get(EventSwipeData.API_SECRET_KEY);
        STU_NUM_PATTERN = p.get(EventSwipeData.STUDENT_ID_PATTERN_KEY);
        ADMIN_URL = HOST + "admin/";

        LOGIN_URL =            ADMIN_URL + "login/";
        QUERY_URL_TEMPL =      ADMIN_URL + "events/bookings/query/%s?sessionId=%s";
        BOOKING_URL =          ADMIN_URL + "events/bookings/create/";
        MARK_ATTENDED_URL =    ADMIN_URL + "events/bookings/markattended/";
        MARK_UNSPECIFIED_URL = ADMIN_URL + "events/bookings/markunspecified/";
        MARK_ABSENT_URL =      ADMIN_URL + "events/bookings/markabsent/";
        WAITING_LIST_BASE =    ADMIN_URL + "eventwaitinglist.aspx?id=";
        STUDENT_SEARCH_BASE =  ADMIN_URL + "suggest/JobSeeker";
        EVENT_ADMIN_URL_BASE = ADMIN_URL + "event.aspx?id=";
        
        EVENT_API_URL = HOST + "api/integrations/v1/events/";
        EVENT_BOOKING_URL_TEMPL = EVENT_API_URL + "bookings/%s/%s?sessionId=%s";
        EVENT_API_LIST_URL = EVENT_API_URL + "?filterOptions.filterIds=82&filterOptions.filterIds=83&filterOptions.filterOperator=And";
    }

    @Override
    public boolean logIn(String username, char[] password) throws IOException {
        if (isAlreadyLoggedIn()) {
            return true;
        }
        String dateStr = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(new Date());
        Map<String,String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded;charset=" + getCharset());
        requestHeaders.put("Cookie", "CareerHubCookieCheck=" + dateStr);
        String loginData = "__RequestVerificationToken=" + getVerificationToken() +
                          "&username=" + username +
                          "&password=" + String.valueOf(password) +
                          "&isPersistent=true&isPersistent=false";
        HttpUtils.sendDataToURL(LOGIN_URL, "POST", loginData, getCharset(), requestHeaders);
        return isAlreadyLoggedIn();
    }

    @Override
    public String getAPIToken(String scope) throws IOException {
        AccessToken t = tokens.get(scope);
        if (t == null || t.getExpiryDate().before(new Date())) {
            t = new AccessToken();
            String apiURL = HOST + "oauth/token";
            String postdata = "grant_type=client_credentials" +
                              "&client_id=" + URLEncoder.encode(API_ID, charset) +
                              "&client_secret=" + URLEncoder.encode(SECRET, charset) +
                              "&scope=" + scope;
            Map<String,String> requestHeaders = new HashMap<>();
            requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
            String response = HttpUtils.sendDataToURL(apiURL, "POST", postdata, charset, requestHeaders);
            JSONObject apiData = new JSONObject(response);
            t.setScope(scope);
            t.setToken(apiData.getString("access_token"));
            t.setTokenType(apiData.getString("token_type"));
            int expiresInMins = apiData.getInt("expires_in")/60 - 1;
            t.setExpiryDate(Utils.addMins(new Date(), expiresInMins));
            tokens.put(scope, t);
        }
        return t.getToken();
    }

    @Override
    public List<Booking> getBookingList(String eventKey, String sessionKey) throws IOException {
        List<Booking> bookings = new ArrayList<>();
        String response = HttpUtils.getDataFromURL(String.format(QUERY_URL_TEMPL, eventKey, sessionKey));
        LOG.log(Level.FINER, "Booking list JSON for event {0}: {1}", new Object[]{eventKey, response});
        JSONObject bookingData = new JSONObject(response);
        JSONArray jsonBookings = bookingData.getJSONArray("bookings");
        for (int i=0; i < jsonBookings.length(); i++) {
            JSONObject jsonBooking = jsonBookings.getJSONObject(i);
            Booking booking;
            try {
                booking = new Booking(jsonBooking.getString("externalId"));
                if (!jsonBooking.isNull("firstName")) {
                    booking.setFirstName(jsonBooking.getString("firstName"));
                } 
                else {
                    booking.setFirstName("");
                }
                if (!jsonBooking.isNull("lastName")) {
                    booking.setLastName(jsonBooking.getString("lastName"));
                } 
                else {
                    booking.setLastName("");
                }
                booking.setId(jsonBooking.getInt("jobSeekerId"));
                booking.setBookingId(jsonBooking.getInt("id"));
                booking.setSessionId(sessionKey);
                booking.setStatus(jsonBooking.getInt("status"));
                bookings.add(booking);
            } catch (org.json.JSONException je) {
                LOG.log(Level.SEVERE, "Empty student number for id: {0}", jsonBooking.getInt("jobSeekerId"));
            }
        }
        return bookings;
    }

    @Override
    public List<String> getUnspecified(String eventKey) throws IOException {
        List<String> unspecifiedNumbers = new ArrayList<>();
        Event event = this.getEvent(eventKey, true);
        for (Booking b: event.getBookingList()) {
            if (b.getStatus() == UNSPECIFIED_STATUS) {
                unspecifiedNumbers.add(b.getStuNumber());
            }
        }
        return unspecifiedNumbers;
    }

    @Override
    public List<Student> getWaitingList(String eventKey) throws IOException {
        List<Student> waitingList = new ArrayList<>();
        Document doc = Jsoup.connect(WAITING_LIST_BASE + eventKey).timeout(0).get();
        Elements linkElems = doc.select("#ctl00_ctl00_mainContent_mainContent_grid > tbody > tr > td:nth-child(2) > a");
        for (Element link : linkElems) {
            String url = link.attr("href");
            int id = Integer.parseInt(url.split("id=")[1]);
            Student waiting = new Student();
            waiting.setId(id);
            waitingList.add(waiting);
        }
        return waitingList;
    }

    @Override
    public int getAttendeeCount(String eventKey) throws IOException {
        Event event = this.getEvent(eventKey, false);
        return event.getAttendeeCount();
    }
    
    @Override
    public Booking getBooking(String externalId, String eventKey) throws IOException {
        String requestUrl = String.format(EVENT_BOOKING_URL_TEMPL, externalId, eventKey, "");
        String response = HttpUtils.getDataFromURL(requestUrl, getAPIAuthHeaders());
        System.out.println(response);
        Booking booking = new Booking(externalId);
        JSONObject statusObject = new JSONObject(response);
        int status;
        if (statusObject.getBoolean("isBooked")) {
            String statusStr = statusObject.getString("status");
            switch(statusStr) {
                case "Attended":
                    status = ATTENDED_STATUS;
                    break;
                case "Absent":
                    status = ABSENT_STATUS;
                    break;
                default:
                    status = UNSPECIFIED_STATUS;
            }
        } else {
            status = NOT_BOOKED_STATUS;
        }
        booking.setStatus(status);
        booking.setId(statusObject.getInt("jobSeekerId"));
        return booking;
    }

    @Override
    public void markStatus(STATUS status, String bookingId, String eventKey) throws IOException {
        List<String> key = Arrays.asList(bookingId);
        markStatus(status, key, eventKey);
    }

    @Override
    public void markStatus(STATUS status, List<String> bookingIds, String eventKey) throws IOException {
        Map<String,String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/json;charset=" + this.getCharset());
        String postData = "{\"eventID\":" + eventKey + "," +
                          "\"ids\":" + bookingIds;
        String url;
        switch (status) {
            case ATTENDED:
                url = MARK_ATTENDED_URL;
                break;
            case ABSENT:
                url = MARK_ABSENT_URL;
                postData += ",\"notify\":false";
                break;
            default:
                url = MARK_UNSPECIFIED_URL;
                break;
        }
        postData += "}";
        url += eventKey + "?sessionId="; //intentionally empty query parameter
        try {
            HttpUtils.sendDataToURL(url, "POST", postData, this.getCharset(), requestHeaders);
        }
        catch (IOException ioe) {
            if (ioe.getMessage().equals("Server returned HTTP response code: 400 for URL: " +
                                        MARK_ATTENDED_URL)) {
                throw new EarlyRegistrationException("Too early to mark as attended");
            }
        }
    }

    @Override
    public void markAbsent(List<String> bookingIds, String eventKey, Boolean notify) throws IOException {
        Map<String,String> requestHeaders = new HashMap<>();

        requestHeaders.put("Content-Type", "application/json;charset=" + this.getCharset());
        String postData = "{\"eventID\":" + eventKey + "," +
                          "\"ids\":" + bookingIds + "," +
                          "\"notify\":" + notify + "}";
        String url = MARK_ABSENT_URL + eventKey;
        HttpUtils.sendDataToURL(url, "POST", postData, this.getCharset(), requestHeaders);
    }

    @Override
    public void markAllUnspecifiedAbsent(String eventKey, Boolean notify) throws IOException {
        List<String> unspecifiedKeys = this.getUnspecified(eventKey);
        if (!unspecifiedKeys.isEmpty())
            this.markAbsent(unspecifiedKeys, eventKey, notify);
    }

    @Override
    public void cancelBooking(String externalId, String eventKey) throws IOException {
        String requestUrl = String.format(EVENT_BOOKING_URL_TEMPL, externalId, eventKey, "");
        HttpUtils.sendDeleteRequestToUrl(requestUrl, getAPIAuthHeaders());
    }

    @Override
    public Booking bookStudent(String jobSeekerId, String eventKey, String sessionId) throws IOException {
        Map<String,String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/json;charset=" + getCharset());
        String postData = "{\"eventID\":" + eventKey + "," +
                          "\"jobSeekerID\":" + jobSeekerId + "," +
                          "\"sessionID\":" + sessionId + "," +
                          "\"notify\":false}";
        String requestUrl = BOOKING_URL + eventKey + "?sessionId=" + sessionId;
        String bookingDetails = HttpUtils.sendDataToURL(requestUrl, "POST", postData, getCharset(), requestHeaders);
        JSONObject jsonBooking = (JSONObject) new JSONObject(bookingDetails).get("booking");
        LOG.log(Level.FINER, jsonBooking.toString());
        Booking booking = new Booking(jsonBooking.getString("externalId"));
        if (!jsonBooking.isNull("firstName")) {
            booking.setFirstName(jsonBooking.getString("firstName"));
        } 
        else {
            booking.setFirstName("");
        }
        if (!jsonBooking.isNull("lastName")) {
            booking.setLastName(jsonBooking.getString("lastName"));
        } 
        else {
            booking.setLastName("");
        }
        booking.setId(jsonBooking.getInt("jobSeekerId"));
        booking.setBookingId(jsonBooking.getInt("id"));
        booking.setStatus(jsonBooking.getInt("status"));
        return booking;
    }
    
    @Override
    public Booking bookStudentWithStuNumber(String externalId, String eventKey, String sessionId) throws IOException {
        String url = String.format(EVENT_BOOKING_URL_TEMPL, externalId, eventKey, sessionId);
        String response = HttpUtils.sendDataToURL(url, "POST", " ", charset, getAPIAuthHeaders());
        JSONObject jsonResponse = (JSONObject) new JSONObject(response);
        Booking booking = new Booking(externalId);
        booking.setId(jsonResponse.getInt("jobSeekerId"));
        booking.setStatus(this.getUNSPECIFIED_STATUS());
        return booking;
    }

    @Override
    public Student getStudent(String externalId) throws IOException {
        String query = "?s=" + externalId +
                       "&type=JobSeeker&maxResults=1&current=Current&active=true";
        String stuData = HttpUtils.getDataFromURL(STUDENT_SEARCH_BASE + query);
        LOG.log(Level.FINER, stuData);
        JSONObject jsonStudent = null;
        try {
            jsonStudent = new JSONArray(stuData).getJSONObject(0);
            if (!jsonStudent.getString("ExternalId").equals(externalId)) {
                throw new NoStudentFoundException("No users with student number " + externalId, externalId);
            }
        } catch (org.json.JSONException je) {
            throw new NoStudentFoundException("No users with student number " + externalId, externalId);
        }
        Student student = new Student();
        student.setStuNumber(externalId);
        if (!jsonStudent.isNull("FirstName")) {
            student.setFirstName(jsonStudent.getString("FirstName"));
        } else {
            student.setFirstName("");
        }
      
        if (!jsonStudent.isNull("LastName")) {
            student.setLastName(jsonStudent.getString("LastName"));
        } else {
            student.setLastName("");
        }
        
        student.setId(jsonStudent.getInt("Id"));
        return student;
    }

    @Override
    public List<Student> getStudents(String search) throws IOException {
        List<Student> students = new ArrayList<>();
        String query = "?s=" + search +
                       "&maxResults=100&current=Current&active=true";
        String stuData = HttpUtils.getDataFromURL(STUDENT_SEARCH_BASE + query);
        JSONArray jsonStudents = new JSONArray(stuData);
        for (int i=0; i < jsonStudents.length(); i++) {
            JSONObject jsonStudent = jsonStudents.getJSONObject(i);
            Student student = new Student();
            
            if (!jsonStudent.isNull("FirstName")) {
              student.setFirstName(jsonStudent.getString("FirstName"));
            } else {
              student.setFirstName("");
            }

            if (!jsonStudent.isNull("LastName")) {
                student.setLastName(jsonStudent.getString("LastName"));
            } else {
                student.setLastName("");
            }

            student.setId(jsonStudent.getInt("Id"));
            String stuNum = getEmptyStuNumString();
            try {
                stuNum = (String) jsonStudent.get("ExternalId");
            } catch (java.lang.ClassCastException ex) {
                System.err.println("Empty student number error. Student id: " + student.getId());
            }
            student.setStuNumber(stuNum);
            students.add(student);
        }
        return students;
    }

    @Override
    public List<Event> getEventsList() throws IOException {
        List<Event> events = new ArrayList<>();
        String venue;
        String response = HttpUtils.getDataFromURL(EVENT_API_LIST_URL, getAPIAuthHeaders()); 
        LOG.log(Level.FINER, response);
        JSONArray jsonEvents = new JSONArray(response);
        for (int i=0; i < jsonEvents.length(); i++) {
            JSONObject jsonEvent = jsonEvents.getJSONObject(i);
            String title = jsonEvent.getString("name");
            String startDate = jsonEvent.getString("start");
            String id = JSONObject.numberToString(jsonEvent.getInt("entityId"));
            Event event = new Event(title, startDate, id);
            if (!(jsonEvent.isNull("building") && jsonEvent.isNull("location"))) {
                String building = jsonEvent.getString("building");
                String location = jsonEvent.getString("location");
                venue = (location + ", " + building);
                event.setVenue(venue);
            }
            events.add(event);
        }
        return events;
    }

    @Override
    public Event getEvent(String eventKey, boolean getBookings) throws IOException {
        String response= HttpUtils.getDataFromURL(EVENT_API_URL + eventKey, getAPIAuthHeaders());
        JSONObject jsonEvent = new JSONObject(response);
        String title = jsonEvent.getString("name");
        String startDate = jsonEvent.getString("start");
        Event event = new Event(title, startDate, eventKey);
        
        String venue = "Venue TBC";
        if (!(jsonEvent.isNull("building") && jsonEvent.isNull("location"))) {
          String building = jsonEvent.getString("building");
          String location = jsonEvent.getString("location");
          venue = location + ", " + building;
        }
        else if (jsonEvent.isNull("offCampusVenue")) {
            venue = jsonEvent.getString("offCampusVenue");
        }
        event.setVenue(venue);
        startDate = this.prepareActiveDateStr(startDate);
        event.setStartDate(Utils.strToDate(startDate, ACTIVE_DATE_FORMAT));
        event.setBookingLimit(0);
        Integer bookingType = jsonEvent.getInt("bookingType");
        if (bookingType == 1) { //CH booking
            int regPeriod = Calendar.getInstance().getTimeZone().useDaylightTime() ? 120 : 60;
            event.setRegStart(Utils.subtractMins(event.getStartDate(), regPeriod));
            event.setBookingList(new ArrayList<Booking>());
            JSONObject settings = jsonEvent.getJSONObject("bookingSettings");
            if (settings.isNull("bookingLimit")) {
                event.setUnlimited(true);
            }
            else {
                event.setUnlimited(false);
                event.setBookingLimit(settings.getInt("bookingLimit"));
            }
        }
        else {
            event.setDropIn(true);
        }
        JSONArray jsonSessions = jsonEvent.getJSONArray("sessions");
        for (int i = 0; i < jsonSessions.length(); i++) {
            JSONObject jsonSession = jsonSessions.getJSONObject(i);
            Session session = new Session();
            session.setId(String.valueOf(jsonSession.getInt("id")));
            String startStr = this.prepareActiveDateStr(jsonSession.getString("start"));
            String endStr = this.prepareActiveDateStr(jsonSession.getString("end"));
            session.setStart(Utils.strToDate(startStr, ACTIVE_DATE_FORMAT));
            session.setEnd(Utils.strToDate(endStr, ACTIVE_DATE_FORMAT));
            session.setBookingCount(jsonSession.getInt("bookings"));
            if (bookingType == 1 && getBookings) {
                List bookings = this.getBookingList(eventKey, session.getId());
                event.getBookingList().addAll(bookings);
            }
            event.getSessions().add(session);
        }
        JSONObject attendance = jsonEvent.getJSONObject("attendance");
        event.setAttendeeCount(attendance.getInt("attended"));
        event.setBookingCount(attendance.getInt("total"));
        event.setUnspecifiedCount(attendance.getInt("unspecified"));
        return event;
    }
    
    private Map<String,String> getAPIAuthHeaders() throws IOException {
        Map<String,String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + this.getAPIToken("Integrations.Events"));
        return requestHeaders;
    }

    private String getVerificationToken() throws IOException {
        String token;
        Document doc = Jsoup.connect(LOGIN_URL).timeout(0).get();
        Element tokenInput = doc.select("input[name=__RequestVerificationToken]").get(0);
        token = tokenInput.val();
        return token;
    }

    @Override
    public String getAdminEventURL(String eventKey) {
        return EVENT_ADMIN_URL_BASE + eventKey;
    }

    @Override
    public String getCharset() {
        return charset;
    }

    @Override
    public String getEmptyStuNumString() {
        return "";
    }

    @Override
    public int getATTENDED_STATUS() {
        return ATTENDED_STATUS;
    }

    @Override
    public int getEVENT_FULL_STATUS() {
        return EVENT_FULL_STATUS;
    }
    
    @Override
    public int getUNSPECIFIED_STATUS() {
        return UNSPECIFIED_STATUS;
    }

    @Override
    public int getABSENT_STATUS() {
        return ABSENT_STATUS;
    }
    
    @Override
    public int getNOT_BOOKED_STATUS() {
        return NOT_BOOKED_STATUS;
    }

    @Override
    public boolean isValidStuNum(String stuNum) {
        return stuNum.matches(STU_NUM_PATTERN);
    }
    
    @Override
    public String getDateFormat() {
        return ACTIVE_DATE_FORMAT;
    }
    
    private boolean isAlreadyLoggedIn() {
        CookieManager manager = (CookieManager)CookieHandler.getDefault();
        CookieStore cookieJar =  manager.getCookieStore();
        List <HttpCookie> cookies = cookieJar.getCookies();
        for(HttpCookie cookie : cookies) {
            if(cookie.getName().equals(AUTH_COOKIE_NAME)) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidId(String id) {
        return Utils.isNumeric(id);
    }

    public final int ATTENDED_STATUS = 1;
    private final int UNSPECIFIED_STATUS = 0;
    public final int EVENT_FULL_STATUS = -1;
    private final int ABSENT_STATUS = 2;
    private final int NOT_BOOKED_STATUS = -2;

    private String prepareActiveDateStr(String str) {
        return str.replaceAll(":(\\d\\d)$", "$1");
    }

    private String HOST;
    private String API_ID;
    private String SECRET;
    private String STU_NUM_PATTERN;

    public String ADMIN_URL;

    public String LOGIN_URL;
    public String QUERY_URL_TEMPL;
    public String BOOKING_URL;
    public String MARK_ATTENDED_URL;
    public String MARK_UNSPECIFIED_URL;
    public String MARK_ABSENT_URL;
    public String WAITING_LIST_BASE;
    public String STUDENT_SEARCH_BASE;
    public String EVENT_API_URL;
    public String EVENT_API_LIST_URL;
    public String EVENT_BOOKING_URL_TEMPL;
    public String EVENT_ADMIN_URL_BASE;

    private final String ACTIVE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    private final String charset = "UTF-8";
    private final String AUTH_COOKIE_NAME = ".CHAUTH";
    
    private static final Logger LOG = Logger.getLogger(CareerHubAPI.class.getName());

    private final Map<String,AccessToken> tokens = new HashMap<>();
    private static BookingSystemAPI instance = null;

}