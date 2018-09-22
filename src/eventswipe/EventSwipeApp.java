package eventswipe;

import eventswipe.APIs.*;
import eventswipe.APIs.BookingSystemAPI.STATUS;
import eventswipe.exceptions.*;
import eventswipe.utils.*;
import eventswipe.models.*;
import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class EventSwipeApp extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override
    public void startup() {
        if (data.isPropertiesFlag()) {
            try {
                Properties p = getProperties(EventSwipeData.API_PROPERITES_PATH);

                @SuppressWarnings({"unchecked", "rawtypes"})
                Map<String, String> pMap = new HashMap(p);
                
                data.setCustomProperties(pMap);
                data.setDefaultUsername(p.getProperty(EventSwipeData.USERNAME_KEY, ""));
                data.setDefaultPassword(p.getProperty(EventSwipeData.PASSWORD_KEY, "").toCharArray());
                api.init();
            } catch (IOException ex) {
                data.setPropertiesFlag(false);
                Logger.getLogger(EventSwipeApp.class.getName()).log(Level.SEVERE, null, ex);
                logger.logException(ex);
            }
        }
        show(new EventSwipeView(this));
    }

    @Override
    protected void configureWindow(final java.awt.Window root) {
        this.addExitListener(new org.jdesktop.application.Application.ExitListener() {
            @Override
            public boolean canExit(EventObject arg0) {
                return data.getSavedFlag();
            }
            @Override
            public void willExit(EventObject arg0) {
                root.dispose();
            }
        });
        root.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (e.getID() == WindowEvent.WINDOW_CLOSING && !(data.getSavedFlag() ||
                    e.getWindow().getClass().getCanonicalName().contains("AboutBox"))) {
                    int exit = JOptionPane.showConfirmDialog(
                         EventSwipeApp.getApplication().getMainFrame(),
                         "You have recorded unsaved records. " +
                         "Are you sure you want to exit?",
                         "Exit warning",
                         JOptionPane.YES_NO_OPTION);
                    if (exit == JOptionPane.YES_OPTION) {
                        data.setSavedFlag(true);
                    }
                    else {
                        data.setSavedFlag(false);
                    }
                }
                else {
                    e.getWindow().dispose();
                }
            }           
        });
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of EventSwipeApp
     */
    public static EventSwipeApp getApplication() {
        return Application.getInstance(EventSwipeApp.class);
    }

    public EventSwipeApp() {
        data = EventSwipeData.getInstance();
        logger = EventSwipeLogger.getInstance();
        api = CareerHubAPI.getInstance();
        executor = Executors.newFixedThreadPool(EventSwipeData.MAX_ENTRY_SLOTS);
        HttpUtils.setCookiePolicy();
        data.setNetFlag(Utils.isInternetReachable());
        data.setPropertiesFlag(propertiesSet());
    }

    /**
     * Returns the Properties object for the specific booking system installation.
     *
     * @param   path to the properties file
     * @return  Booking system Properties object
     * @throws  IOException
     * @see     Properties
     */
    public Properties getProperties(String path) throws IOException {
        Properties p = new Properties();
        FileInputStream in = new FileInputStream(path);
        p.load(in);
        return p;
    }

    public void saveProperties(Map<String, String> props) throws NoPropertiesException {
        Properties p = new Properties();
        for (Map.Entry<String, String> prop : props.entrySet()) {
            p.setProperty(prop.getKey(), prop.getValue());
        }
        data.setDefaultUsername(p.getProperty(EventSwipeData.USERNAME_KEY, ""));
        data.setDefaultPassword(p.getProperty(EventSwipeData.PASSWORD_KEY, "").toCharArray());
        File propFile = new File(EventSwipeData.API_PROPERITES_PATH);
        if (propFile.exists()) {
            propFile.delete();
        }
        try {
            propFile.createNewFile();
            FileOutputStream out = new FileOutputStream(propFile);
            p.store(out, "Booking system properties and customisation");
            data.setCustomProperties(props);
            data.setPropertiesFlag(true);
            api.init();
        } catch (IOException ex) {
            data.setPropertiesFlag(false);
            Logger.getLogger(EventSwipeApp.class.getName())
                .log(Level.SEVERE, "Error setting properties", ex);
            throw new NoPropertiesException();
        }
    }

    public void clearProperties() {
        data.setDefaultUsername("");
        data.setDefaultPassword(null);
        saveProperties(EventSwipeData.DEFAULT_PROPS);
        data.setPropertiesFlag(false);
    }

    public EventSwipeData getData() {
        return data;
    }

    public EventSwipeLogger getLogger() {
        return logger;
    }

    public void setBookingFlag(boolean selected) {
        data.setBookingFlag(selected);
    }

    public void setWaitingListFlag(boolean selected) {
        data.setWaitingListFlag(selected);
    }

    public boolean getBookingFlag() {
        return data.isCheckingBookingLists();
    }

    public boolean getWaitingListFlag() {
        return data.isWaitingListFlag();
    }

    public void setSlots(int slots) {
        data.setSlots(slots);
    }

    public int getSlots() {
        return data.getSlots();
    }

    public void setEventTitle(String title) {
        data.setEventTitle(title);
    }

    public void setOnlineModeFlag(boolean flag) {
        data.setOnlineMode(flag);
    }

    public boolean isOnlineMode() {
        return data.isOnlineMode();
    }

    public boolean isSingleSlot() {
        return data.isSingleSlot();
    }

    public boolean isValidId(String id) {
        return api.isValidStuNum(id);
    }

    private Booking getBooking(String stuNumber) throws MalformedURLException, IOException {
        for (Event event : data.getEvents()) {
            for (Booking booking : event.getBookingList()) {
                if (booking.getStuNumber().equals(stuNumber)) {
                    booking.setEntrySlot(event.getSlot());
                    return booking;
                }
            }
        }
        Booking newBooking = new Booking(stuNumber);
        bookStudent(stuNumber, newBooking);
        newBooking.setEntrySlot(0);
        return newBooking;
    }

    public Booking checkBooking(String stuNumber) throws MalformedURLException, IOException {
        Booking bookingResult = new Booking(stuNumber);
        int slot = 0;
        boolean booked = true;
        boolean waitingList = false;
        boolean alreadyRecorded = false;
        if (data.getAllRecordedList().contains(stuNumber)) {
            alreadyRecorded = true;
        }
        else if(data.isCheckingBookingLists()) {
            booked = false;
            for (Event event : data.getEvents()) {
                for (Booking booking : event.getBookingList()) {
                    if (booking.getStuNumber().equals(stuNumber)) {
                        booked = true;
                        slot = event.getSlot();
                        bookingResult = booking;
                        break;
                    }
                }
            }
            if (!booked && data.isWaitingListFlag()) {
                for (Event event : data.getEvents()) {
                    if (!event.getWaitingList().isEmpty()) {
                        for (Student student : event.getWaitingList()) {
                            try {
                                if (student.getStuNumber().equals(stuNumber)) {
                                    waitingList = true;
                                    slot = event.getSlot();
                                }
                            } catch (NullPointerException np) {
                                System.err.println("Waiting list student " +
                                   student.getId() + " has no student number");
                            }
                        }
                    }
                }
            }
        }
        else if (data.isOnlineMode()) {
            bookingResult = getBooking(stuNumber);
            slot = bookingResult.getEntrySlot();
        }
        bookingResult.setBooked(booked);
        slot = (slot == 0) ? 1 : slot;
        bookingResult.setEntrySlot(slot);
        bookingResult.setAlreadyRecorded(alreadyRecorded);
        bookingResult.setOnWaitingList(waitingList);
        if(booked && !alreadyRecorded) {
            recordAttendance(bookingResult);
        }
        return bookingResult;
    }

    public void bookStudent(String stuNumber, Booking booking) throws MalformedURLException, IOException {
        if (data.isOnlineMode()) {
            final String stuNumberFin = stuNumber;
            final Booking bookingFin = booking;
            Future<?> response = executor.submit(new Runnable() {
                @Override
                public void run() {
                    Event freeEvent = data.getEvents().get(0);
                    try {
                        int freeSlot = 0;
                        for (Event event : data.getEvents()) {
                            int slot = event.getSlot();
                            event = api.getEvent(event.getId());
                            List<Booking> bookings = api.getBookingList(event.getId());
                            event.setSlot(slot);
                            event.setBookingList(bookings);
                            if (event.isUnlimited() || (event.getBookingLimit() > event.getBookingList().size())) {
                                freeSlot = freeSlot == 0 ? event.getSlot() : freeSlot;
                            }
                        }
                        if (freeSlot > 0) {
                            freeEvent = data.getEvents().get(freeSlot - 1);
                            String eventId = freeEvent.getId();
                            String studId = api.getStudent(stuNumberFin).getId().toString();
                            Integer newId = api.bookStudent(studId, eventId).getBookingId();
                            bookingFin.setEntrySlot(freeSlot);
                            bookingFin.setBookingId(newId);
                            recordAttendance(bookingFin);
                        }
                        else {
                            freeEvent.getUnsavedList().add(stuNumberFin);
                            data.setSavedFlag(false);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(EventSwipeApp.class.getName())
                              .log(Level.SEVERE, "Student not booked", ex);
                        logger.logException(ex);
                        freeEvent.getUnsavedList().add(stuNumberFin);
                        data.setSavedFlag(false);
                    }
                }
            });
        }
        else {
            booking.setEntrySlot(1);
            data.getEvents().get(0).getUnsavedList().add(stuNumber);
            data.setSavedFlag(false);
        }
    }

    public void recordAttendance(Booking booking) throws MalformedURLException, IOException {
        final Event event = data.getEvents().get(booking.getEntrySlot() - 1);
        final Booking bookingFin = booking;
        if (data.isOnlineMode()) {
            Date now = new Date();
            if (now.after(event.getRegStart())) {
                Future<?> response = executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        String bookingId = bookingFin.getBookingId().toString();
                        try {
                            api.markStatus(STATUS.ATTENDED, bookingId, event.getId());
                        } catch (IOException ex) {
                            Logger.getLogger(EventSwipeApp.class.getName()).log(Level.SEVERE, null, ex);
                            logger.logException(ex);
                            event.getUnsavedList().add(bookingFin.getStuNumber());
                            data.setSavedFlag(false);
                        }
                    }
                });
                }
            else {
                booking.setStatus(Booking.EARLY_STATUS);
                event.getUnsavedList().add(booking.getStuNumber());
                data.setSavedFlag(false);
            }
        }
        else {
            event.getUnsavedList().add(booking.getStuNumber());
            data.setSavedFlag(false);
        }
        data.getAllRecordedList().add(booking.getStuNumber());
    }

    public String incrementLocalAttendeeCount() {
        Integer a = data.incrementAttendeesCount();
        return a.toString();
    }
    
    public String getLocalAttendeeCount() {
        Integer a = data.getLocalAttendeeCount();
        return a.toString();
    }

    public String getAttendeeCount() throws MalformedURLException, IOException {
        Integer a = 0;
        for (Event event : data.getEvents()) {
            a += api.getAttendeeCount(event.getId());
        }
        return a.toString();
    }

    public void writeToFile(File file, String content) {
        try {
            try (FileWriter fw = new FileWriter(file.getAbsoluteFile(), true)) {
                fw.write(content);
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    @Action
    public void saveAttendeesToFile() {
        String header = "Event attendees";
	header += " - " + Utils.getDate("dd/MM/yyyy HH:mm:ss")
                        + System.getProperty("line.separator");
        FileDialog fDialog = new FileDialog(this.getMainFrame(), 
                        "Save attendees list", FileDialog.SAVE);
        fDialog.setVisible(true);
        String path = fDialog.getDirectory() + fDialog.getFile();
        if (!path.equals("nullnull")) {
            if (!path.endsWith(".txt")) {
                path += ".txt";
            }
            File saveFile = new File(path);
            try {
                saveFile.createNewFile();
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
            writeToFile(saveFile, header);
            for (Event event : data.getEvents()) {
                writeToFile(saveFile, event.getTitle() + System.getProperty("line.separator"));
                for (String stuNum : event.getUnsavedList()) {
                    writeToFile(saveFile, stuNum + System.getProperty("line.separator"));
                }
            }
            data.setSavedFlag(true);
            Desktop dk = Desktop.getDesktop();
            try {
                dk.open(saveFile);
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    public void createLog() {
        logger.createLog(data.getEventTitle());
    }

    public void log(String message) {
        logger.log(message);
    }

    public void clearData() {
        data.clearData();
    }

    public boolean logIn(String username, char[] password) 
           throws MalformedURLException, IOException, NoPropertiesException {
        boolean success = false;
        if (data.isPropertiesFlag()) {
            success = api.logIn(username, password);
            Arrays.fill(password, '0');
        }
        else {
            Arrays.fill(password, '0');
            throw new NoPropertiesException();
        }
        return success;
    }

    public Event getEvent(String key) throws MalformedURLException, IOException {
        return api.getEvent(key);
    }

    public List<Event> getEvents(String term) throws MalformedURLException, IOException {
        return api.getEventsList();
    }

    public String getAdminEventURL(String eventKey) throws IOException {
        return api.getAdminEventURL(eventKey);
    }

    public void createWaitingList(String path) {
        File file = new File(path);
        List<String> numberList = Utils.readAllLines(file, Utils.getEncoding(file));
        List<Student> waitingList = new ArrayList<>();
        for (String number : numberList) {
            Student student = new Student();
            student.setStuNumber(number);
            waitingList.add(student);
        }
        for (Event event : data.getEvents()) {
            event.setWaitingList(waitingList);
        }
    }

    public Booking processSearchInput(String input) throws MalformedURLException, IOException {
        return checkBooking(input);
    }

    public List<Student> getStudents(String input) throws MalformedURLException, IOException {
        return api.getStudents(input);
    }

    public void addEvent(Event event) {
        data.addEvent(event);
    }

    public void setEventsOffline(List<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            File file = new File(paths.get(i));
            List<String> numberList = Utils.readAllLines(file, Utils.getEncoding(file));
            List<Booking> bookingList = new ArrayList<>();
            for (String number : numberList) {
                Booking booking = new Booking(number);
                bookingList.add(booking);
            }
            Event event = new Event();
            event.setBookingList(bookingList);
            event.setSlot(i+1);
            event.setUnlimited(true);
            data.addEvent(event);
        }
    }

    public Event loadEvent(String eventKey, int slot, Boolean useWaitingList) throws MalformedURLException, IOException {
//        Future<Response> response = executor.submit(new Request() {
//            TODO: get event asyncly
//        }); 
        Event event = api.getEvent(eventKey);
        event.setSlot(slot);
        event.setBookingList(api.getBookingList(eventKey));
        if(useWaitingList) {
            List<Student> waitingList = api.getWaitingList(eventKey);
            event.setWaitingList(waitingList);
            if (!waitingList.isEmpty()) {
                data.setWaitingListFlag(true);
            }
        }
        data.addEvent(event);
        data.setSingleSlot(slot == 1);
        return event;
    }

    public void goToOnlineMode() throws IOException {
        if (Utils.isInternetReachable()) {
            setOnlineModeFlag(true);
            try {
                bookUnsavedRecords();
            } catch (EventFullException ef) {
                logger.logException(ef);
                throw ef;
            }
        }
        else {
            throw new IOException();
        }
    }

    public int getBookedCount() {
        Integer b = 0;
        for (Event event : data.getEvents()) {
            b += event.getBookingList().size();
        }
        return b;
    }

    public String getCharset() {
        return api.getCharset();
    }

    public String getEmptyStuNumString() {
        return api.getEmptyStuNumString();
    }

    public Integer getEventFullStatus() {
        return api.getEVENT_FULL_STATUS();
    }

    public Event getEvent(int slot) {
        return data.getEvents().get(slot);
    }

    public void addToEarlyList(String stuNumber, Integer entrySlot) {
        this.getEvent(entrySlot - 1).getUnsavedList().add(stuNumber);
        data.setSavedFlag(false);
    }

    public void saveAndFinish() {
        if (!data.getSavedFlag()) {
            this.saveAttendeesToFile();
        }
        if (data.getSavedFlag()) {
            System.exit(0);
        }
    }

    public boolean isSaved() {
        return data.getSavedFlag();
    }

    public void setLoggedInFlag(boolean b) {
        data.setLoggedInFlag(b);
    }

    public boolean isLoggedIn() {
        return data.isLoggedInFlag();
    }

    public void finish(Boolean markAbsent, Boolean notify) throws MalformedURLException, IOException {
        if (!data.getSavedFlag()) {
            this.saveAndFinish();
        }
        else if(markAbsent && data.getSavedFlag()) {
            for (Event event : data.getEvents()) {
               api.markAllUnspecifiedAbsent(event.getId(), notify);
            }
        }
        System.exit(0);
    }

    public void finishCounting() {
        String body = "Attendees recorded - "
                    + Utils.getDate("dd/MM/yyyy HH:mm:ss")
                    + System.getProperty("line.separator")
                    + System.getProperty("line.separator")
                    + data.getCount().toString();
        FileDialog fDialog = new FileDialog(this.getMainFrame(),
                        "Save attendee count", FileDialog.SAVE);
        fDialog.setVisible(true);
        String path = fDialog.getDirectory() + fDialog.getFile();
        if (!path.equals("nullnull")) {
            if (!path.endsWith(".txt")) {
                path += ".txt";
            }
            File saveFile = new File(path);
            try {
                saveFile.createNewFile();
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
            writeToFile(saveFile, body);
            Desktop dk = Desktop.getDesktop();
            try {
                dk.open(saveFile);
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        System.exit(0);
    }

    public int incrementCount() {
        return data.incrementCount();
    }

    public void resetCounter() {
        data.setCount(0);
    }

    public static void main(String[] args) {
        launch(EventSwipeApp.class, args);
    }

    private void bookUnsavedRecords() {
        data.setSavedFlag(true);
        for (Event event : data.getEvents()) {
            List<String> saveErrors = new ArrayList<>();
            for (int i = 0; i < event.getUnsavedList().size(); i++) {
                String stuNum = event.getUnsavedList().get(i);
                try {
                    Booking booking = getBooking(stuNum);
                    booking.setEntrySlot(event.getSlot());
                    recordAttendance(booking);
                    if (Objects.equals(booking.getStatus(), Booking.EARLY_STATUS)) {
                        saveErrors.add(stuNum);
                    }
                } catch (EventFullException ef) {
                    logger.logException(ef);
                    for (int j = i; j < event.getUnsavedList().size(); j++) {
                        saveErrors.add(event.getUnsavedList().get(j));
                    }
                    event.setUnsavedList(saveErrors);
                } catch (IOException ex) {
                    Logger.getLogger(EventSwipeApp.class.getName()).log(Level.SEVERE, null, ex);
                    logger.logException(ex);
                    saveErrors.add(stuNum);
                } 
            }
            if (saveErrors.isEmpty()) {
                event.getUnsavedList().clear();
            }
            else {
                event.setUnsavedList(saveErrors);
                data.setSavedFlag(false);
            }
        }
    }

    private boolean propertiesSet() {
        File props = new File(EventSwipeData.API_PROPERITES_PATH);
        if (!props.exists() || props.isDirectory()) {
            return false;
        }
        else {
            Properties p = new Properties();
            try {
                FileInputStream in;
                in = new FileInputStream(EventSwipeData.API_PROPERITES_PATH);
                p.load(in);
                if (p.getProperty(EventSwipeData.STATUS_KEY, "default").equals("default")) {
                    return false;
                }
            } catch (IOException ex) {
                Logger.getLogger(EventSwipeApp.class.getName())
                   .log(Level.SEVERE, "Error accessing properties file", ex);
                logger.logException(ex);
                return false;
            }
        }
        return true;
    }
    
    private final ExecutorService executor;
    private final EventSwipeLogger logger;
    private final EventSwipeData data;
    private final BookingSystemAPI api;

}
