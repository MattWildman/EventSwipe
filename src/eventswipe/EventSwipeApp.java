package eventswipe;

import eventswipe.APIs.BookingSystemAPI;
import eventswipe.APIs.BookingSystemAPI.STATUS;
import eventswipe.APIs.CareerHubAPI;
import eventswipe.exceptions.EventFullException;
import eventswipe.utils.Utils;
import eventswipe.utils.EventSwipeLogger;
import eventswipe.utils.HttpUtils;
import eventswipe.models.Event;
import eventswipe.models.Booking;
import eventswipe.models.Student;
import eventswipe.utils.Request;
import eventswipe.utils.Response;
import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EventObject;
import java.util.List;
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
    @Override public void startup() {
        data = new EventSwipeData();
        logger = new EventSwipeLogger();
        try {
            api = CareerHubAPI.getInstance();
        } catch (IOException ex) {
            Logger.getLogger(EventSwipeApp.class.getName()).log(Level.SEVERE, null, ex);
            //TODO: handle error
            System.exit(0);
        }
        executor = Executors.newFixedThreadPool(EventSwipeData.MAX_ENTRY_SLOTS);
        HttpUtils.setCookiePolicy();
        if (Utils.isInternetReachable()) {
            data.setNetFlag(true);
        }
        show(new EventSwipeView(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override
    protected void configureWindow(java.awt.Window root) {
        this.addExitListener(new org.jdesktop.application.Application.ExitListener() {
            public boolean canExit(EventObject arg0) {
                return data.getSavedFlag();
            }
            public void willExit(EventObject arg0) {
                System.exit(0);
            }
        });
        root.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (e.getID() == WindowEvent.WINDOW_CLOSING && !data.getSavedFlag()) {
                    int exit = JOptionPane.showConfirmDialog(EventSwipeApp.getApplication().getMainFrame(),
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
                    exit();
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

    public EventSwipeData getData() {
        return data;
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
                            String studId = "";
                            studId = api.getStudent(stuNumberFin).getId().toString();
                            Integer newId = 0;
                            newId = api.bookStudent(studId, eventId).getBookingId();
                            bookingFin.setEntrySlot(freeSlot);
                            bookingFin.setBookingId(newId);
                            recordAttendance(bookingFin);
                        }
                        else {
                            freeEvent.getUnsavedList().add(stuNumberFin);
                            data.setSavedFlag(false);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(EventSwipeApp.class.getName())
                              .log(Level.SEVERE, "Student not booked", ex);
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
                    public void run() {
                        String bookingId = bookingFin.getBookingId().toString();
                        try {
                            api.markStatus(STATUS.ATTENDED, bookingId, event.getId());
                        } catch (Exception ex) {
                            Logger.getLogger(EventSwipeApp.class.getName()).log(Level.SEVERE, null, ex);
                            event.getUnsavedList().add(bookingFin.getStuNumber());
                            data.setSavedFlag(false);
                        }
                    }
                });
                }
            else {
                booking.setStatus(Booking.EARLY_STATUS);
            }
        }
        else {
            event.getUnsavedList().add(booking.getStuNumber());
            data.getAllRecordedList().add(booking.getStuNumber());
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
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            fw.write(content);
            fw.close();
        } catch (Exception e) {
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
            } catch (Exception e) {
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
            } catch (Exception e) {
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

    public boolean logIn(String username, char[] password) throws MalformedURLException, IOException {
        boolean success = api.logIn(username, password);
        Arrays.fill(password, '0');
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
        List<Student> waitingList = new ArrayList<Student>();
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

    public void setEvents(List<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            File file = new File(paths.get(i));
            List<String> numberList = Utils.readAllLines(file, Utils.getEncoding(file));
            List<Booking> bookingList = new ArrayList<Booking>();
            for (String number : numberList) {
                Booking booking = new Booking(number);
                bookingList.add(booking);
            }
            Event event = new Event();
            event.setBookingList(bookingList);
            event.setSlot(i+1);
            data.addEvent(event);
        }
    }

    public Event loadEvent(String eventKey, int slot, Boolean useWaitingList) throws MalformedURLException, IOException {
        Future<Response> response = executor.submit(new Request() {
            //TODO: get event asyncly
        });
        
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
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
            writeToFile(saveFile, body);
            Desktop dk = Desktop.getDesktop();
            try {
                dk.open(saveFile);
            } catch (Exception e) {
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

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(EventSwipeApp.class, args);
    }

    private void bookUnsavedRecords() {
        data.setSavedFlag(true);
        for (Event event : data.getEvents()) {
            List<String> saveErrors = new ArrayList<String>();
            for (int i = 0; i < event.getUnsavedList().size(); i++) {
                String stuNum = event.getUnsavedList().get(i);
                try {
                    Booking booking = getBooking(stuNum);
                    booking.setEntrySlot(event.getSlot());
                    recordAttendance(booking);
                    if (booking.getStatus() == Booking.EARLY_STATUS) {
                        saveErrors.add(stuNum);
                    }
                } catch (EventFullException ef) {
                    for (int j = i; j < event.getUnsavedList().size(); j++) {
                        saveErrors.add(event.getUnsavedList().get(j));
                    }
                    event.setUnsavedList(saveErrors);
                } catch (Exception ex) {
                    Logger.getLogger(EventSwipeApp.class.getName()).log(Level.SEVERE, null, ex);
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

    private ExecutorService executor;
    private EventSwipeLogger logger;
    private EventSwipeData data;
    private BookingSystemAPI api;

    private int loadEventsLock = 0;

}
