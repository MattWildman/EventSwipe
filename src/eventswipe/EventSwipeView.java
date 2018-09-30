package eventswipe;

import eventswipe.exceptions.*;
import eventswipe.utils.*;
import eventswipe.models.*;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jdesktop.application.Action;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.UIManager;
import org.apache.commons.validator.routines.UrlValidator;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

/**
 * The application's main frame.
 */
public class EventSwipeView extends FrameView {

    public EventSwipeView(SingleFrameApplication appl) {
        super(appl);
        ResourceMap resourceMap = Application.getInstance(eventswipe.EventSwipeApp.class)
           .getContext().getResourceMap(EventSwipeView.class);
        titleInputDefault = resourceMap.getString("eventTitleInputDefault");
        fileInputDefault = resourceMap.getString("fileInputDefault");
        idInputDefault = resourceMap.getString("idInputDefault");
        recordingAllText = resourceMap.getString("checkingModeToggle1.text");
        checkingListsText = resourceMap.getString("checkingListsText");
        onlineModeTooltipText = resourceMap.getString("onlineModeToggler.toolTipText");
        offlineModeTooltipText = resourceMap.getString("offlineModeTooltipText");
        initComponents();
        initSlotViews();
        try {
            Image i = ImageIO.read(getClass().getResource("/eventswipe/resources/yourLogoLarge.jpeg"));
            this.getFrame().setIconImage(i);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        this.getFrame().setPreferredSize(new Dimension(750, 500));
        this.getFrame().setResizable(true);
        usernameInput.requestFocusInWindow();
        buildCounterMap();
        updateBookingPanel(true);
        updateOnlineBookingPanel(true);
        panelStack = new Stack<>();
        panelStack.push((JPanel) this.getComponent());
    }

    javax.swing.Action save = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            app.saveAttendeesToFile();
        }
    };
    javax.swing.Action toggleBooking = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            checkingModeToggle1.doClick();
        }
    };
    javax.swing.Action checkBooking = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            searchButton.doClick();
        }
    };
    javax.swing.Action toggleConnection = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            onlineModeToggle.doClick();
        }
    };
    javax.swing.Action refreshAttendees = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            refreshAttendeesButton.doClick();
        }
    };

    @Action
    public void initialFinishAction() {
        if (app.isOnlineMode()) {
            if (app.isSaved()) {
               this.switchToPanel(finishPanel); 
            }
            else {
                Object[] options = {"Save and exit", "Exit without saving"};
                int reply = JOptionPane.showOptionDialog(
                                app.getMainFrame(),
                                "There are unrecorded students. " +
                                System.getProperty("line.separator") +
                                "Please save these and then mark absentees manually.",
                                "Unsaved records",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                options,
                                options[0]
                             );
                if (reply == JOptionPane.YES_OPTION) {
                    app.saveAndFinish();
                }
                else if (reply == JOptionPane.NO_OPTION) {
                    System.exit(0);
                }
            }
        }
        else {
            app.saveAndFinish();
        }
    }

    @Action
    public void finishCounting() {
        app.finishCounting();
    }

    @Action
    public void finishAction() {
        try {
            app.finish(markAbsentOption.isSelected(), notifyAbsentOption.isSelected());
        } catch (IOException ex) {
            Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ex);
            app.getLogger().logException(ex);
            JOptionPane.showMessageDialog(
                app.getMainFrame(),
                "Error marking absentees. You'll have to do this manually.",
                "Marking absentee error",
                JOptionPane.ERROR_MESSAGE
            );
        }
        try {
            app.finish(false, false);
        } catch (IOException ex) {
            Logger.getLogger(EventSwipeApp.class.getName()).log(Level.SEVERE, null, ex);
            app.getLogger().logException(ex);
            System.exit(0);
        }
    }

    @Action
    public void goToSettings() {
        if (app.getData().isPropertiesFlag()) {
            Map<String, String> p = app.getData().getCustomProperties();
            hostInput.setText(p.get(EventSwipeData.HOST_KEY));
            apiIdInput.setText(p.get(EventSwipeData.API_ID_KEY));
            apiSecretInput.setText(p.get(EventSwipeData.API_SECRET_KEY));
            regexInput.setText(p.get(EventSwipeData.STUDENT_ID_PATTERN_KEY));
            defaultUsernameInput.setText(app.getData().getDefaultUsername());
            defaultPasswordInput.setText(String.valueOf(app.getData().getDefaultPassword()));
        }
        this.switchToPanel(settingsPanel);
    }
    
    @Action
    public void saveSettings() {
        boolean complete;
        JTextField[] required = {
            hostInput, apiIdInput, apiSecretInput, regexInput
        };
        complete = markEmptyFields(required);
        if (useDefaultLoginCheckbox.isSelected()) {
            JTextField[] requiredLoginInfo = {
                defaultUsernameInput, defaultPasswordInput
            };
            complete = markEmptyFields(requiredLoginInfo);
        }
        if (!complete) {
            JOptionPane.showMessageDialog(app.getMainFrame(),
              "Fields in red are required.",
              "Required fields missing",
              JOptionPane.ERROR_MESSAGE);
        }
        String[] schemes = {"http", "https"};
        UrlValidator validator = new UrlValidator(schemes);
        String hostStr = hostInput.getText();
        if (!validator.isValid(hostStr)) {
            complete = false;
            markErrorField(hostInput);
            JOptionPane.showMessageDialog(app.getMainFrame(),
              "Host is not a valid URL.",
              "Invalid host",
              JOptionPane.ERROR_MESSAGE);
        }
        else {
            if (!hostStr.endsWith("/")) {
                hostStr += "/";
            }
            markDefaultField(hostInput);
        }
        if (complete) {
            Map<String,String> props = new HashMap<>();
            props.put(EventSwipeData.STATUS_KEY, "custom");
            props.put(EventSwipeData.API_ID_KEY, apiIdInput.getText());
            props.put(EventSwipeData.API_SECRET_KEY, apiSecretInput.getText());
            props.put(EventSwipeData.HOST_KEY, hostStr);
            props.put(EventSwipeData.STUDENT_ID_PATTERN_KEY, regexInput.getText());
            props.put(EventSwipeData.USERNAME_KEY, defaultUsernameInput.getText());
            props.put(EventSwipeData.PASSWORD_KEY, String.valueOf(defaultPasswordInput.getPassword()));
            try {
                app.saveProperties(props);
            } catch (NoPropertiesException ex) {
                //TODO: handle properly - instructions for manually editing file
                this.showGenericErrorMessage();
            } finally {
                this.goBack();
            }
        }
    }

    @Action
    public void resetSettings() {
        apiIdInput.setText("");
        apiSecretInput.setText("");
        hostInput.setText("");
        this.setRegexValue(defaultRegexType, defaultRegexQuantifier);
        defaultUsernameInput.setText("");
        defaultPasswordInput.setText("");
        studentIdLengthSpinner.setValue(1);
        if (useDefaultLoginCheckbox.isSelected()) {
            useDefaultLoginCheckbox.doClick();
        }
        if (useCustomRegexCheckbox.isSelected()) {
            useCustomRegexCheckbox.doClick();
        }
        if (fixedLengthCheckbox.isSelected()) {
            fixedLengthCheckbox.doClick();
        }
        JTextField[] fields = {
            hostInput, apiIdInput, apiSecretInput, regexInput,
            defaultUsernameInput, defaultPasswordInput
        };
        for (JTextField field : fields) {
            this.markDefaultField(field);
        }
        app.clearProperties();
        hostInput.requestFocusInWindow();
    }

    @Action
    public void goBack() {
        JPanel panel = panelStack.pop();
        JFrame mainFrame = app.getMainFrame();
        mainFrame.setContentPane(panel);
        if (panel.equals(preConfigPanel)) {
            usernameInput.setText(app.getData().getDefaultUsername());
            passwordInput.setText(String.valueOf(app.getData().getDefaultPassword()));
        }
        panel.revalidate();
        mainFrame.repaint();
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = app.getMainFrame();
            aboutBox = new EventSwipeAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        app.show(aboutBox);
    }

    @Action
    public void count() {
        this.displayCount((Integer)app.incrementCount());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        settingsMenuItem = new javax.swing.JMenuItem();
        toggleMenuItem = new javax.swing.JMenuItem();
        connectionMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        configPanel = new javax.swing.JPanel();
        okConfigButton = new javax.swing.JButton();
        titleLabel = new javax.swing.JLabel();
        bookingDetailsPanel = new javax.swing.JPanel();
        entrySlot1Panel = new javax.swing.JPanel();
        bookingListBrowseButton1 = new javax.swing.JButton();
        entrySlotBookingListFilePathInput1 = new javax.swing.JFormattedTextField();
        entrySlotBookingListLabel1 = new javax.swing.JLabel();
        entrySlot2Panel = new javax.swing.JPanel();
        entrySlotBookingListFilePathInput2 = new javax.swing.JFormattedTextField();
        entrySlotBookingListLabel2 = new javax.swing.JLabel();
        bookingListBrowseButton2 = new javax.swing.JButton();
        entrySlot4Panel = new javax.swing.JPanel();
        entrySlotBookingListLabel4 = new javax.swing.JLabel();
        entrySlotBookingListFilePathInput4 = new javax.swing.JFormattedTextField();
        bookingListBrowseButton4 = new javax.swing.JButton();
        entrySlot5Panel = new javax.swing.JPanel();
        entrySlotBookingListFilePathInput5 = new javax.swing.JFormattedTextField();
        bookingListBrowseButton5 = new javax.swing.JButton();
        entrySlotBookingListLabel5 = new javax.swing.JLabel();
        waitingListFilePanel = new javax.swing.JPanel();
        waitingListFileLabel = new javax.swing.JLabel();
        waitingListBrowseButton = new javax.swing.JButton();
        waitingListFilePathInput = new javax.swing.JFormattedTextField();
        entrySlot3Panel = new javax.swing.JPanel();
        bookingListBrowseButton3 = new javax.swing.JButton();
        entrySlotBookingListLabel3 = new javax.swing.JLabel();
        entrySlotBookingListFilePathInput3 = new javax.swing.JFormattedTextField();
        slotsPanel = new javax.swing.JPanel();
        entrySlotsSpinnerOffline = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(1, 1, EventSwipeData.MAX_ENTRY_SLOTS, 1));
        entrySlotsLabel = new javax.swing.JLabel();
        waitingListRadioPanel = new javax.swing.JPanel();
        noWaitingListRadioButton = new javax.swing.JRadioButton();
        yesWaitingListRadioButton = new javax.swing.JRadioButton();
        waitingListLabel = new javax.swing.JLabel();
        configBackButton = new javax.swing.JButton();
        requireBookingPanel = new javax.swing.JPanel();
        yesBookingRadioButton = new javax.swing.JRadioButton();
        noBookingRadioButton = new javax.swing.JRadioButton();
        requireBookingLabel = new javax.swing.JLabel();
        eventTitlePanel = new javax.swing.JPanel();
        eventTitleInputLabel = new javax.swing.JLabel();
        eventTitleInput = new javax.swing.JFormattedTextField();
        requireBookingButtonGroup = new javax.swing.ButtonGroup();
        waitingListButtonGroup = new javax.swing.ButtonGroup();
        preConfigPanel = new javax.swing.JPanel();
        titlePanel = new javax.swing.JPanel();
        smallLogoLabel = new javax.swing.JLabel();
        versionLabel = new javax.swing.JLabel();
        mainTitle = new javax.swing.JLabel();
        titleLoginPanel = new javax.swing.JPanel();
        loginOfflineSeparator = new javax.swing.JSeparator();
        loginFormPanel = new javax.swing.JPanel();
        logInButton = new javax.swing.JButton();
        passwordLabel = new javax.swing.JLabel();
        usernameLabel = new javax.swing.JLabel();
        usernameInput = new javax.swing.JTextField();
        passwordInput = new javax.swing.JPasswordField();
        logInLabel = new javax.swing.JLabel();
        offlinePanel = new javax.swing.JPanel();
        useOfflineLabel = new javax.swing.JLabel();
        startOfflineButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        useOfflineDescription = new javax.swing.JTextArea();
        offlineCounterSeparator = new javax.swing.JSeparator();
        startCounterModePanel = new javax.swing.JPanel();
        startCounterModeButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        useOfflineDescription1 = new javax.swing.JTextArea();
        onlineConfigPanel = new javax.swing.JPanel();
        bookingDetailsPanel1 = new javax.swing.JPanel();
        entrySlotsLabel1 = new javax.swing.JLabel();
        entrySlotsSpinnerOnline = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(1, 1, EventSwipeData.MAX_ENTRY_SLOTS, 1));
        slot1DetailsPanel = new javax.swing.JPanel();
        generatedTitleLabel1 = new javax.swing.JLabel();
        searchEventsButton1 = new javax.swing.JButton();
        entrySlotIdLabel1 = new javax.swing.JLabel();
        generatedTitle1 = new javax.swing.JTextField();
        loadEventButton1 = new javax.swing.JButton();
        entrySlotIdInput1 = new javax.swing.JFormattedTextField();
        slot1Label = new javax.swing.JLabel();
        slot2DetailsPanel = new javax.swing.JPanel();
        generatedTitle2 = new javax.swing.JTextField();
        entrySlotIdLabel2 = new javax.swing.JLabel();
        slot2Label = new javax.swing.JLabel();
        entrySlotIdInput2 = new javax.swing.JFormattedTextField();
        loadEventButton2 = new javax.swing.JButton();
        searchEventsButton2 = new javax.swing.JButton();
        generatedTitleLabel2 = new javax.swing.JLabel();
        slot3DtailsPanel = new javax.swing.JPanel();
        generatedTitleLabel3 = new javax.swing.JLabel();
        searchEventsButton3 = new javax.swing.JButton();
        generatedTitle3 = new javax.swing.JTextField();
        slot3Label = new javax.swing.JLabel();
        entrySlotIdLabel3 = new javax.swing.JLabel();
        loadEventButton3 = new javax.swing.JButton();
        entrySlotIdInput3 = new javax.swing.JFormattedTextField();
        slot4DetailsPanel = new javax.swing.JPanel();
        searchEventsButton4 = new javax.swing.JButton();
        slot4Label = new javax.swing.JLabel();
        loadEventButton4 = new javax.swing.JButton();
        generatedTitleLabel4 = new javax.swing.JLabel();
        entrySlotIdInput4 = new javax.swing.JFormattedTextField();
        generatedTitle4 = new javax.swing.JTextField();
        entrySlotIdLabel4 = new javax.swing.JLabel();
        slot5DetailsPanel = new javax.swing.JPanel();
        entrySlotIdLabel5 = new javax.swing.JLabel();
        entrySlotIdInput5 = new javax.swing.JFormattedTextField();
        loadEventButton5 = new javax.swing.JButton();
        generatedTitle5 = new javax.swing.JTextField();
        generatedTitleLabel5 = new javax.swing.JLabel();
        searchEventsButton5 = new javax.swing.JButton();
        slot5Label = new javax.swing.JLabel();
        waitingListOptionsPanel = new javax.swing.JPanel();
        loadWaitingListLabel = new javax.swing.JLabel();
        noLoadWaitingListRadioButton = new javax.swing.JRadioButton();
        yesLoadWaitingListRadioButton = new javax.swing.JRadioButton();
        onlineConfigBackButton = new javax.swing.JButton();
        aboutEventLabel = new javax.swing.JLabel();
        okConfigButton1 = new javax.swing.JButton();
        mainOnlinePanel = new javax.swing.JPanel();
        searchInput = new javax.swing.JFormattedTextField();
        searchButton = new javax.swing.JButton();
        bookingStatusScrollPane1 = new javax.swing.JScrollPane();
        bookingStatusTextArea1 = new javax.swing.JTextArea();
        backButton1 = new javax.swing.JButton();
        finishButton = new javax.swing.JButton();
        searchInputLabel = new javax.swing.JLabel();
        checkingModeToggle1 = new javax.swing.JToggleButton();
        onlineModeToggle = new javax.swing.JToggleButton();
        bookingStatusPanel = new javax.swing.JPanel();
        totalLabel = new javax.swing.JLabel();
        entrySlotDisplayTextField1 = new javax.swing.JFormattedTextField();
        attendeeCountLabel1 = new javax.swing.JLabel();
        statusDisplayTextField1 = new javax.swing.JFormattedTextField();
        attendeeCountLabel2 = new javax.swing.JLabel();
        localAttendeeCountTextField = new javax.swing.JFormattedTextField();
        statusLabel1 = new javax.swing.JLabel();
        entrySlotLabel1 = new javax.swing.JLabel();
        thisMachineLabel = new javax.swing.JLabel();
        totalAttendeeCountDisplay = new javax.swing.JFormattedTextField();
        refreshAttendeesButton = new javax.swing.JButton();
        historyLabel = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        eventStatusPanel = new javax.swing.JPanel();
        eventStatusLabel = new javax.swing.JLabel();
        eventStatusBookedLabel = new javax.swing.JLabel();
        eventStatusAttendedLabel = new javax.swing.JLabel();
        eventStatusSavedLabel = new javax.swing.JLabel();
        eventStatusUnsavedLabel = new javax.swing.JLabel();
        eventStatusNotAttendedLabel = new javax.swing.JLabel();
        eventStatusNotAttendedLabel1 = new javax.swing.JLabel();
        eventStatusBookedNumber = new javax.swing.JTextField();
        eventStatusAttendedNumber = new javax.swing.JTextField();
        eventStatusSavedNumber = new javax.swing.JTextField();
        eventStatusUnsavedNumber = new javax.swing.JTextField();
        eventStatusNotAttendedNumber = new javax.swing.JTextField();
        eventStatusPlacesNumber = new javax.swing.JTextField();
        finishPanel = new javax.swing.JPanel();
        finishCloseButton = new javax.swing.JButton();
        finishBackButton = new javax.swing.JButton();
        finishPanelTitle = new javax.swing.JLabel();
        markAbsentOption = new javax.swing.JCheckBox();
        notifyAbsentOption = new javax.swing.JCheckBox();
        counterPanel = new javax.swing.JPanel();
        finishCountingButton = new javax.swing.JButton();
        counterBackButton = new javax.swing.JButton();
        counterPanelTitle = new javax.swing.JLabel();
        counterDisplayPanel = new javax.swing.JPanel();
        counterThousands = new javax.swing.JFormattedTextField();
        counterTens = new javax.swing.JFormattedTextField();
        counterUnits = new javax.swing.JFormattedTextField();
        counterHundreds = new javax.swing.JFormattedTextField();
        counterTenThousands = new javax.swing.JFormattedTextField();
        countButton = new javax.swing.JButton();
        resetCounterButton = new javax.swing.JButton();
        onlineWaitingListButtonGroup = new javax.swing.ButtonGroup();
        settingsPanel = new javax.swing.JPanel();
        doneSettingsButton = new javax.swing.JButton();
        settingsBackButton = new javax.swing.JButton();
        settingsPanelTitle = new javax.swing.JLabel();
        hostLabel = new javax.swing.JLabel();
        hostInput = new javax.swing.JTextField();
        apiIdLabel = new javax.swing.JLabel();
        apiIdInput = new javax.swing.JTextField();
        apiSecretLabel = new javax.swing.JLabel();
        apiSecretInput = new javax.swing.JTextField();
        studentIdFormatPanel = new javax.swing.JPanel();
        studentIdFormatTitle = new javax.swing.JLabel();
        numbersRadio = new javax.swing.JRadioButton();
        lettersRadio = new javax.swing.JRadioButton();
        lettersNumbersRadio = new javax.swing.JRadioButton();
        studentIdLengthSpinner = new javax.swing.JSpinner();
        studentIdLengthLabel = new javax.swing.JLabel();
        useCustomRegexCheckbox = new javax.swing.JCheckBox();
        regexInput = new javax.swing.JTextField();
        regexLabel = new javax.swing.JLabel();
        fixedLengthCheckbox = new javax.swing.JCheckBox();
        defaultUsernameInput = new javax.swing.JTextField();
        defaultUsernameLabel = new javax.swing.JLabel();
        defaultPasswordLabel = new javax.swing.JLabel();
        defaultPasswordInput = new javax.swing.JPasswordField();
        useDefaultLoginCheckbox = new javax.swing.JCheckBox();
        resetSettingsButton = new javax.swing.JButton();
        studentIdFormatTypeButtonGroup = new javax.swing.ButtonGroup();

        menuBar.setName("menuBar"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(eventswipe.EventSwipeApp.class).getContext().getResourceMap(EventSwipeView.class);
        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(eventswipe.EventSwipeApp.class).getContext().getActionMap(EventSwipeView.class, this);
        settingsMenuItem.setAction(actionMap.get("goToSettings")); // NOI18N
        settingsMenuItem.setIcon(resourceMap.getIcon("settingsMenuItem.icon")); // NOI18N
        settingsMenuItem.setText(resourceMap.getString("settingsMenuItem.text")); // NOI18N
        settingsMenuItem.setName("settingsMenuItem"); // NOI18N
        fileMenu.add(settingsMenuItem);

        toggleMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        toggleMenuItem.setText(resourceMap.getString("toggleMenuItem.text")); // NOI18N
        toggleMenuItem.setEnabled(false);
        toggleMenuItem.setName("toggleMenuItem"); // NOI18N
        toggleMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(toggleMenuItem);

        connectionMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_MASK));
        connectionMenuItem.setText(resourceMap.getString("connectionMenuItem.text")); // NOI18N
        connectionMenuItem.setEnabled(false);
        connectionMenuItem.setName("connectionMenuItem"); // NOI18N
        connectionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectionMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(connectionMenuItem);

        saveMenuItem.setAction(actionMap.get("saveAttendeesToFile")); // NOI18N
        saveMenuItem.setText(resourceMap.getString("saveMenuItem.text")); // NOI18N
        saveMenuItem.setName("saveMenuItem"); // NOI18N
        fileMenu.add(saveMenuItem);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        configPanel.setMinimumSize(new java.awt.Dimension(720, 350));
        configPanel.setName("configPanel"); // NOI18N
        configPanel.setPreferredSize(new java.awt.Dimension(720, 540));

        okConfigButton.setText(resourceMap.getString("okConfigButton.text")); // NOI18N
        okConfigButton.setName("okConfigButton"); // NOI18N
        okConfigButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okConfigButtonActionPerformed(evt);
            }
        });

        titleLabel.setFont(resourceMap.getFont("titleLabel.font")); // NOI18N
        titleLabel.setText(resourceMap.getString("titleLabel.text")); // NOI18N
        titleLabel.setName("titleLabel"); // NOI18N

        bookingDetailsPanel.setEnabled(false);
        bookingDetailsPanel.setMaximumSize(new java.awt.Dimension(808, 472));
        bookingDetailsPanel.setName("bookingDetailsPanel"); // NOI18N
        bookingDetailsPanel.setRequestFocusEnabled(false);

        entrySlot1Panel.setEnabled(false);
        entrySlot1Panel.setName("entrySlot1Panel"); // NOI18N
        entrySlot1Panel.setPreferredSize(new java.awt.Dimension(475, 40));

        bookingListBrowseButton1.setText(resourceMap.getString("bookingListBrowseButton1.text")); // NOI18N
        bookingListBrowseButton1.setName("bookingListBrowseButton1"); // NOI18N
        bookingListBrowseButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseFileAction(evt);
            }
        });

        entrySlotBookingListFilePathInput1.setText(fileInputDefault);
        entrySlotBookingListFilePathInput1.setName("entrySlotBookingListFilePathInput1"); // NOI18N
        entrySlotBookingListFilePathInput1.setPreferredSize(new java.awt.Dimension(200, 20));
        entrySlotBookingListFilePathInput1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                inputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                inputFocusLost(evt);
            }
        });

        entrySlotBookingListLabel1.setText(resourceMap.getString("entrySlotBookingListLabel1.text")); // NOI18N
        entrySlotBookingListLabel1.setName("entrySlotBookingListLabel1"); // NOI18N

        javax.swing.GroupLayout entrySlot1PanelLayout = new javax.swing.GroupLayout(entrySlot1Panel);
        entrySlot1Panel.setLayout(entrySlot1PanelLayout);
        entrySlot1PanelLayout.setHorizontalGroup(
            entrySlot1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(entrySlot1PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(entrySlotBookingListLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(entrySlotBookingListFilePathInput1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bookingListBrowseButton1)
                .addContainerGap())
        );
        entrySlot1PanelLayout.setVerticalGroup(
            entrySlot1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(entrySlot1PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(entrySlot1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(entrySlotBookingListLabel1)
                    .addComponent(entrySlotBookingListFilePathInput1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bookingListBrowseButton1))
                .addContainerGap())
        );

        entrySlot2Panel.setName("entrySlot2Panel"); // NOI18N
        entrySlot2Panel.setPreferredSize(new java.awt.Dimension(475, 40));

        entrySlotBookingListFilePathInput2.setText(fileInputDefault);
        entrySlotBookingListFilePathInput2.setName("entrySlotBookingListFilePathInput2"); // NOI18N
        entrySlotBookingListFilePathInput2.setPreferredSize(new java.awt.Dimension(200, 20));
        entrySlotBookingListFilePathInput2.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                inputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                inputFocusLost(evt);
            }
        });

        entrySlotBookingListLabel2.setText(resourceMap.getString("entrySlotBookingListLabel2.text")); // NOI18N
        entrySlotBookingListLabel2.setName("entrySlotBookingListLabel2"); // NOI18N

        bookingListBrowseButton2.setText(resourceMap.getString("bookingListBrowseButton2.text")); // NOI18N
        bookingListBrowseButton2.setName("bookingListBrowseButton2"); // NOI18N
        bookingListBrowseButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseFileAction(evt);
            }
        });

        javax.swing.GroupLayout entrySlot2PanelLayout = new javax.swing.GroupLayout(entrySlot2Panel);
        entrySlot2Panel.setLayout(entrySlot2PanelLayout);
        entrySlot2PanelLayout.setHorizontalGroup(
            entrySlot2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(entrySlot2PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(entrySlotBookingListLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(entrySlotBookingListFilePathInput2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bookingListBrowseButton2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        entrySlot2PanelLayout.setVerticalGroup(
            entrySlot2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(entrySlot2PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(entrySlot2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(entrySlotBookingListLabel2)
                    .addComponent(bookingListBrowseButton2)
                    .addComponent(entrySlotBookingListFilePathInput2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        entrySlot4Panel.setName("entrySlot4Panel"); // NOI18N
        entrySlot4Panel.setPreferredSize(new java.awt.Dimension(475, 40));
        entrySlot4Panel.setRequestFocusEnabled(false);

        entrySlotBookingListLabel4.setText(resourceMap.getString("entrySlotBookingListLabel4.text")); // NOI18N
        entrySlotBookingListLabel4.setName("entrySlotBookingListLabel4"); // NOI18N

        entrySlotBookingListFilePathInput4.setText(fileInputDefault);
        entrySlotBookingListFilePathInput4.setName("entrySlotBookingListFilePathInput4"); // NOI18N
        entrySlotBookingListFilePathInput4.setPreferredSize(new java.awt.Dimension(200, 20));
        entrySlotBookingListFilePathInput4.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                inputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                inputFocusLost(evt);
            }
        });

        bookingListBrowseButton4.setText(resourceMap.getString("bookingListBrowseButton4.text")); // NOI18N
        bookingListBrowseButton4.setName("bookingListBrowseButton4"); // NOI18N
        bookingListBrowseButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseFileAction(evt);
            }
        });

        javax.swing.GroupLayout entrySlot4PanelLayout = new javax.swing.GroupLayout(entrySlot4Panel);
        entrySlot4Panel.setLayout(entrySlot4PanelLayout);
        entrySlot4PanelLayout.setHorizontalGroup(
            entrySlot4PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(entrySlot4PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(entrySlotBookingListLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(entrySlotBookingListFilePathInput4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bookingListBrowseButton4)
                .addContainerGap())
        );
        entrySlot4PanelLayout.setVerticalGroup(
            entrySlot4PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(entrySlot4PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(entrySlot4PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false)
                    .addComponent(entrySlotBookingListLabel4)
                    .addComponent(entrySlotBookingListFilePathInput4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bookingListBrowseButton4))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        entrySlot5Panel.setName("entrySlot5Panel"); // NOI18N
        entrySlot5Panel.setPreferredSize(new java.awt.Dimension(475, 40));

        entrySlotBookingListFilePathInput5.setText(fileInputDefault);
        entrySlotBookingListFilePathInput5.setName("entrySlotBookingListFilePathInput5"); // NOI18N
        entrySlotBookingListFilePathInput5.setPreferredSize(new java.awt.Dimension(200, 20));
        entrySlotBookingListFilePathInput5.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                inputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                inputFocusLost(evt);
            }
        });

        bookingListBrowseButton5.setText(resourceMap.getString("bookingListBrowseButton5.text")); // NOI18N
        bookingListBrowseButton5.setName("bookingListBrowseButton5"); // NOI18N
        bookingListBrowseButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseFileAction(evt);
            }
        });

        entrySlotBookingListLabel5.setText(resourceMap.getString("entrySlotBookingListLabel5.text")); // NOI18N
        entrySlotBookingListLabel5.setName("entrySlotBookingListLabel5"); // NOI18N

        javax.swing.GroupLayout entrySlot5PanelLayout = new javax.swing.GroupLayout(entrySlot5Panel);
        entrySlot5Panel.setLayout(entrySlot5PanelLayout);
        entrySlot5PanelLayout.setHorizontalGroup(
            entrySlot5PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(entrySlot5PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(entrySlotBookingListLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(entrySlotBookingListFilePathInput5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bookingListBrowseButton5)
                .addContainerGap())
        );
        entrySlot5PanelLayout.setVerticalGroup(
            entrySlot5PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(entrySlot5PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(entrySlot5PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(entrySlotBookingListLabel5)
                    .addComponent(entrySlotBookingListFilePathInput5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bookingListBrowseButton5))
                .addContainerGap())
        );

        waitingListFilePanel.setName("waitingListFilePanel"); // NOI18N
        waitingListFilePanel.setPreferredSize(new java.awt.Dimension(500, 40));

        waitingListFileLabel.setText(resourceMap.getString("waitingListFileLabel.text")); // NOI18N
        waitingListFileLabel.setName("waitingListFileLabel"); // NOI18N

        waitingListBrowseButton.setText(resourceMap.getString("waitingListBrowseButton.text")); // NOI18N
        waitingListBrowseButton.setName("waitingListBrowseButton"); // NOI18N
        waitingListBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseFileAction(evt);
            }
        });

        waitingListFilePathInput.setText(fileInputDefault);
        waitingListFilePathInput.setName("waitingListFilePathInput"); // NOI18N
        waitingListFilePathInput.setPreferredSize(new java.awt.Dimension(200, 20));
        waitingListFilePathInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                inputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                inputFocusLost(evt);
            }
        });

        javax.swing.GroupLayout waitingListFilePanelLayout = new javax.swing.GroupLayout(waitingListFilePanel);
        waitingListFilePanel.setLayout(waitingListFilePanelLayout);
        waitingListFilePanelLayout.setHorizontalGroup(
            waitingListFilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(waitingListFilePanelLayout.createSequentialGroup()
                .addGap(70, 70, 70)
                .addComponent(waitingListFileLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(waitingListFilePathInput, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(waitingListBrowseButton)
                .addContainerGap())
        );
        waitingListFilePanelLayout.setVerticalGroup(
            waitingListFilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(waitingListFilePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(waitingListFilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(waitingListBrowseButton)
                    .addComponent(waitingListFilePathInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(waitingListFileLabel))
                .addContainerGap())
        );

        entrySlot3Panel.setName("entrySlot3Panel"); // NOI18N
        entrySlot3Panel.setPreferredSize(new java.awt.Dimension(475, 40));

        bookingListBrowseButton3.setText(resourceMap.getString("bookingListBrowseButton3.text")); // NOI18N
        bookingListBrowseButton3.setName("bookingListBrowseButton3"); // NOI18N
        bookingListBrowseButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseFileAction(evt);
            }
        });

        entrySlotBookingListLabel3.setText(resourceMap.getString("entrySlotBookingListLabel3.text")); // NOI18N
        entrySlotBookingListLabel3.setName("entrySlotBookingListLabel3"); // NOI18N

        entrySlotBookingListFilePathInput3.setText(fileInputDefault);
        entrySlotBookingListFilePathInput3.setName("entrySlotBookingListFilePathInput3"); // NOI18N
        entrySlotBookingListFilePathInput3.setPreferredSize(new java.awt.Dimension(200, 20));
        entrySlotBookingListFilePathInput3.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                inputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                inputFocusLost(evt);
            }
        });

        javax.swing.GroupLayout entrySlot3PanelLayout = new javax.swing.GroupLayout(entrySlot3Panel);
        entrySlot3Panel.setLayout(entrySlot3PanelLayout);
        entrySlot3PanelLayout.setHorizontalGroup(
            entrySlot3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(entrySlot3PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(entrySlotBookingListLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(entrySlotBookingListFilePathInput3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bookingListBrowseButton3)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        entrySlot3PanelLayout.setVerticalGroup(
            entrySlot3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(entrySlot3PanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(entrySlot3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(entrySlotBookingListLabel3)
                    .addComponent(entrySlotBookingListFilePathInput3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bookingListBrowseButton3))
                .addContainerGap())
        );

        slotsPanel.setName("slotsPanel"); // NOI18N

        entrySlotsSpinnerOffline.setName("entrySlotsSpinnerOffline"); // NOI18N
        entrySlotsSpinnerOffline.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                entrySlotsSpinnerOfflineStateChanged(evt);
            }
        });

        entrySlotsLabel.setText(resourceMap.getString("entrySlotsLabel.text")); // NOI18N
        entrySlotsLabel.setName("entrySlotsLabel"); // NOI18N

        javax.swing.GroupLayout slotsPanelLayout = new javax.swing.GroupLayout(slotsPanel);
        slotsPanel.setLayout(slotsPanelLayout);
        slotsPanelLayout.setHorizontalGroup(
            slotsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slotsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(entrySlotsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(entrySlotsSpinnerOffline, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        slotsPanelLayout.setVerticalGroup(
            slotsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slotsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(slotsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(entrySlotsLabel)
                    .addComponent(entrySlotsSpinnerOffline, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        waitingListRadioPanel.setName("waitingListRadioPanel"); // NOI18N

        waitingListButtonGroup.add(noWaitingListRadioButton);
        noWaitingListRadioButton.setSelected(true);
        noWaitingListRadioButton.setText(resourceMap.getString("noWaitingListRadioButton.text")); // NOI18N
        noWaitingListRadioButton.setName("noWaitingListRadioButton"); // NOI18N
        noWaitingListRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                noWaitingListRadioButtonActionPerformed(evt);
            }
        });

        waitingListButtonGroup.add(yesWaitingListRadioButton);
        yesWaitingListRadioButton.setText(resourceMap.getString("yesWaitingListRadioButton.text")); // NOI18N
        yesWaitingListRadioButton.setName("yesWaitingListRadioButton"); // NOI18N
        yesWaitingListRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yesWaitingListRadioButtonActionPerformed(evt);
            }
        });

        waitingListLabel.setText(resourceMap.getString("waitingListLabel.text")); // NOI18N
        waitingListLabel.setName("waitingListLabel"); // NOI18N

        javax.swing.GroupLayout waitingListRadioPanelLayout = new javax.swing.GroupLayout(waitingListRadioPanel);
        waitingListRadioPanel.setLayout(waitingListRadioPanelLayout);
        waitingListRadioPanelLayout.setHorizontalGroup(
            waitingListRadioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(waitingListRadioPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(waitingListLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(yesWaitingListRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(noWaitingListRadioButton)
                .addContainerGap())
        );
        waitingListRadioPanelLayout.setVerticalGroup(
            waitingListRadioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(waitingListRadioPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(waitingListRadioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(waitingListLabel)
                    .addComponent(yesWaitingListRadioButton)
                    .addComponent(noWaitingListRadioButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout bookingDetailsPanelLayout = new javax.swing.GroupLayout(bookingDetailsPanel);
        bookingDetailsPanel.setLayout(bookingDetailsPanelLayout);
        bookingDetailsPanelLayout.setHorizontalGroup(
            bookingDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bookingDetailsPanelLayout.createSequentialGroup()
                .addGroup(bookingDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(slotsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(waitingListRadioPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(bookingDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bookingDetailsPanelLayout.createSequentialGroup()
                        .addComponent(entrySlot1Panel, javax.swing.GroupLayout.PREFERRED_SIZE, 412, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(88, 88, 88))
                    .addGroup(bookingDetailsPanelLayout.createSequentialGroup()
                        .addComponent(waitingListFilePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 413, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(bookingDetailsPanelLayout.createSequentialGroup()
                        .addGroup(bookingDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(entrySlot2Panel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE)
                            .addComponent(entrySlot3Panel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE)
                            .addComponent(entrySlot4Panel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE)
                            .addComponent(entrySlot5Panel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE))
                        .addContainerGap())))
        );
        bookingDetailsPanelLayout.setVerticalGroup(
            bookingDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bookingDetailsPanelLayout.createSequentialGroup()
                .addGroup(bookingDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(entrySlot1Panel, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(slotsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(1, 1, 1)
                .addComponent(entrySlot2Panel, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(entrySlot3Panel, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(entrySlot4Panel, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(entrySlot5Panel, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(bookingDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(bookingDetailsPanelLayout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addComponent(waitingListRadioPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(bookingDetailsPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(waitingListFilePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        configBackButton.setAction(actionMap.get("goBack")); // NOI18N
        configBackButton.setText(resourceMap.getString("configBackButton.text")); // NOI18N
        configBackButton.setName("configBackButton"); // NOI18N

        requireBookingPanel.setName("requireBookingPanel"); // NOI18N

        requireBookingButtonGroup.add(yesBookingRadioButton);
        yesBookingRadioButton.setSelected(true);
        yesBookingRadioButton.setText(resourceMap.getString("yesBookingRadioButton.text")); // NOI18N
        yesBookingRadioButton.setName("yesBookingRadioButton"); // NOI18N
        yesBookingRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yesBookingRadioButtonActionPerformed(evt);
            }
        });

        requireBookingButtonGroup.add(noBookingRadioButton);
        noBookingRadioButton.setText(resourceMap.getString("noBookingRadioButton.text")); // NOI18N
        noBookingRadioButton.setName("noBookingRadioButton"); // NOI18N
        noBookingRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                noBookingRadioButtonActionPerformed(evt);
            }
        });

        requireBookingLabel.setText(resourceMap.getString("requireBookingLabel.text")); // NOI18N
        requireBookingLabel.setName("requireBookingLabel"); // NOI18N

        javax.swing.GroupLayout requireBookingPanelLayout = new javax.swing.GroupLayout(requireBookingPanel);
        requireBookingPanel.setLayout(requireBookingPanelLayout);
        requireBookingPanelLayout.setHorizontalGroup(
            requireBookingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(requireBookingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(requireBookingLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(yesBookingRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(noBookingRadioButton)
                .addContainerGap())
        );
        requireBookingPanelLayout.setVerticalGroup(
            requireBookingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(requireBookingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(requireBookingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(requireBookingLabel)
                    .addComponent(yesBookingRadioButton)
                    .addComponent(noBookingRadioButton))
                .addContainerGap())
        );

        eventTitlePanel.setName("eventTitlePanel"); // NOI18N
        eventTitlePanel.setPreferredSize(new java.awt.Dimension(750, 42));

        eventTitleInputLabel.setText(resourceMap.getString("eventTitleInputLabel.text")); // NOI18N
        eventTitleInputLabel.setName("eventTitleInputLabel"); // NOI18N

        eventTitleInput.setText(titleInputDefault);
        eventTitleInput.setName("eventTitleInput"); // NOI18N
        eventTitleInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                inputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                inputFocusLost(evt);
            }
        });
        javax.swing.Action checkConfiguration = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                checkConfiguration();
            }
        };
        eventTitleInput.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            "checkConfiguration");
        eventTitleInput.getActionMap().put("checkConfiguration", checkConfiguration);

        javax.swing.GroupLayout eventTitlePanelLayout = new javax.swing.GroupLayout(eventTitlePanel);
        eventTitlePanel.setLayout(eventTitlePanelLayout);
        eventTitlePanelLayout.setHorizontalGroup(
            eventTitlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(eventTitlePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(eventTitleInputLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(eventTitleInput, javax.swing.GroupLayout.DEFAULT_SIZE, 492, Short.MAX_VALUE)
                .addContainerGap())
        );
        eventTitlePanelLayout.setVerticalGroup(
            eventTitlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(eventTitlePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(eventTitlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(eventTitleInputLabel)
                    .addComponent(eventTitleInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(48, 48, 48))
        );

        javax.swing.GroupLayout configPanelLayout = new javax.swing.GroupLayout(configPanel);
        configPanel.setLayout(configPanelLayout);
        configPanelLayout.setHorizontalGroup(
            configPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(configPanelLayout.createSequentialGroup()
                .addGroup(configPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(requireBookingPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(eventTitlePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 583, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, configPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(configPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(configPanelLayout.createSequentialGroup()
                                .addComponent(titleLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(configBackButton, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(okConfigButton, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(bookingDetailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 702, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        configPanelLayout.setVerticalGroup(
            configPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(configPanelLayout.createSequentialGroup()
                .addContainerGap(115, Short.MAX_VALUE)
                .addComponent(titleLabel)
                .addGap(1, 1, 1)
                .addComponent(eventTitlePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(requireBookingPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bookingDetailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(configPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(configBackButton)
                    .addComponent(okConfigButton))
                .addContainerGap())
        );

        preConfigPanel.setFocusable(false);
        preConfigPanel.setMinimumSize(new java.awt.Dimension(720, 350));
        preConfigPanel.setName("preConfigPanel"); // NOI18N

        titlePanel.setFocusable(false);
        titlePanel.setName("titlePanel"); // NOI18N

        smallLogoLabel.setIcon(resourceMap.getIcon("smallLogoLabel.icon")); // NOI18N
        smallLogoLabel.setText(resourceMap.getString("smallLogoLabel.text")); // NOI18N
        smallLogoLabel.setFocusable(false);
        smallLogoLabel.setName("smallLogoLabel"); // NOI18N

        versionLabel.setFont(resourceMap.getFont("versionLabel.font")); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("eventswipe/resources/EventSwipeApp"); // NOI18N
        versionLabel.setText(bundle.getString("Application.version")); // NOI18N
        versionLabel.setFocusable(false);
        versionLabel.setName("versionLabel"); // NOI18N

        mainTitle.setFont(resourceMap.getFont("mainTitle.font")); // NOI18N
        mainTitle.setText(resourceMap.getString("mainTitle.text")); // NOI18N
        mainTitle.setFocusable(false);
        mainTitle.setName("mainTitle"); // NOI18N

        javax.swing.GroupLayout titlePanelLayout = new javax.swing.GroupLayout(titlePanel);
        titlePanel.setLayout(titlePanelLayout);
        titlePanelLayout.setHorizontalGroup(
            titlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(titlePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(smallLogoLabel)
                .addGroup(titlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(titlePanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(mainTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 253, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18))
                    .addGroup(titlePanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(versionLabel)
                        .addContainerGap())))
        );
        titlePanelLayout.setVerticalGroup(
            titlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(titlePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(titlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(titlePanelLayout.createSequentialGroup()
                        .addComponent(mainTitle)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(versionLabel))
                    .addComponent(smallLogoLabel))
                .addGap(14, 14, 14))
        );

        titleLoginPanel.setFocusable(false);
        titleLoginPanel.setName("titleLoginPanel"); // NOI18N

        loginOfflineSeparator.setOrientation(javax.swing.SwingConstants.VERTICAL);
        loginOfflineSeparator.setName("loginOfflineSeparator"); // NOI18N

        loginFormPanel.setName("loginFormPanel"); // NOI18N

        logInButton.setText(resourceMap.getString("logInButton.text")); // NOI18N
        logInButton.setName("logInButton"); // NOI18N
        logInButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logInButtonActionPerformed(evt);
            }
        });

        passwordLabel.setText(resourceMap.getString("passwordLabel.text")); // NOI18N
        passwordLabel.setFocusable(false);
        passwordLabel.setName("passwordLabel"); // NOI18N

        usernameLabel.setText(resourceMap.getString("usernameLabel.text")); // NOI18N
        usernameLabel.setFocusable(false);
        usernameLabel.setName("usernameLabel"); // NOI18N

        usernameInput.setText(app.getData().getDefaultUsername());
        usernameInput.setName("usernameInput"); // NOI18N
        usernameInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                loginInputKeyPressed(evt);
            }
        });

        passwordInput.setText(String.valueOf(app.getData().getDefaultPassword()));
        passwordInput.setName("passwordInput"); // NOI18N
        passwordInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                loginInputKeyPressed(evt);
            }
        });

        logInLabel.setFont(resourceMap.getFont("logInLabel.font")); // NOI18N
        logInLabel.setText(resourceMap.getString("logInLabel.text")); // NOI18N
        logInLabel.setFocusable(false);
        logInLabel.setName("logInLabel"); // NOI18N

        javax.swing.GroupLayout loginFormPanelLayout = new javax.swing.GroupLayout(loginFormPanel);
        loginFormPanel.setLayout(loginFormPanelLayout);
        loginFormPanelLayout.setHorizontalGroup(
            loginFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loginFormPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(loginFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(logInButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(logInLabel, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(usernameLabel, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(passwordLabel, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(usernameInput, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)
                    .addComponent(passwordInput, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap(12, Short.MAX_VALUE))
        );
        loginFormPanelLayout.setVerticalGroup(
            loginFormPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loginFormPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(logInLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(usernameLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(usernameInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(passwordLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(passwordInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                .addComponent(logInButton)
                .addContainerGap())
        );

        offlinePanel.setName("offlinePanel"); // NOI18N

        useOfflineLabel.setFont(resourceMap.getFont("useOfflineLabel.font")); // NOI18N
        useOfflineLabel.setText(resourceMap.getString("useOfflineLabel.text")); // NOI18N
        useOfflineLabel.setFocusable(false);
        useOfflineLabel.setName("useOfflineLabel"); // NOI18N

        startOfflineButton.setText(resourceMap.getString("startOfflineButton.text")); // NOI18N
        startOfflineButton.setName("startOfflineButton"); // NOI18N
        startOfflineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startOfflineButtonActionPerformed(evt);
            }
        });

        jScrollPane1.setBorder(null);
        jScrollPane1.setFocusable(false);
        jScrollPane1.setName("jScrollPane1"); // NOI18N

        useOfflineDescription.setBackground(resourceMap.getColor("useOfflineDescription.background")); // NOI18N
        useOfflineDescription.setColumns(20);
        useOfflineDescription.setEditable(false);
        useOfflineDescription.setFont(resourceMap.getFont("useOfflineDescription.font")); // NOI18N
        useOfflineDescription.setLineWrap(true);
        useOfflineDescription.setRows(5);
        useOfflineDescription.setText(resourceMap.getString("useOfflineDescription.text")); // NOI18N
        useOfflineDescription.setWrapStyleWord(true);
        useOfflineDescription.setAutoscrolls(false);
        useOfflineDescription.setBorder(null);
        useOfflineDescription.setFocusable(false);
        useOfflineDescription.setName("useOfflineDescription"); // NOI18N
        jScrollPane1.setViewportView(useOfflineDescription);

        javax.swing.GroupLayout offlinePanelLayout = new javax.swing.GroupLayout(offlinePanel);
        offlinePanel.setLayout(offlinePanelLayout);
        offlinePanelLayout.setHorizontalGroup(
            offlinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(offlinePanelLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
                .addGap(20, 20, 20))
            .addGroup(offlinePanelLayout.createSequentialGroup()
                .addComponent(useOfflineLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 222, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(offlinePanelLayout.createSequentialGroup()
                .addComponent(startOfflineButton, javax.swing.GroupLayout.DEFAULT_SIZE, 222, Short.MAX_VALUE)
                .addContainerGap())
        );
        offlinePanelLayout.setVerticalGroup(
            offlinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(offlinePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(useOfflineLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(startOfflineButton)
                .addContainerGap())
        );

        offlineCounterSeparator.setOrientation(javax.swing.SwingConstants.VERTICAL);
        offlineCounterSeparator.setName("offlineCounterSeparator"); // NOI18N

        startCounterModePanel.setName("startCounterModePanel"); // NOI18N

        startCounterModeButton.setText(resourceMap.getString("startCounterModeButton.text")); // NOI18N
        startCounterModeButton.setName("startCounterModeButton"); // NOI18N
        startCounterModeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startCounterModeButtonActionPerformed(evt);
            }
        });

        jLabel1.setFont(resourceMap.getFont("logInLabel.font")); // NOI18N
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jScrollPane2.setBorder(null);
        jScrollPane2.setFocusable(false);
        jScrollPane2.setName("jScrollPane2"); // NOI18N

        useOfflineDescription1.setBackground(resourceMap.getColor("useOfflineDescription1.background")); // NOI18N
        useOfflineDescription1.setColumns(20);
        useOfflineDescription1.setEditable(false);
        useOfflineDescription1.setFont(resourceMap.getFont("useOfflineDescription1.font")); // NOI18N
        useOfflineDescription1.setLineWrap(true);
        useOfflineDescription1.setText(resourceMap.getString("useOfflineDescription1.text")); // NOI18N
        useOfflineDescription1.setWrapStyleWord(true);
        useOfflineDescription1.setAutoscrolls(false);
        useOfflineDescription1.setBorder(null);
        useOfflineDescription1.setFocusable(false);
        useOfflineDescription1.setName("useOfflineDescription1"); // NOI18N
        jScrollPane2.setViewportView(useOfflineDescription1);

        javax.swing.GroupLayout startCounterModePanelLayout = new javax.swing.GroupLayout(startCounterModePanel);
        startCounterModePanel.setLayout(startCounterModePanelLayout);
        startCounterModePanelLayout.setHorizontalGroup(
            startCounterModePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, startCounterModePanelLayout.createSequentialGroup()
                .addGroup(startCounterModePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE)
                    .addComponent(startCounterModeButton, javax.swing.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE))
                .addContainerGap())
        );
        startCounterModePanelLayout.setVerticalGroup(
            startCounterModePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(startCounterModePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 37, Short.MAX_VALUE)
                .addComponent(startCounterModeButton)
                .addContainerGap())
        );

        javax.swing.GroupLayout titleLoginPanelLayout = new javax.swing.GroupLayout(titleLoginPanel);
        titleLoginPanel.setLayout(titleLoginPanelLayout);
        titleLoginPanelLayout.setHorizontalGroup(
            titleLoginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(titleLoginPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(loginFormPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(loginOfflineSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 8, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(offlinePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(offlineCounterSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(startCounterModePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20))
        );
        titleLoginPanelLayout.setVerticalGroup(
            titleLoginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(titleLoginPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(titleLoginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(loginFormPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(startCounterModePanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(offlineCounterSeparator, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(offlinePanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(loginOfflineSeparator, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout preConfigPanelLayout = new javax.swing.GroupLayout(preConfigPanel);
        preConfigPanel.setLayout(preConfigPanelLayout);
        preConfigPanelLayout.setHorizontalGroup(
            preConfigPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(preConfigPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(preConfigPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(titlePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(titleLoginPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 710, Short.MAX_VALUE))
                .addContainerGap())
        );
        preConfigPanelLayout.setVerticalGroup(
            preConfigPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(preConfigPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(titlePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26)
                .addComponent(titleLoginPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        onlineConfigPanel.setMinimumSize(new java.awt.Dimension(720, 400));
        onlineConfigPanel.setName("onlineConfigPanel"); // NOI18N
        onlineConfigPanel.setPreferredSize(new java.awt.Dimension(723, 400));

        bookingDetailsPanel1.setEnabled(false);
        bookingDetailsPanel1.setName("bookingDetailsPanel1"); // NOI18N

        entrySlotsLabel1.setText(resourceMap.getString("entrySlotsLabel1.text")); // NOI18N
        entrySlotsLabel1.setName("entrySlotsLabel1"); // NOI18N

        entrySlotsSpinnerOnline.setName("entrySlotsSpinnerOnline"); // NOI18N
        entrySlotsSpinnerOnline.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                entrySlotsSpinnerOnlineStateChanged(evt);
            }
        });

        slot1DetailsPanel.setName("slot1DetailsPanel"); // NOI18N

        generatedTitleLabel1.setText(resourceMap.getString("generatedTitleLabel1.text")); // NOI18N
        generatedTitleLabel1.setName("generatedTitleLabel1"); // NOI18N

        searchEventsButton1.setText(resourceMap.getString("searchEventsButton1.text")); // NOI18N
        searchEventsButton1.setName("searchEventsButton1"); // NOI18N
        searchEventsButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchEventsButtonActionPerformed(evt);
            }
        });

        entrySlotIdLabel1.setText(resourceMap.getString("entrySlotIdLabel1.text")); // NOI18N
        entrySlotIdLabel1.setName("entrySlotIdLabel1"); // NOI18N

        generatedTitle1.setText(resourceMap.getString("generatedTitle1.text")); // NOI18N
        generatedTitle1.setEnabled(false);
        generatedTitle1.setName("generatedTitle1"); // NOI18N

        loadEventButton1.setText(resourceMap.getString("loadEventButton1.text")); // NOI18N
        loadEventButton1.setName("loadEventButton1"); // NOI18N
        loadEventButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadEventButtonActionPerformed(evt);
            }
        });

        entrySlotIdInput1.setText(idInputDefault);
        entrySlotIdInput1.setName("entrySlotIdInput1"); // NOI18N
        entrySlotIdInput1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                inputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                inputFocusLost(evt);
            }
        });
        entrySlotIdInput1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                idInputKeyPressed(evt);
            }
        });

        slot1Label.setFont(resourceMap.getFont("slot1Label.font")); // NOI18N
        slot1Label.setText(resourceMap.getString("slot1Label.text")); // NOI18N
        slot1Label.setName("slot1Label"); // NOI18N

        javax.swing.GroupLayout slot1DetailsPanelLayout = new javax.swing.GroupLayout(slot1DetailsPanel);
        slot1DetailsPanel.setLayout(slot1DetailsPanelLayout);
        slot1DetailsPanelLayout.setHorizontalGroup(
            slot1DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slot1DetailsPanelLayout.createSequentialGroup()
                .addGroup(slot1DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(slot1DetailsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(slot1DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(generatedTitleLabel1)
                            .addComponent(entrySlotIdLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(slot1DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(slot1DetailsPanelLayout.createSequentialGroup()
                                .addComponent(entrySlotIdInput1, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(loadEventButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(searchEventsButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(generatedTitle1, javax.swing.GroupLayout.PREFERRED_SIZE, 281, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(slot1Label))
                .addContainerGap())
        );
        slot1DetailsPanelLayout.setVerticalGroup(
            slot1DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slot1DetailsPanelLayout.createSequentialGroup()
                .addComponent(slot1Label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(slot1DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(entrySlotIdInput1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(searchEventsButton1)
                    .addComponent(entrySlotIdLabel1)
                    .addComponent(loadEventButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(slot1DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(generatedTitle1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(generatedTitleLabel1))
                .addContainerGap())
        );

        slot2DetailsPanel.setName("slot2DetailsPanel"); // NOI18N

        generatedTitle2.setText(resourceMap.getString("generatedTitle2.text")); // NOI18N
        generatedTitle2.setEnabled(false);
        generatedTitle2.setName("generatedTitle2"); // NOI18N

        entrySlotIdLabel2.setText(resourceMap.getString("entrySlotIdLabel2.text")); // NOI18N
        entrySlotIdLabel2.setName("entrySlotIdLabel2"); // NOI18N

        slot2Label.setFont(resourceMap.getFont("slot2Label.font")); // NOI18N
        slot2Label.setText(resourceMap.getString("slot2Label.text")); // NOI18N
        slot2Label.setEnabled(false);
        slot2Label.setFocusable(false);
        slot2Label.setName("slot2Label"); // NOI18N

        entrySlotIdInput2.setText(idInputDefault);
        entrySlotIdInput2.setEnabled(false);
        entrySlotIdInput2.setName("entrySlotIdInput2"); // NOI18N
        entrySlotIdInput2.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                inputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                inputFocusLost(evt);
            }
        });
        entrySlotIdInput2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                idInputKeyPressed(evt);
            }
        });

        loadEventButton2.setText(resourceMap.getString("loadEventButton2.text")); // NOI18N
        loadEventButton2.setEnabled(false);
        loadEventButton2.setName("loadEventButton2"); // NOI18N
        loadEventButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadEventButtonActionPerformed(evt);
            }
        });

        searchEventsButton2.setText(resourceMap.getString("searchEventsButton2.text")); // NOI18N
        searchEventsButton2.setEnabled(false);
        searchEventsButton2.setName("searchEventsButton2"); // NOI18N
        searchEventsButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchEventsButtonActionPerformed(evt);
            }
        });

        generatedTitleLabel2.setText(resourceMap.getString("generatedTitleLabel2.text")); // NOI18N
        generatedTitleLabel2.setName("generatedTitleLabel2"); // NOI18N

        javax.swing.GroupLayout slot2DetailsPanelLayout = new javax.swing.GroupLayout(slot2DetailsPanel);
        slot2DetailsPanel.setLayout(slot2DetailsPanelLayout);
        slot2DetailsPanelLayout.setHorizontalGroup(
            slot2DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slot2DetailsPanelLayout.createSequentialGroup()
                .addGroup(slot2DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(slot2DetailsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(slot2DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(generatedTitleLabel2)
                            .addComponent(entrySlotIdLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(slot2DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(slot2DetailsPanelLayout.createSequentialGroup()
                                .addComponent(entrySlotIdInput2, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(loadEventButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(searchEventsButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(generatedTitle2, javax.swing.GroupLayout.PREFERRED_SIZE, 285, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(slot2Label))
                .addContainerGap(26, Short.MAX_VALUE))
        );
        slot2DetailsPanelLayout.setVerticalGroup(
            slot2DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slot2DetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(slot2Label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(slot2DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(entrySlotIdInput2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(loadEventButton2)
                    .addComponent(searchEventsButton2)
                    .addComponent(entrySlotIdLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(slot2DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(generatedTitle2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(generatedTitleLabel2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        slot3DtailsPanel.setName("slot3DtailsPanel"); // NOI18N

        generatedTitleLabel3.setText(resourceMap.getString("generatedTitleLabel3.text")); // NOI18N
        generatedTitleLabel3.setName("generatedTitleLabel3"); // NOI18N

        searchEventsButton3.setText(resourceMap.getString("searchEventsButton3.text")); // NOI18N
        searchEventsButton3.setEnabled(false);
        searchEventsButton3.setName("searchEventsButton3"); // NOI18N
        searchEventsButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchEventsButtonActionPerformed(evt);
            }
        });

        generatedTitle3.setText(resourceMap.getString("generatedTitle3.text")); // NOI18N
        generatedTitle3.setEnabled(false);
        generatedTitle3.setName("generatedTitle3"); // NOI18N

        slot3Label.setFont(resourceMap.getFont("slot3Label.font")); // NOI18N
        slot3Label.setText(resourceMap.getString("slot3Label.text")); // NOI18N
        slot3Label.setEnabled(false);
        slot3Label.setFocusable(false);
        slot3Label.setName("slot3Label"); // NOI18N

        entrySlotIdLabel3.setText(resourceMap.getString("entrySlotIdLabel3.text")); // NOI18N
        entrySlotIdLabel3.setName("entrySlotIdLabel3"); // NOI18N

        loadEventButton3.setText(resourceMap.getString("loadEventButton3.text")); // NOI18N
        loadEventButton3.setEnabled(false);
        loadEventButton3.setName("loadEventButton3"); // NOI18N
        loadEventButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadEventButtonActionPerformed(evt);
            }
        });

        entrySlotIdInput3.setText(idInputDefault);
        entrySlotIdInput3.setEnabled(false);
        entrySlotIdInput3.setName("entrySlotIdInput3"); // NOI18N
        entrySlotIdInput3.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                inputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                inputFocusLost(evt);
            }
        });
        entrySlotIdInput3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                idInputKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout slot3DtailsPanelLayout = new javax.swing.GroupLayout(slot3DtailsPanel);
        slot3DtailsPanel.setLayout(slot3DtailsPanelLayout);
        slot3DtailsPanelLayout.setHorizontalGroup(
            slot3DtailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slot3DtailsPanelLayout.createSequentialGroup()
                .addGroup(slot3DtailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(slot3DtailsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(slot3DtailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(entrySlotIdLabel3)
                            .addComponent(generatedTitleLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(slot3DtailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(slot3DtailsPanelLayout.createSequentialGroup()
                                .addComponent(entrySlotIdInput3, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(loadEventButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(searchEventsButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(generatedTitle3, javax.swing.GroupLayout.DEFAULT_SIZE, 289, Short.MAX_VALUE)))
                    .addComponent(slot3Label))
                .addContainerGap())
        );
        slot3DtailsPanelLayout.setVerticalGroup(
            slot3DtailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slot3DtailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(slot3Label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(slot3DtailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(entrySlotIdInput3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(entrySlotIdLabel3)
                    .addComponent(searchEventsButton3)
                    .addComponent(loadEventButton3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(slot3DtailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(generatedTitle3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(generatedTitleLabel3))
                .addContainerGap())
        );

        slot4DetailsPanel.setName("slot4DetailsPanel"); // NOI18N

        searchEventsButton4.setText(resourceMap.getString("searchEventsButton4.text")); // NOI18N
        searchEventsButton4.setEnabled(false);
        searchEventsButton4.setName("searchEventsButton4"); // NOI18N
        searchEventsButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchEventsButtonActionPerformed(evt);
            }
        });

        slot4Label.setFont(resourceMap.getFont("slot4Label.font")); // NOI18N
        slot4Label.setText(resourceMap.getString("slot4Label.text")); // NOI18N
        slot4Label.setEnabled(false);
        slot4Label.setFocusable(false);
        slot4Label.setName("slot4Label"); // NOI18N

        loadEventButton4.setText(resourceMap.getString("loadEventButton4.text")); // NOI18N
        loadEventButton4.setEnabled(false);
        loadEventButton4.setName("loadEventButton4"); // NOI18N
        loadEventButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadEventButtonActionPerformed(evt);
            }
        });

        generatedTitleLabel4.setText(resourceMap.getString("generatedTitleLabel4.text")); // NOI18N
        generatedTitleLabel4.setName("generatedTitleLabel4"); // NOI18N

        entrySlotIdInput4.setText(idInputDefault);
        entrySlotIdInput4.setEnabled(false);
        entrySlotIdInput4.setName("entrySlotIdInput4"); // NOI18N
        entrySlotIdInput4.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                inputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                inputFocusLost(evt);
            }
        });
        entrySlotIdInput4.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                idInputKeyPressed(evt);
            }
        });

        generatedTitle4.setEnabled(false);
        generatedTitle4.setName("generatedTitle4"); // NOI18N

        entrySlotIdLabel4.setText(resourceMap.getString("entrySlotIdLabel4.text")); // NOI18N
        entrySlotIdLabel4.setName("entrySlotIdLabel4"); // NOI18N

        javax.swing.GroupLayout slot4DetailsPanelLayout = new javax.swing.GroupLayout(slot4DetailsPanel);
        slot4DetailsPanel.setLayout(slot4DetailsPanelLayout);
        slot4DetailsPanelLayout.setHorizontalGroup(
            slot4DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slot4DetailsPanelLayout.createSequentialGroup()
                .addGroup(slot4DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(slot4DetailsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(slot4DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(generatedTitleLabel4)
                            .addComponent(entrySlotIdLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(slot4DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(slot4DetailsPanelLayout.createSequentialGroup()
                                .addComponent(entrySlotIdInput4, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(loadEventButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(searchEventsButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(generatedTitle4, javax.swing.GroupLayout.PREFERRED_SIZE, 282, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(slot4Label))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        slot4DetailsPanelLayout.setVerticalGroup(
            slot4DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slot4DetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(slot4Label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(slot4DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(entrySlotIdInput4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(loadEventButton4)
                    .addComponent(searchEventsButton4)
                    .addComponent(entrySlotIdLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(slot4DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(generatedTitle4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(generatedTitleLabel4))
                .addContainerGap())
        );

        slot5DetailsPanel.setName("slot5DetailsPanel"); // NOI18N

        entrySlotIdLabel5.setText(resourceMap.getString("entrySlotIdLabel5.text")); // NOI18N
        entrySlotIdLabel5.setName("entrySlotIdLabel5"); // NOI18N

        entrySlotIdInput5.setText(idInputDefault);
        entrySlotIdInput5.setEnabled(false);
        entrySlotIdInput5.setName("entrySlotIdInput5"); // NOI18N
        entrySlotIdInput5.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                inputFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                inputFocusLost(evt);
            }
        });
        entrySlotIdInput5.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                idInputKeyPressed(evt);
            }
        });

        loadEventButton5.setText(resourceMap.getString("loadEventButton5.text")); // NOI18N
        loadEventButton5.setEnabled(false);
        loadEventButton5.setName("loadEventButton5"); // NOI18N
        loadEventButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadEventButtonActionPerformed(evt);
            }
        });

        generatedTitle5.setEnabled(false);
        generatedTitle5.setName("generatedTitle5"); // NOI18N

        generatedTitleLabel5.setText(resourceMap.getString("generatedTitleLabel5.text")); // NOI18N
        generatedTitleLabel5.setName("generatedTitleLabel5"); // NOI18N

        searchEventsButton5.setText(resourceMap.getString("searchEventsButton5.text")); // NOI18N
        searchEventsButton5.setEnabled(false);
        searchEventsButton5.setName("searchEventsButton5"); // NOI18N
        searchEventsButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchEventsButtonActionPerformed(evt);
            }
        });

        slot5Label.setFont(resourceMap.getFont("slot5Label.font")); // NOI18N
        slot5Label.setText(resourceMap.getString("slot5Label.text")); // NOI18N
        slot5Label.setEnabled(false);
        slot5Label.setFocusable(false);
        slot5Label.setName("slot5Label"); // NOI18N

        javax.swing.GroupLayout slot5DetailsPanelLayout = new javax.swing.GroupLayout(slot5DetailsPanel);
        slot5DetailsPanel.setLayout(slot5DetailsPanelLayout);
        slot5DetailsPanelLayout.setHorizontalGroup(
            slot5DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slot5DetailsPanelLayout.createSequentialGroup()
                .addGroup(slot5DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(slot5DetailsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(slot5DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(entrySlotIdLabel5)
                            .addComponent(generatedTitleLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(slot5DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(slot5DetailsPanelLayout.createSequentialGroup()
                                .addComponent(entrySlotIdInput5, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(loadEventButton5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(searchEventsButton5))
                            .addComponent(generatedTitle5, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)))
                    .addComponent(slot5Label))
                .addContainerGap())
        );
        slot5DetailsPanelLayout.setVerticalGroup(
            slot5DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slot5DetailsPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(slot5Label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(slot5DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(entrySlotIdInput5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(entrySlotIdLabel5)
                    .addComponent(searchEventsButton5)
                    .addComponent(loadEventButton5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(slot5DetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(generatedTitle5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(generatedTitleLabel5))
                .addContainerGap())
        );

        waitingListOptionsPanel.setName("waitingListOptionsPanel"); // NOI18N

        loadWaitingListLabel.setText(resourceMap.getString("loadWaitingListLabel.text")); // NOI18N
        loadWaitingListLabel.setName("loadWaitingListLabel"); // NOI18N

        onlineWaitingListButtonGroup.add(noLoadWaitingListRadioButton);
        noLoadWaitingListRadioButton.setSelected(true);
        noLoadWaitingListRadioButton.setText(resourceMap.getString("noLoadWaitingListRadioButton.text")); // NOI18N
        noLoadWaitingListRadioButton.setName("noLoadWaitingListRadioButton"); // NOI18N

        onlineWaitingListButtonGroup.add(yesLoadWaitingListRadioButton);
        yesLoadWaitingListRadioButton.setText(resourceMap.getString("yesLoadWaitingListRadioButton.text")); // NOI18N
        yesLoadWaitingListRadioButton.setName("yesLoadWaitingListRadioButton"); // NOI18N

        javax.swing.GroupLayout waitingListOptionsPanelLayout = new javax.swing.GroupLayout(waitingListOptionsPanel);
        waitingListOptionsPanel.setLayout(waitingListOptionsPanelLayout);
        waitingListOptionsPanelLayout.setHorizontalGroup(
            waitingListOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(waitingListOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(loadWaitingListLabel)
                .addGap(4, 4, 4)
                .addComponent(yesLoadWaitingListRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(noLoadWaitingListRadioButton)
                .addContainerGap())
        );
        waitingListOptionsPanelLayout.setVerticalGroup(
            waitingListOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(waitingListOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(waitingListOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(loadWaitingListLabel)
                    .addComponent(yesLoadWaitingListRadioButton)
                    .addComponent(noLoadWaitingListRadioButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout bookingDetailsPanel1Layout = new javax.swing.GroupLayout(bookingDetailsPanel1);
        bookingDetailsPanel1.setLayout(bookingDetailsPanel1Layout);
        bookingDetailsPanel1Layout.setHorizontalGroup(
            bookingDetailsPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bookingDetailsPanel1Layout.createSequentialGroup()
                .addGroup(bookingDetailsPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(bookingDetailsPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(slot2DetailsPanel, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(bookingDetailsPanel1Layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addComponent(entrySlotsLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(entrySlotsSpinnerOnline, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(bookingDetailsPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(slot4DetailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(bookingDetailsPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(slot1DetailsPanel, 0, 328, Short.MAX_VALUE)
                    .addComponent(slot5DetailsPanel, 0, 328, Short.MAX_VALUE)
                    .addComponent(slot3DtailsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 328, Short.MAX_VALUE))
                .addGap(7, 7, 7))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bookingDetailsPanel1Layout.createSequentialGroup()
                .addGap(505, 505, 505)
                .addComponent(waitingListOptionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 191, Short.MAX_VALUE)
                .addContainerGap())
        );
        bookingDetailsPanel1Layout.setVerticalGroup(
            bookingDetailsPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bookingDetailsPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(bookingDetailsPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(bookingDetailsPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(entrySlotsSpinnerOnline, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(entrySlotsLabel1))
                    .addComponent(slot1DetailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(bookingDetailsPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(slot3DtailsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(slot2DetailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(bookingDetailsPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(slot4DetailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(slot5DetailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(waitingListOptionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20))
        );

        onlineConfigBackButton.setAction(actionMap.get("goBack")); // NOI18N
        onlineConfigBackButton.setText(resourceMap.getString("onlineConfigBackButton.text")); // NOI18N
        onlineConfigBackButton.setName("onlineConfigBackButton"); // NOI18N

        aboutEventLabel.setFont(resourceMap.getFont("aboutEventLabel.font")); // NOI18N
        aboutEventLabel.setText(resourceMap.getString("aboutEventLabel.text")); // NOI18N
        aboutEventLabel.setName("aboutEventLabel"); // NOI18N

        okConfigButton1.setText(resourceMap.getString("okConfigButton1.text")); // NOI18N
        okConfigButton1.setName("okConfigButton1"); // NOI18N
        okConfigButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okConfigButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout onlineConfigPanelLayout = new javax.swing.GroupLayout(onlineConfigPanel);
        onlineConfigPanel.setLayout(onlineConfigPanelLayout);
        onlineConfigPanelLayout.setHorizontalGroup(
            onlineConfigPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(onlineConfigPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(onlineConfigPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, onlineConfigPanelLayout.createSequentialGroup()
                        .addComponent(onlineConfigBackButton, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 510, Short.MAX_VALUE)
                        .addComponent(okConfigButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(onlineConfigPanelLayout.createSequentialGroup()
                        .addComponent(aboutEventLabel)
                        .addContainerGap(624, Short.MAX_VALUE))
                    .addGroup(onlineConfigPanelLayout.createSequentialGroup()
                        .addComponent(bookingDetailsPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(7, 7, 7))))
        );
        onlineConfigPanelLayout.setVerticalGroup(
            onlineConfigPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, onlineConfigPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(aboutEventLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bookingDetailsPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(onlineConfigPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(okConfigButton1)
                    .addComponent(onlineConfigBackButton))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        mainOnlinePanel.setMinimumSize(new java.awt.Dimension(720, 350));
        mainOnlinePanel.setName("mainOnlinePanel"); // NOI18N

        searchInput.setFont(resourceMap.getFont("searchInput.font")); // NOI18N
        searchInput.setName("searchInput"); // NOI18N

        searchButton.setText(resourceMap.getString("searchButton.text")); // NOI18N
        searchButton.setFocusable(false);
        searchButton.setName("searchButton"); // NOI18N
        searchButton.setRequestFocusEnabled(false);
        searchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchButtonActionPerformed(evt);
            }
        });

        bookingStatusScrollPane1.setBackground(null);
        bookingStatusScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        bookingStatusScrollPane1.setFocusable(false);
        bookingStatusScrollPane1.setName("bookingStatusScrollPane1"); // NOI18N

        bookingStatusTextArea1.setBackground(null);
        bookingStatusTextArea1.setColumns(20);
        bookingStatusTextArea1.setEditable(false);
        bookingStatusTextArea1.setRows(5);
        bookingStatusTextArea1.setFocusable(false);
        bookingStatusTextArea1.setName("bookingStatusTextArea1"); // NOI18N
        bookingStatusTextArea1.setRequestFocusEnabled(false);
        bookingStatusScrollPane1.setViewportView(bookingStatusTextArea1);

        backButton1.setAction(actionMap.get("goBack")); // NOI18N
        backButton1.setText(resourceMap.getString("backButton1.text")); // NOI18N
        backButton1.setFocusable(false);
        backButton1.setName("backButton1"); // NOI18N
        backButton1.setRequestFocusEnabled(false);

        finishButton.setAction(actionMap.get("initialFinishAction")); // NOI18N
        finishButton.setText(resourceMap.getString("finishButton.text")); // NOI18N
        finishButton.setAutoscrolls(true);
        finishButton.setFocusable(false);
        finishButton.setName("finishButton"); // NOI18N
        finishButton.setRequestFocusEnabled(false);

        searchInputLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        searchInputLabel.setLabelFor(searchInput);
        searchInputLabel.setText(resourceMap.getString("searchInputLabel.text")); // NOI18N
        searchInputLabel.setAlignmentY(0.0F);
        searchInputLabel.setFocusable(false);
        searchInputLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        searchInputLabel.setName("searchInputLabel"); // NOI18N

        checkingModeToggle1.setText(resourceMap.getString("checkingModeToggle1.text")); // NOI18N
        checkingModeToggle1.setToolTipText(resourceMap.getString("checkingModeToggle1.toolTipText")); // NOI18N
        checkingModeToggle1.setFocusPainted(false);
        checkingModeToggle1.setFocusable(false);
        checkingModeToggle1.setName("checkingModeToggle1"); // NOI18N
        checkingModeToggle1.setRequestFocusEnabled(false);
        checkingModeToggle1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkingModeToggle1ActionPerformed(evt);
            }
        });

        onlineModeToggle.setIcon(resourceMap.getIcon("onlineModeToggler.icon")); // NOI18N
        onlineModeToggle.setText(resourceMap.getString("onlineModeToggler.text")); // NOI18N
        onlineModeToggle.setToolTipText(resourceMap.getString("onlineModeToggler.toolTipText")); // NOI18N
        onlineModeToggle.setFocusable(false);
        onlineModeToggle.setName("onlineModeToggler"); // NOI18N
        onlineModeToggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onlineModeToggleActionPerformed(evt);
            }
        });

        bookingStatusPanel.setFocusable(false);
        bookingStatusPanel.setName("bookingStatusPanel"); // NOI18N

        totalLabel.setText(resourceMap.getString("totalLabel.text")); // NOI18N
        totalLabel.setFocusable(false);
        totalLabel.setName("totalLabel"); // NOI18N

        entrySlotDisplayTextField1.setEditable(false);
        entrySlotDisplayTextField1.setFocusable(false);
        entrySlotDisplayTextField1.setFont(resourceMap.getFont("entrySlotDisplayTextField1.font")); // NOI18N
        entrySlotDisplayTextField1.setName("entrySlotDisplayTextField1"); // NOI18N
        entrySlotDisplayTextField1.setRequestFocusEnabled(false);

        attendeeCountLabel1.setText(resourceMap.getString("attendeeCountLabel1.text")); // NOI18N
        attendeeCountLabel1.setFocusable(false);
        attendeeCountLabel1.setName("attendeeCountLabel1"); // NOI18N

        statusDisplayTextField1.setEditable(false);
        statusDisplayTextField1.setFocusable(false);
        statusDisplayTextField1.setFont(resourceMap.getFont("statusDisplayTextField1.font")); // NOI18N
        statusDisplayTextField1.setName("statusDisplayTextField1"); // NOI18N

        attendeeCountLabel2.setText(resourceMap.getString("attendeeCountLabel2.text")); // NOI18N
        attendeeCountLabel2.setFocusable(false);
        attendeeCountLabel2.setName("attendeeCountLabel2"); // NOI18N

        localAttendeeCountTextField.setEditable(false);
        localAttendeeCountTextField.setText(resourceMap.getString("localAttendeeCountTextField.text")); // NOI18N
        localAttendeeCountTextField.setFocusable(false);
        localAttendeeCountTextField.setFont(resourceMap.getFont("localAttendeeCountTextField.font")); // NOI18N
        localAttendeeCountTextField.setName("localAttendeeCountTextField"); // NOI18N
        localAttendeeCountTextField.setRequestFocusEnabled(false);

        statusLabel1.setText(resourceMap.getString("statusLabel1.text")); // NOI18N
        statusLabel1.setFocusable(false);
        statusLabel1.setName("statusLabel1"); // NOI18N

        entrySlotLabel1.setText(resourceMap.getString("entrySlotLabel1.text")); // NOI18N
        entrySlotLabel1.setFocusable(false);
        entrySlotLabel1.setName("entrySlotLabel1"); // NOI18N

        thisMachineLabel.setText(resourceMap.getString("thisMachineLabel.text")); // NOI18N
        thisMachineLabel.setFocusable(false);
        thisMachineLabel.setName("thisMachineLabel"); // NOI18N

        totalAttendeeCountDisplay.setEditable(false);
        totalAttendeeCountDisplay.setText(resourceMap.getString("totalAttendeeCountDisplay.text")); // NOI18N
        totalAttendeeCountDisplay.setFocusable(false);
        totalAttendeeCountDisplay.setFont(resourceMap.getFont("totalAttendeeCountDisplay.font")); // NOI18N
        totalAttendeeCountDisplay.setName("totalAttendeeCountDisplay"); // NOI18N
        totalAttendeeCountDisplay.setRequestFocusEnabled(false);

        refreshAttendeesButton.setIcon(resourceMap.getIcon("refreshAttendeesButton.icon")); // NOI18N
        refreshAttendeesButton.setText(resourceMap.getString("refreshAttendeesButton.text")); // NOI18N
        refreshAttendeesButton.setToolTipText(resourceMap.getString("refreshAttendeesButton.toolTipText")); // NOI18N
        refreshAttendeesButton.setFocusable(false);
        refreshAttendeesButton.setName("refreshAttendeesButton"); // NOI18N
        refreshAttendeesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshAttendeesButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout bookingStatusPanelLayout = new javax.swing.GroupLayout(bookingStatusPanel);
        bookingStatusPanel.setLayout(bookingStatusPanelLayout);
        bookingStatusPanelLayout.setHorizontalGroup(
            bookingStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bookingStatusPanelLayout.createSequentialGroup()
                .addComponent(statusLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusDisplayTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(entrySlotLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(entrySlotDisplayTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(bookingStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(bookingStatusPanelLayout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addComponent(attendeeCountLabel1))
                    .addGroup(bookingStatusPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(thisMachineLabel)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(localAttendeeCountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(bookingStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(bookingStatusPanelLayout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addComponent(totalLabel))
                    .addGroup(bookingStatusPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(attendeeCountLabel2)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(totalAttendeeCountDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(refreshAttendeesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        bookingStatusPanelLayout.setVerticalGroup(
            bookingStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bookingStatusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(bookingStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(statusLabel1)
                    .addGroup(bookingStatusPanelLayout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addGroup(bookingStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(statusDisplayTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(entrySlotDisplayTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(localAttendeeCountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(thisMachineLabel)))
                    .addComponent(entrySlotLabel1)
                    .addComponent(attendeeCountLabel1)
                    .addGroup(bookingStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(bookingStatusPanelLayout.createSequentialGroup()
                            .addComponent(attendeeCountLabel2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(totalLabel))
                        .addComponent(totalAttendeeCountDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(refreshAttendeesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(6, Short.MAX_VALUE))
        );

        historyLabel.setText(resourceMap.getString("historyLabel.text")); // NOI18N
        historyLabel.setName("historyLabel"); // NOI18N

        jSeparator1.setName("jSeparator1"); // NOI18N

        eventStatusPanel.setName("eventStatusPanel"); // NOI18N

        eventStatusLabel.setFont(resourceMap.getFont("eventStatusLabel.font")); // NOI18N
        eventStatusLabel.setText(resourceMap.getString("eventStatusLabel.text")); // NOI18N
        eventStatusLabel.setName("eventStatusLabel"); // NOI18N

        eventStatusBookedLabel.setText(resourceMap.getString("eventStatusBookedLabel.text")); // NOI18N
        eventStatusBookedLabel.setName("eventStatusBookedLabel"); // NOI18N

        eventStatusAttendedLabel.setText(resourceMap.getString("eventStatusAttendedLabel.text")); // NOI18N
        eventStatusAttendedLabel.setName("eventStatusAttendedLabel"); // NOI18N

        eventStatusSavedLabel.setText(resourceMap.getString("eventStatusSavedLabel.text")); // NOI18N
        eventStatusSavedLabel.setName("eventStatusSavedLabel"); // NOI18N

        eventStatusUnsavedLabel.setText(resourceMap.getString("eventStatusUnsavedLabel.text")); // NOI18N
        eventStatusUnsavedLabel.setName("eventStatusUnsavedLabel"); // NOI18N

        eventStatusNotAttendedLabel.setText(resourceMap.getString("eventStatusNotAttendedLabel.text")); // NOI18N
        eventStatusNotAttendedLabel.setName("eventStatusNotAttendedLabel"); // NOI18N

        eventStatusNotAttendedLabel1.setText(resourceMap.getString("eventStatusNotAttendedLabel1.text")); // NOI18N
        eventStatusNotAttendedLabel1.setName("eventStatusNotAttendedLabel1"); // NOI18N

        eventStatusBookedNumber.setEditable(false);
        eventStatusBookedNumber.setText(resourceMap.getString("eventStatusBookedNumber.text")); // NOI18N
        eventStatusBookedNumber.setFocusable(false);
        eventStatusBookedNumber.setName("eventStatusBookedNumber"); // NOI18N
        eventStatusBookedNumber.setRequestFocusEnabled(false);

        eventStatusAttendedNumber.setEditable(false);
        eventStatusAttendedNumber.setText(resourceMap.getString("eventStatusAttendedNumber.text")); // NOI18N
        eventStatusAttendedNumber.setFocusable(false);
        eventStatusAttendedNumber.setName("eventStatusAttendedNumber"); // NOI18N
        eventStatusAttendedNumber.setRequestFocusEnabled(false);

        eventStatusSavedNumber.setEditable(false);
        eventStatusSavedNumber.setFont(resourceMap.getFont("eventStatusSavedNumber.font")); // NOI18N
        eventStatusSavedNumber.setForeground(resourceMap.getColor("eventStatusSavedNumber.foreground")); // NOI18N
        eventStatusSavedNumber.setText(resourceMap.getString("eventStatusSavedNumber.text")); // NOI18N
        eventStatusSavedNumber.setFocusable(false);
        eventStatusSavedNumber.setName("eventStatusSavedNumber"); // NOI18N
        eventStatusSavedNumber.setRequestFocusEnabled(false);

        eventStatusUnsavedNumber.setEditable(false);
        eventStatusUnsavedNumber.setFont(resourceMap.getFont("eventStatusUnsavedNumber.font")); // NOI18N
        eventStatusUnsavedNumber.setForeground(resourceMap.getColor("eventStatusUnsavedNumber.foreground")); // NOI18N
        eventStatusUnsavedNumber.setText(resourceMap.getString("eventStatusUnsavedNumber.text")); // NOI18N
        eventStatusUnsavedNumber.setFocusable(false);
        eventStatusUnsavedNumber.setName("eventStatusUnsavedNumber"); // NOI18N
        eventStatusUnsavedNumber.setRequestFocusEnabled(false);

        eventStatusNotAttendedNumber.setEditable(false);
        eventStatusNotAttendedNumber.setText(resourceMap.getString("eventStatusNotAttendedNumber.text")); // NOI18N
        eventStatusNotAttendedNumber.setFocusable(false);
        eventStatusNotAttendedNumber.setName("eventStatusNotAttendedNumber"); // NOI18N
        eventStatusNotAttendedNumber.setRequestFocusEnabled(false);

        eventStatusPlacesNumber.setEditable(false);
        eventStatusPlacesNumber.setText(resourceMap.getString("eventStatusPlacesNumber.text")); // NOI18N
        eventStatusPlacesNumber.setFocusable(false);
        eventStatusPlacesNumber.setName("eventStatusPlacesNumber"); // NOI18N
        eventStatusPlacesNumber.setRequestFocusEnabled(false);

        javax.swing.GroupLayout eventStatusPanelLayout = new javax.swing.GroupLayout(eventStatusPanel);
        eventStatusPanel.setLayout(eventStatusPanelLayout);
        eventStatusPanelLayout.setHorizontalGroup(
            eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(eventStatusPanelLayout.createSequentialGroup()
                .addGap(37, 37, 37)
                .addGroup(eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(eventStatusBookedLabel)
                    .addComponent(eventStatusAttendedLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(eventStatusPanelLayout.createSequentialGroup()
                        .addComponent(eventStatusBookedNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(19, 19, 19))
                    .addGroup(eventStatusPanelLayout.createSequentialGroup()
                        .addComponent(eventStatusAttendedNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)))
                .addGap(65, 65, 65)
                .addGroup(eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(eventStatusUnsavedLabel)
                    .addComponent(eventStatusSavedLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(eventStatusUnsavedNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(eventStatusSavedNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 118, Short.MAX_VALUE)
                .addGroup(eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(eventStatusNotAttendedLabel)
                    .addComponent(eventStatusNotAttendedLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(eventStatusNotAttendedNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(eventStatusPlacesNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addComponent(eventStatusLabel)
        );
        eventStatusPanelLayout.setVerticalGroup(
            eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(eventStatusPanelLayout.createSequentialGroup()
                .addGroup(eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(eventStatusPanelLayout.createSequentialGroup()
                        .addComponent(eventStatusLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(eventStatusBookedLabel)
                            .addComponent(eventStatusBookedNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(eventStatusSavedLabel)
                        .addComponent(eventStatusSavedNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(eventStatusNotAttendedNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(eventStatusNotAttendedLabel)))
                .addGap(18, 18, 18)
                .addGroup(eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(eventStatusUnsavedNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(eventStatusUnsavedLabel))
                    .addGroup(eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(eventStatusAttendedNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(eventStatusAttendedLabel))
                    .addGroup(eventStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(eventStatusPlacesNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(eventStatusNotAttendedLabel1)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout mainOnlinePanelLayout = new javax.swing.GroupLayout(mainOnlinePanel);
        mainOnlinePanel.setLayout(mainOnlinePanelLayout);
        mainOnlinePanelLayout.setHorizontalGroup(
            mainOnlinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainOnlinePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainOnlinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(bookingStatusScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 715, Short.MAX_VALUE)
                    .addComponent(eventStatusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 715, Short.MAX_VALUE)
                    .addGroup(mainOnlinePanelLayout.createSequentialGroup()
                        .addComponent(searchInputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(searchInput, javax.swing.GroupLayout.DEFAULT_SIZE, 356, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(searchButton, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(checkingModeToggle1, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(onlineModeToggle, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(bookingStatusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainOnlinePanelLayout.createSequentialGroup()
                        .addComponent(backButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 534, Short.MAX_VALUE)
                        .addComponent(finishButton, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(historyLabel))
                .addContainerGap())
        );
        mainOnlinePanelLayout.setVerticalGroup(
            mainOnlinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainOnlinePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainOnlinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(searchInput, javax.swing.GroupLayout.DEFAULT_SIZE, 53, Short.MAX_VALUE)
                    .addComponent(searchInputLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 53, Short.MAX_VALUE)
                    .addGroup(mainOnlinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(mainOnlinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(searchButton, javax.swing.GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE)
                            .addComponent(checkingModeToggle1, javax.swing.GroupLayout.DEFAULT_SIZE, 52, Short.MAX_VALUE))
                        .addComponent(onlineModeToggle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bookingStatusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addComponent(historyLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bookingStatusScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(eventStatusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainOnlinePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(backButton1)
                    .addComponent(finishButton))
                .addContainerGap())
        );

        searchInput.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S,
            java.awt.event.InputEvent.CTRL_DOWN_MASK), "save");
    searchInput.getActionMap().put("save", save);

    searchInput.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_R,
        java.awt.event.InputEvent.CTRL_DOWN_MASK), "toggleBooking");
searchInput.getActionMap().put("toggleBooking", toggleBooking);

searchInput.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_T,
    java.awt.event.InputEvent.CTRL_DOWN_MASK), "toggleConnection");
    searchInput.getActionMap().put("toggleConnection", toggleConnection);

    searchInput.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
        "enterStudentNumber");
    searchInput.getActionMap().put("enterStudentNumber",checkBooking);

    searchInput.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
        "refreshAttendees");
    searchInput.getActionMap().put("refreshAttendees",refreshAttendees);

    finishPanel.setMinimumSize(new java.awt.Dimension(720, 350));
    finishPanel.setName("finishPanel"); // NOI18N

    finishCloseButton.setAction(actionMap.get("finishAction")); // NOI18N
    finishCloseButton.setText(resourceMap.getString("finishCloseButton.text")); // NOI18N
    finishCloseButton.setName("finishCloseButton"); // NOI18N

    finishBackButton.setAction(actionMap.get("goBack")); // NOI18N
    finishBackButton.setText(resourceMap.getString("finishBackButton.text")); // NOI18N
    finishBackButton.setName("finishBackButton"); // NOI18N

    finishPanelTitle.setFont(resourceMap.getFont("finishPanelTitle.font")); // NOI18N
    finishPanelTitle.setText(resourceMap.getString("finishPanelTitle.text")); // NOI18N
    finishPanelTitle.setName("finishPanelTitle"); // NOI18N

    markAbsentOption.setFont(resourceMap.getFont("markAbsentOption.font")); // NOI18N
    markAbsentOption.setText(resourceMap.getString("markAbsentOption.text")); // NOI18N
    markAbsentOption.setName("markAbsentOption"); // NOI18N
    markAbsentOption.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            markAbsentOptionActionPerformed(evt);
        }
    });

    notifyAbsentOption.setFont(resourceMap.getFont("notifyAbsentOption.font")); // NOI18N
    notifyAbsentOption.setSelected(true);
    notifyAbsentOption.setText(resourceMap.getString("notifyAbsentOption.text")); // NOI18N
    notifyAbsentOption.setEnabled(false);
    notifyAbsentOption.setName("notifyAbsentOption"); // NOI18N

    javax.swing.GroupLayout finishPanelLayout = new javax.swing.GroupLayout(finishPanel);
    finishPanel.setLayout(finishPanelLayout);
    finishPanelLayout.setHorizontalGroup(
        finishPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(finishPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(finishPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(finishPanelLayout.createSequentialGroup()
                    .addComponent(markAbsentOption)
                    .addContainerGap())
                .addGroup(finishPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(finishPanelLayout.createSequentialGroup()
                        .addComponent(notifyAbsentOption)
                        .addContainerGap())
                    .addGroup(finishPanelLayout.createSequentialGroup()
                        .addComponent(finishBackButton, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 492, Short.MAX_VALUE)
                        .addComponent(finishCloseButton)
                        .addGap(20, 20, 20))
                    .addGroup(finishPanelLayout.createSequentialGroup()
                        .addComponent(finishPanelTitle)
                        .addContainerGap(460, Short.MAX_VALUE)))))
    );
    finishPanelLayout.setVerticalGroup(
        finishPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, finishPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(finishPanelTitle)
            .addGap(41, 41, 41)
            .addComponent(markAbsentOption)
            .addGap(18, 18, 18)
            .addComponent(notifyAbsentOption)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 177, Short.MAX_VALUE)
            .addGroup(finishPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(finishBackButton)
                .addComponent(finishCloseButton))
            .addContainerGap())
    );

    counterPanel.setMinimumSize(new java.awt.Dimension(720, 350));
    counterPanel.setName("counterPanel"); // NOI18N

    finishCountingButton.setAction(actionMap.get("finishCounting")); // NOI18N
    finishCountingButton.setText(resourceMap.getString("finishCountingButton.text")); // NOI18N
    finishCountingButton.setName("finishCountingButton"); // NOI18N

    counterBackButton.setAction(actionMap.get("goBack")); // NOI18N
    counterBackButton.setText(resourceMap.getString("counterBackButton.text")); // NOI18N
    counterBackButton.setName("counterBackButton"); // NOI18N

    counterPanelTitle.setFont(resourceMap.getFont("counterPanelTitle.font")); // NOI18N
    counterPanelTitle.setText(resourceMap.getString("counterPanelTitle.text")); // NOI18N
    counterPanelTitle.setName("counterPanelTitle"); // NOI18N

    counterDisplayPanel.setName("counterDisplayPanel"); // NOI18N

    counterThousands.setEditable(false);
    counterThousands.setForeground(resourceMap.getColor("counterThousands.foreground")); // NOI18N
    counterThousands.setText(resourceMap.getString("counterThousands.text")); // NOI18N
    counterThousands.setEnabled(false);
    counterThousands.setFocusable(false);
    counterThousands.setFont(resourceMap.getFont("counterUnits.font")); // NOI18N
    counterThousands.setName("counterThousands"); // NOI18N

    counterTens.setEditable(false);
    counterTens.setForeground(resourceMap.getColor("counterTens.foreground")); // NOI18N
    counterTens.setText(resourceMap.getString("counterTens.text")); // NOI18N
    counterTens.setEnabled(false);
    counterTens.setFocusable(false);
    counterTens.setFont(resourceMap.getFont("counterUnits.font")); // NOI18N
    counterTens.setName("counterTens"); // NOI18N

    counterUnits.setEditable(false);
    counterUnits.setForeground(resourceMap.getColor("counterUnits.foreground")); // NOI18N
    counterUnits.setText(resourceMap.getString("counterUnits.text")); // NOI18N
    counterUnits.setEnabled(false);
    counterUnits.setFocusable(false);
    counterUnits.setFont(resourceMap.getFont("counterUnits.font")); // NOI18N
    counterUnits.setName("counterUnits"); // NOI18N

    counterHundreds.setEditable(false);
    counterHundreds.setForeground(resourceMap.getColor("counterHundreds.foreground")); // NOI18N
    counterHundreds.setText(resourceMap.getString("counterHundreds.text")); // NOI18N
    counterHundreds.setEnabled(false);
    counterHundreds.setFocusable(false);
    counterHundreds.setFont(resourceMap.getFont("counterUnits.font")); // NOI18N
    counterHundreds.setName("counterHundreds"); // NOI18N

    counterTenThousands.setEditable(false);
    counterTenThousands.setForeground(resourceMap.getColor("counterTenThousands.foreground")); // NOI18N
    counterTenThousands.setText(resourceMap.getString("counterTenThousands.text")); // NOI18N
    counterTenThousands.setEnabled(false);
    counterTenThousands.setFocusable(false);
    counterTenThousands.setFont(resourceMap.getFont("counterUnits.font")); // NOI18N
    counterTenThousands.setName("counterTenThousands"); // NOI18N

    javax.swing.GroupLayout counterDisplayPanelLayout = new javax.swing.GroupLayout(counterDisplayPanel);
    counterDisplayPanel.setLayout(counterDisplayPanelLayout);
    counterDisplayPanelLayout.setHorizontalGroup(
        counterDisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(counterDisplayPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(counterTenThousands, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(counterThousands, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(counterHundreds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(counterTens, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(counterUnits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap())
    );
    counterDisplayPanelLayout.setVerticalGroup(
        counterDisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(counterDisplayPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(counterDisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                .addComponent(counterTenThousands, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)
                .addComponent(counterThousands, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)
                .addComponent(counterHundreds, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)
                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, counterDisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(counterTens)
                    .addComponent(counterUnits)))
            .addContainerGap())
    );

    countButton.setAction(actionMap.get("count")); // NOI18N
    countButton.setFont(resourceMap.getFont("countButton.font")); // NOI18N
    countButton.setText(resourceMap.getString("countButton.text")); // NOI18N
    countButton.setName("countButton"); // NOI18N

    resetCounterButton.setText(resourceMap.getString("resetCounterButton.text")); // NOI18N
    resetCounterButton.setName("resetCounterButton"); // NOI18N
    resetCounterButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            resetCounterButtonActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout counterPanelLayout = new javax.swing.GroupLayout(counterPanel);
    counterPanel.setLayout(counterPanelLayout);
    counterPanelLayout.setHorizontalGroup(
        counterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(counterPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(counterPanelTitle)
            .addContainerGap(535, Short.MAX_VALUE))
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, counterPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(counterBackButton, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(199, 199, 199)
            .addComponent(resetCounterButton)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 202, Short.MAX_VALUE)
            .addComponent(finishCountingButton)
            .addContainerGap())
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, counterPanelLayout.createSequentialGroup()
            .addGap(186, 186, 186)
            .addGroup(counterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                .addComponent(countButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 352, Short.MAX_VALUE)
                .addComponent(counterDisplayPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGap(185, 185, 185))
    );
    counterPanelLayout.setVerticalGroup(
        counterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(counterPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(counterPanelTitle)
            .addGap(74, 74, 74)
            .addComponent(counterDisplayPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(countButton, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 107, Short.MAX_VALUE)
            .addGroup(counterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(finishCountingButton)
                .addComponent(counterBackButton)
                .addComponent(resetCounterButton))
            .addContainerGap())
    );

    settingsPanel.setMinimumSize(new java.awt.Dimension(720, 350));
    settingsPanel.setName("settingsPanel"); // NOI18N

    doneSettingsButton.setAction(actionMap.get("saveSettings")); // NOI18N
    doneSettingsButton.setText(resourceMap.getString("doneSettingsButton.text")); // NOI18N
    doneSettingsButton.setName("doneSettingsButton"); // NOI18N

    settingsBackButton.setAction(actionMap.get("goBack")); // NOI18N
    settingsBackButton.setText(resourceMap.getString("settingsBackButton.text")); // NOI18N
    settingsBackButton.setName("settingsBackButton"); // NOI18N

    settingsPanelTitle.setFont(resourceMap.getFont("titleLabel.font")); // NOI18N
    settingsPanelTitle.setText(resourceMap.getString("settingsPanelTitle.text")); // NOI18N
    settingsPanelTitle.setName("settingsPanelTitle"); // NOI18N

    hostLabel.setText(resourceMap.getString("hostLabel.text")); // NOI18N
    hostLabel.setName("hostLabel"); // NOI18N

    hostInput.setText(resourceMap.getString("hostInput.text")); // NOI18N
    hostInput.setName("hostInput"); // NOI18N
    hostInput.setNextFocusableComponent(apiIdInput);

    apiIdLabel.setText(resourceMap.getString("apiIdLabel.text")); // NOI18N
    apiIdLabel.setName("apiIdLabel"); // NOI18N

    apiIdInput.setText(resourceMap.getString("apiIdInput.text")); // NOI18N
    apiIdInput.setName("apiIdInput"); // NOI18N
    apiIdInput.setNextFocusableComponent(apiSecretInput);

    apiSecretLabel.setText(resourceMap.getString("apiSecretLabel.text")); // NOI18N
    apiSecretLabel.setName("apiSecretLabel"); // NOI18N

    apiSecretInput.setText(resourceMap.getString("apiSecretInput.text")); // NOI18N
    apiSecretInput.setName("apiSecretInput"); // NOI18N

    studentIdFormatPanel.setName("studentIdFormatPanel"); // NOI18N

    studentIdFormatTitle.setFont(resourceMap.getFont("")); // NOI18N
    studentIdFormatTitle.setText(resourceMap.getString("studentIdFormatTitle.text")); // NOI18N
    studentIdFormatTitle.setName("studentIdFormatTitle"); // NOI18N

    studentIdFormatTypeButtonGroup.add(numbersRadio);
    numbersRadio.setText(resourceMap.getString("numbersRadio.text")); // NOI18N
    numbersRadio.setName("numbersRadio"); // NOI18N
    numbersRadio.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            regexRadioActionPerformed(evt);
        }
    });

    studentIdFormatTypeButtonGroup.add(lettersRadio);
    lettersRadio.setText(resourceMap.getString("lettersRadio.text")); // NOI18N
    lettersRadio.setName("lettersRadio"); // NOI18N
    lettersRadio.setNextFocusableComponent(lettersNumbersRadio);
    lettersRadio.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            regexRadioActionPerformed(evt);
        }
    });

    studentIdFormatTypeButtonGroup.add(lettersNumbersRadio);
    lettersNumbersRadio.setText(resourceMap.getString("lettersNumbersRadio.text")); // NOI18N
    lettersNumbersRadio.setName("lettersNumbersRadio"); // NOI18N
    lettersNumbersRadio.setNextFocusableComponent(fixedLengthCheckbox);
    lettersNumbersRadio.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            regexRadioActionPerformed(evt);
        }
    });

    studentIdLengthSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), null, Integer.valueOf(1)));
    studentIdLengthSpinner.setEnabled(false);
    studentIdLengthSpinner.setName("studentIdLengthSpinner"); // NOI18N
    studentIdLengthSpinner.setNextFocusableComponent(useCustomRegexCheckbox);
    studentIdLengthSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            studentIdLengthSpinnerStateChanged(evt);
        }
    });

    studentIdLengthLabel.setText(resourceMap.getString("studentIdLengthLabel.text")); // NOI18N
    studentIdLengthLabel.setEnabled(false);
    studentIdLengthLabel.setName("studentIdLengthLabel"); // NOI18N

    useCustomRegexCheckbox.setText(resourceMap.getString("useCustomRegexCheckbox.text")); // NOI18N
    useCustomRegexCheckbox.setName("useCustomRegexCheckbox"); // NOI18N
    useCustomRegexCheckbox.setNextFocusableComponent(regexInput);
    useCustomRegexCheckbox.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            useCustomRegexCheckboxActionPerformed(evt);
        }
    });

    regexInput.setEditable(false);
    regexInput.setText(resourceMap.getString("regexInput.text")); // NOI18N
    regexInput.setName("regexInput"); // NOI18N
    regexInput.setNextFocusableComponent(useDefaultLoginCheckbox);

    regexLabel.setText(resourceMap.getString("regexLabel.text")); // NOI18N
    regexLabel.setName("regexLabel"); // NOI18N

    fixedLengthCheckbox.setText(resourceMap.getString("fixedLengthCheckbox.text")); // NOI18N
    fixedLengthCheckbox.setName("fixedLengthCheckbox"); // NOI18N
    fixedLengthCheckbox.setNextFocusableComponent(studentIdLengthSpinner);
    fixedLengthCheckbox.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            fixedLengthCheckboxActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout studentIdFormatPanelLayout = new javax.swing.GroupLayout(studentIdFormatPanel);
    studentIdFormatPanel.setLayout(studentIdFormatPanelLayout);
    studentIdFormatPanelLayout.setHorizontalGroup(
        studentIdFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(studentIdFormatPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(studentIdFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(useCustomRegexCheckbox)
                .addGroup(studentIdFormatPanelLayout.createSequentialGroup()
                    .addComponent(regexLabel)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(regexInput, javax.swing.GroupLayout.DEFAULT_SIZE, 313, Short.MAX_VALUE))
                .addComponent(numbersRadio)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, studentIdFormatPanelLayout.createSequentialGroup()
                    .addGroup(studentIdFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(lettersNumbersRadio)
                        .addComponent(lettersRadio)
                        .addComponent(studentIdFormatTitle))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 132, Short.MAX_VALUE)
                    .addGroup(studentIdFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(fixedLengthCheckbox)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, studentIdFormatPanelLayout.createSequentialGroup()
                            .addComponent(studentIdLengthLabel)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(studentIdLengthSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)))))
            .addContainerGap())
    );
    studentIdFormatPanelLayout.setVerticalGroup(
        studentIdFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(studentIdFormatPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(studentIdFormatTitle)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(numbersRadio)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(studentIdFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(lettersRadio)
                .addComponent(fixedLengthCheckbox))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(studentIdFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(lettersNumbersRadio)
                .addComponent(studentIdLengthSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(studentIdLengthLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(useCustomRegexCheckbox)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(studentIdFormatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(regexInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(regexLabel))
            .addContainerGap(12, Short.MAX_VALUE))
    );

    defaultUsernameInput.setText(resourceMap.getString("defaultUsernameInput.text")); // NOI18N
    defaultUsernameInput.setEnabled(false);
    defaultUsernameInput.setName("defaultUsernameInput"); // NOI18N
    defaultUsernameInput.setNextFocusableComponent(defaultPasswordInput);

    defaultUsernameLabel.setText(resourceMap.getString("defaultUsernameLabel.text")); // NOI18N
    defaultUsernameLabel.setEnabled(false);
    defaultUsernameLabel.setName("defaultUsernameLabel"); // NOI18N

    defaultPasswordLabel.setText(resourceMap.getString("defaultPasswordLabel.text")); // NOI18N
    defaultPasswordLabel.setEnabled(false);
    defaultPasswordLabel.setName("defaultPasswordLabel"); // NOI18N

    defaultPasswordInput.setText(resourceMap.getString("defaultPasswordInput.text")); // NOI18N
    defaultPasswordInput.setEnabled(false);
    defaultPasswordInput.setName("defaultPasswordInput"); // NOI18N

    useDefaultLoginCheckbox.setText(resourceMap.getString("useDefaultLoginCheckbox.text")); // NOI18N
    useDefaultLoginCheckbox.setName("useDefaultLoginCheckbox"); // NOI18N
    useDefaultLoginCheckbox.setNextFocusableComponent(defaultUsernameInput);
    useDefaultLoginCheckbox.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            useDefaultLoginCheckboxActionPerformed(evt);
        }
    });

    resetSettingsButton.setAction(actionMap.get("resetSettings")); // NOI18N
    resetSettingsButton.setText(resourceMap.getString("resetSettingsButton.text")); // NOI18N
    resetSettingsButton.setName("resetSettingsButton"); // NOI18N

    javax.swing.GroupLayout settingsPanelLayout = new javax.swing.GroupLayout(settingsPanel);
    settingsPanel.setLayout(settingsPanelLayout);
    settingsPanelLayout.setHorizontalGroup(
        settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(settingsPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(settingsPanelTitle)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, settingsPanelLayout.createSequentialGroup()
                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(settingsBackButton, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(studentIdFormatPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, settingsPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(apiIdLabel)
                                    .addComponent(hostLabel)
                                    .addComponent(apiSecretLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(apiSecretInput)
                                    .addComponent(apiIdInput)
                                    .addComponent(hostInput, javax.swing.GroupLayout.PREFERRED_SIZE, 312, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(settingsPanelLayout.createSequentialGroup()
                            .addGap(23, 23, 23)
                            .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(settingsPanelLayout.createSequentialGroup()
                                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(defaultUsernameLabel)
                                        .addGroup(settingsPanelLayout.createSequentialGroup()
                                            .addGap(1, 1, 1)
                                            .addComponent(defaultPasswordLabel)))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(defaultPasswordInput, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                                        .addComponent(defaultUsernameInput, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)))
                                .addComponent(useDefaultLoginCheckbox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(resetSettingsButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(doneSettingsButton, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)))))
            .addContainerGap())
    );
    settingsPanelLayout.setVerticalGroup(
        settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, settingsPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(settingsPanelTitle)
            .addGap(26, 26, 26)
            .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(hostInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(hostLabel)
                .addComponent(useDefaultLoginCheckbox))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(apiIdLabel)
                .addComponent(apiIdInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(defaultUsernameInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(defaultUsernameLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(apiSecretLabel)
                .addComponent(apiSecretInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(defaultPasswordInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(defaultPasswordLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                .addComponent(studentIdFormatPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(resetSettingsButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(doneSettingsButton)
                .addComponent(settingsBackButton))
            .addContainerGap())
    );

    setComponent(preConfigPanel);
    setMenuBar(menuBar);
    addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            formPropertyChange(evt);
        }
    });
    }// </editor-fold>//GEN-END:initComponents
    
    private void toggleMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toggleMenuItemActionPerformed
        checkingModeToggle1.doClick();
    }//GEN-LAST:event_toggleMenuItemActionPerformed

    private void okConfigButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okConfigButton1ActionPerformed
        checkOnlineConfiguration();
    }//GEN-LAST:event_okConfigButton1ActionPerformed

    private void entrySlotsSpinnerOnlineStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_entrySlotsSpinnerOnlineStateChanged
        updateOnlineBookingPanel(true);
    }//GEN-LAST:event_entrySlotsSpinnerOnlineStateChanged

private void inputFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_inputFocusLost
    inputFocusChanged(evt, false);
}//GEN-LAST:event_inputFocusLost

private void inputFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_inputFocusGained
    inputFocusChanged(evt, true);
}//GEN-LAST:event_inputFocusGained

private void yesWaitingListRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yesWaitingListRadioButtonActionPerformed
    updateBookingPanel(true);
}//GEN-LAST:event_yesWaitingListRadioButtonActionPerformed

private void noWaitingListRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_noWaitingListRadioButtonActionPerformed
    updateBookingPanel(true);
}//GEN-LAST:event_noWaitingListRadioButtonActionPerformed

private void browseFileAction(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseFileAction
    JFileChooser fc = new JFileChooser();
    File file;
    fc.addChoosableFileFilter(new TextCSVFilter());
    int returnVal = fc.showDialog(app.getMainFrame(), "Choose");
    if (returnVal == JFileChooser.APPROVE_OPTION) {
        file = fc.getSelectedFile();
        JButton source = (JButton) evt.getSource();
        JFormattedTextField pathInput = (JFormattedTextField) this.getSlotView(source, Component.BROWSE)
                                                                  .allComponents.get(Component.FILE);
        pathInput.setText(file.getPath());
    }
}//GEN-LAST:event_browseFileAction

private void entrySlotsSpinnerOfflineStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_entrySlotsSpinnerOfflineStateChanged
    updateBookingPanel(true);
}//GEN-LAST:event_entrySlotsSpinnerOfflineStateChanged

private void okConfigButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okConfigButtonActionPerformed
    checkConfiguration();
}//GEN-LAST:event_okConfigButtonActionPerformed

private void noBookingRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_noBookingRadioButtonActionPerformed
    updateBookingPanel(yesBookingRadioButton.isSelected());
}//GEN-LAST:event_noBookingRadioButtonActionPerformed

private void yesBookingRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yesBookingRadioButtonActionPerformed
    updateBookingPanel(yesBookingRadioButton.isSelected());
}//GEN-LAST:event_yesBookingRadioButtonActionPerformed

private void loadEventButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadEventButtonActionPerformed
    JButton source = (JButton) evt.getSource();
    Slot slotView = this.getSlotView(source, Component.LOAD);
    JFormattedTextField idInput = (JFormattedTextField) slotView.allComponents.get(Component.ID);
    JTextField titleInput = (JTextField) slotView.allComponents.get(Component.TITLE);
    String id;
    id = idInput.getText();
    if (id.isEmpty() || id.equals(idInputDefault)) {
        JOptionPane.showMessageDialog(app.getMainFrame(),
                                      "You have not entered an entry slot ID!",
                                      "Event ID error",
                                      JOptionPane.ERROR_MESSAGE);
    }
    else {
        Event event;
        try {
            event = app.getEvent(id);
            titleInput.setText(event.getTitle());
        } catch (org.jsoup.HttpStatusException ex) {
            Logger.getLogger(EventSwipeView.class.getName()).log(Level.WARNING, null, ex);
            app.getLogger().logException(ex);
            JOptionPane.showMessageDialog(app.getMainFrame(),
                                          "No event with this ID was found!",
                                          "Event ID error",
                                          JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ex);
            app.getLogger().logException(ex);
            showGenericErrorMessage();
        }
    }
}//GEN-LAST:event_loadEventButtonActionPerformed

private void searchEventsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchEventsButtonActionPerformed
    List<Event> events = new ArrayList<>();
    try {
        events = app.getEvents("");
    } catch (IOException ex) {
        Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ex);
        app.getLogger().logException(ex);
        showGenericErrorMessage();
    }
    List<String> eventTitles = new ArrayList<>();
    for (Event event : events){
        eventTitles.add(event.getTitle());
    }
    JList eventList = new JList(eventTitles.toArray());
    eventList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    eventList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
    eventList.setVisibleRowCount(-1);
    JScrollPane eventListScroller = new JScrollPane(eventList);
    eventListScroller.setPreferredSize(new Dimension(500, 80));
    JOptionPane.showMessageDialog(null, eventListScroller, "Select event", JOptionPane.PLAIN_MESSAGE);
    int i = eventList.getSelectedIndex();
    if (i != -1) {
        Slot slotView = this.getSlotView((JButton) evt.getSource(), Component.SEARCH);
        JFormattedTextField idInput = (JFormattedTextField) slotView.allComponents.get(Component.ID);
        JTextField titleInput = (JTextField) slotView.allComponents.get(Component.TITLE);
        idInput.setText(events.get(i).getId());
        titleInput.setText(events.get(i).getTitle());
    }
}//GEN-LAST:event_searchEventsButtonActionPerformed

private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed
    searchInput.setEnabled(false);
    String input = searchInput.getText();
    if (!input.isEmpty()) {
        if (app.isValidId(input)) {
            Booking booking;
                try {
                    booking = app.processSearchInput(input);
                    updateBookingStatus(booking);
                } catch (EventFullException ef) {
                    app.getLogger().logException(ef);
                    eventFullDisplay(ef.getStuNum());
                } catch (EarlyRegistrationException er) {
                    app.getLogger().logException(er);
                    earlyRegistrationDisplay(input);
                } catch (IOException ex) {
                    Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ex);
                    app.getLogger().logException(ex);
                    showGenericErrorMessage();
                }
                finally {
                    searchInput.setText("");
                    searchInput.requestFocusInWindow();
                }
        }
        else {
            searchInput.setText("");
            searchInput.requestFocusInWindow();
            if (app.isOnlineMode()) {
                List<Student> students = new ArrayList<>();
                try {
                    input = URLEncoder.encode(input, app.getCharset());
                    students = app.getStudents(input);
                } catch (IOException ex) {
                    Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ex);
                    app.getLogger().logException(ex);
                    searchInput.setEnabled(true);
                    showGenericErrorMessage();
                }
                if (students.isEmpty()) {
                    JOptionPane.showMessageDialog(app.getMainFrame(),
                                                  "No students could be found.",
                                                  "Search error",
                                                  JOptionPane.ERROR_MESSAGE);
                }
                else {
                    List<String> studentNames = new ArrayList<>();
                    for (Student student : students){
                        studentNames.add(student.getFirstName() + " " +
                                         student.getLastName());
                    }
                    JList studentList = new JList(studentNames.toArray());
                    studentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    studentList.setLayoutOrientation(JList.VERTICAL_WRAP);
                    studentList.setVisibleRowCount(-1);
                    JScrollPane studentListScroller = new JScrollPane(studentList);
                    studentListScroller.setPreferredSize(new Dimension(500, 80));
                    JOptionPane.showMessageDialog(null, studentListScroller,
                                                  "Select student",
                                                  JOptionPane.PLAIN_MESSAGE);
                    int i = studentList.getSelectedIndex();
                    if (i != -1) {
                        String stuNum = students.get(i).getStuNumber();
                        if (stuNum.equals(app.getEmptyStuNumString())) {
                            JOptionPane.showMessageDialog(app.getMainFrame(),
                                                          "Attendee has no student number.",
                                                          "Student number error",
                                                          JOptionPane.ERROR_MESSAGE);
                        }
                        else {
                            searchInput.setText(students.get(i).getStuNumber());
                            searchButton.doClick();
                        }
                    }
                }
            }
            else {
                JOptionPane.showMessageDialog(app.getMainFrame(),
                                              "Not a valid student number!",
                                              "Student number error",
                                              JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    searchInput.setEnabled(true);
    
}//GEN-LAST:event_searchButtonActionPerformed

private void checkingModeToggle1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkingModeToggle1ActionPerformed
    toggleCheckingMode();
}//GEN-LAST:event_checkingModeToggle1ActionPerformed

private void onlineModeToggleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onlineModeToggleActionPerformed
    toggleOnlineMode();
}//GEN-LAST:event_onlineModeToggleActionPerformed

private void startOfflineButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startOfflineButtonActionPerformed
    app.setOnlineModeFlag(false);
    switchToPanel(configPanel);
}//GEN-LAST:event_startOfflineButtonActionPerformed

private void loginInputKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_loginInputKeyPressed
    KeyEvent ke = (KeyEvent) evt;
    if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
        logInButton.doClick();
    }
}//GEN-LAST:event_loginInputKeyPressed

private void idInputKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_idInputKeyPressed
    KeyEvent ke = (KeyEvent) evt;
    if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
        JButton loadButton = (JButton) this.getSlotView((JFormattedTextField) evt.getSource(), Component.ID)
                                           .allComponents.get(Component.LOAD);
        loadButton.doClick();
    }
}//GEN-LAST:event_idInputKeyPressed

private void markAbsentOptionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_markAbsentOptionActionPerformed
    notifyAbsentOption.setEnabled(markAbsentOption.isSelected());
}//GEN-LAST:event_markAbsentOptionActionPerformed

private void connectionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectionMenuItemActionPerformed
    onlineModeToggle.doClick();
}//GEN-LAST:event_connectionMenuItemActionPerformed

private void refreshAttendeesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshAttendeesButtonActionPerformed
    String currentCount = totalAttendeeCountDisplay.getText();
    try {
        totalAttendeeCountDisplay.setText(app.getAttendeeCount());
        updateEventStatus();
    } catch (IOException ex) {
        Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ex);
        app.getLogger().logException(ex);
        totalAttendeeCountDisplay.setText(currentCount);
    }
}//GEN-LAST:event_refreshAttendeesButtonActionPerformed

private void formPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_formPropertyChange
    // TODO add your handling code here:
}//GEN-LAST:event_formPropertyChange

private void startCounterModeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startCounterModeButtonActionPerformed
    this.switchToPanel(counterPanel);
    countButton.requestFocus();
}//GEN-LAST:event_startCounterModeButtonActionPerformed

private void resetCounterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetCounterButtonActionPerformed
    this.resetCount();
    countButton.requestFocus();
}//GEN-LAST:event_resetCounterButtonActionPerformed

private void logInButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logInButtonActionPerformed
    if (this.logIn(usernameInput, passwordInput)) {
       this.switchToPanel(onlineConfigPanel);
    }
}//GEN-LAST:event_logInButtonActionPerformed

private void useDefaultLoginCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useDefaultLoginCheckboxActionPerformed
    boolean enabled = useDefaultLoginCheckbox.isSelected();
    defaultUsernameInput.setEnabled(enabled);
    defaultUsernameLabel.setEnabled(enabled);
    defaultPasswordInput.setEnabled(enabled);
    defaultPasswordLabel.setEnabled(enabled);
    markDefaultField(defaultUsernameInput);
    markDefaultField(defaultPasswordInput);
}//GEN-LAST:event_useDefaultLoginCheckboxActionPerformed

private void fixedLengthCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fixedLengthCheckboxActionPerformed
    boolean enabled = fixedLengthCheckbox.isSelected();
    studentIdLengthLabel.setEnabled(enabled);
    studentIdLengthSpinner.setEnabled(enabled);
}//GEN-LAST:event_fixedLengthCheckboxActionPerformed

private void useCustomRegexCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useCustomRegexCheckboxActionPerformed
    boolean enabled = useCustomRegexCheckbox.isSelected();
    regexInput.setEditable(enabled);
    fixedLengthCheckbox.setEnabled(!enabled);
    studentIdLengthLabel.setEnabled(!enabled);
    studentIdLengthSpinner.setEnabled(!enabled);
    numbersRadio.setEnabled(!enabled);
    lettersRadio.setEnabled(!enabled);
    lettersNumbersRadio.setEnabled(!enabled);
}//GEN-LAST:event_useCustomRegexCheckboxActionPerformed

private void studentIdLengthSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_studentIdLengthSpinnerStateChanged
    Integer length = (Integer) studentIdLengthSpinner.getValue();
    String regex = "{" + length + "}";
    defaultRegexQuantifier = regex;
    this.setRegexValue(defaultRegexType, defaultRegexQuantifier);
}//GEN-LAST:event_studentIdLengthSpinnerStateChanged

private void regexRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_regexRadioActionPerformed
    String regex;
    JRadioButton source = (JRadioButton) evt.getSource();
    if (source.equals(numbersRadio)) {
        regex = "\\d";
    } else if(source.equals(lettersRadio)) {
        regex = "[A-Za-z]";
    } else {
        regex = "[A-Za-z\\d]";
    }
    defaultRegexType = regex;
    this.setRegexValue(defaultRegexType, defaultRegexQuantifier);
}//GEN-LAST:event_regexRadioActionPerformed
    
private boolean logIn(JTextField uField, JPasswordField pField) {
    String username = uField.getText();
    char[] password = pField.getPassword();
    pField.setText("");
    if (username.isEmpty() || username.equals(usernameInputDefault)) {
        JOptionPane.showMessageDialog(app.getMainFrame(),
          "Please enter your CareerHub username.",
          "No username",
          JOptionPane.ERROR_MESSAGE);
        uField.requestFocusInWindow();
    }
    else if (password.length == 0) {
        JOptionPane.showMessageDialog(app.getMainFrame(),
          "Please enter your CareerHub password.",
          "No password",
          JOptionPane.ERROR_MESSAGE);
        pField.requestFocusInWindow();
    }
    else {
        try {
            if(app.logIn(username, password)) {
                app.setOnlineModeFlag(true);
                app.setLoggedInFlag(true);
                return true;
            }
            else {
                JOptionPane.showMessageDialog(app.getMainFrame(),
                  "Your username or password were incorrect. " +
                  "Please try again",
                  "Login error",
                  JOptionPane.ERROR_MESSAGE);
                usernameInput.requestFocusInWindow();
            }
        } catch (NoPropertiesException np) {
            Object[] options = {"Go to settings", "Continue offline"};
            int reply = JOptionPane.showOptionDialog(app.getMainFrame(),
                  "You need to enter your booking system settings before using EventSwipe in online mode",
                  "No booking system settings",
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  options,
                  options[0]);
            if (reply == JOptionPane.YES_OPTION) {
                goToSettings();
            }
            else if(reply == JOptionPane.NO_OPTION) {
                switchToPanel(offlinePanel);
            }
        } catch (HeadlessException | IOException e) {
            JOptionPane.showMessageDialog(app.getMainFrame(),
              "There was a problem logging in. " +
              "Please check your internet connection and booking system settings and try again",
              "Login error",
              JOptionPane.ERROR_MESSAGE);
            uField.requestFocusInWindow();
            Logger.getLogger(EventSwipeApp.class.getName())
                .log(Level.SEVERE, "Login error", e);
        } finally {
            Arrays.fill(password,'0');
        }
    }
    return false;
}

    private void checkConfiguration() {
        app.clearData();
        int slots = (Integer) entrySlotsSpinnerOffline.getValue();
        boolean booking = yesBookingRadioButton.isSelected();
        boolean waitingList = yesWaitingListRadioButton.isSelected();
        app.setBookingFlag(booking);
        app.setWaitingListFlag(waitingList);
        app.setSlots(slots);
        boolean configOK = false;
        if (eventTitleInput.getText().equals(titleInputDefault)) {
            JOptionPane.showMessageDialog(app.getMainFrame(),
                                          "You have not entered an event title. Please write one!",
                                          "Event title error",
                                          JOptionPane.ERROR_MESSAGE);
        }
        else {
            app.setEventTitle(eventTitleInput.getText());
            configOK = true;
        }

        if (booking) {
            String[] filePaths = new String[slots];
            for (int i = slots - 1; i >= 0; i--) {
                JFormattedTextField fileInput = (JFormattedTextField) slotViews[i].allComponents.get(Component.FILE);
                filePaths[i] = fileInput.getText();
            }
            app.setEventsOffline(Arrays.asList(filePaths));
        }
        else {
            Event event = new Event();
            event.setTitle(eventTitleInput.getText());
            app.addEvent(event);
        }

        if (waitingList) {
            app.createWaitingList(waitingListFilePathInput.getText());
        }

        if (configOK) {
            app.createLog();
            enableOnlineComponents(false);
            switchToPanel(mainOnlinePanel);
        }
    }

    private void checkOnlineConfiguration() {
        app.clearData();
        int slots = (Integer) entrySlotsSpinnerOnline.getValue();
        boolean useWaitingList = yesLoadWaitingListRadioButton.isSelected();
        app.setWaitingListFlag(false);
        app.setSlots(slots);
        boolean configOK = false;
        String displayTitle = "";

        for (int i = 0; i < slots; i++) {
            JFormattedTextField idInput = (JFormattedTextField) slotViews[i].allComponents.get(Component.ID);
            String id = idInput.getText();
            if (!(id.equals(idInputDefault) || id.equals(""))) {
                try {
                    Event slot = app.loadEvent(id, useWaitingList);
                    int bookingCount = slot.getBookingCount(),
                        bookingLimit = slot.getBookingLimit();
                    if (slot.isDropIn()) {
                        Object[] options = {"Go to event", "Cancel"};
                        int reply = JOptionPane.showOptionDialog(app.getMainFrame(),
                              slot.getTitle() + " does not have booking enabled. " +
                              "You must enable booking (without a booking limit) before continuing.",
                              "Event booking error",
                              JOptionPane.YES_NO_OPTION,
                              JOptionPane.QUESTION_MESSAGE,
                              null,
                              options,
                              options[0]);
                        if (reply == JOptionPane.YES_OPTION) {
                            browseToUrl(app.getAdminEventURL(slot.getId()));
                        }
                        return;
                    }
                    else if(!slot.isUnlimited()) {
                        int spaces = bookingLimit - bookingCount;
                        String message;
                        Object[] options = {"Go to event", "Continue"};
                        if (spaces == 0) {
                            message = slot.getTitle() + " is fully booked. " +
                                      System.getProperty("line.separator") +
                                      "It is strongly recommended you remove the booking limit on CareerHub.";
                        }
                        else {
                            message = "You will only be able to book " +
                                      spaces + " extra " + 
                                      (spaces == 1 ? "attendee" : "attendees") +
                                      " for " + slot.getTitle() + "." +
                                      System.getProperty("line.separator") +
                                      "It is recommended you remove the booking limit on CareerHub.";
                        }
                        int reply = JOptionPane.showOptionDialog(app.getMainFrame(),
                              message,
                              "Booking limit warning",
                              JOptionPane.YES_NO_OPTION,
                              JOptionPane.QUESTION_MESSAGE,
                              null,
                              options,
                              options[0]);
                        if (reply == JOptionPane.YES_OPTION) {
                            browseToUrl(app.getAdminEventURL(slot.getId()));
                        }
                    }
                    String title = slot.getTitle();
                    JTextField titlePath = (JTextField) slotViews[i].allComponents.get(Component.TITLE);
                    titlePath.setText(title);
                    displayTitle += " - " + title;
                    configOK = true;
                } catch (Exception ex) {
                    configOK = false;
                    Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ex);
                    app.getLogger().logException(ex);
                }
            }
            else {
                configOK = false;
            }
        }
        if (configOK) {
            app.setEventTitle(displayTitle.substring(3));
            app.createLog();
            app.setBookingFlag(app.getBookedCount() > 0);
            enableOnlineComponents(true);
            connectionMenuItem.setEnabled(true);
            String totalAttendees = "0";
            try {
                totalAttendees = app.getAttendeeCount();
            } catch (IOException ex) {
                Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ex);
                app.getLogger().logException(ex);
            }
            totalAttendeeCountDisplay.setText(totalAttendees);
            switchToPanel(mainOnlinePanel);
        }
        else {
            JOptionPane.showMessageDialog(app.getMainFrame(),
                                          "There is something wrong with your event details. " +
                                          "Please try entering them again.",
                                          "Event configuration error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateEventStatus() {
        eventStatusBookedNumber.setText(app.getData().getBookingCount().toString());
        
        int remoteAttendance = app.getData().getAttendeeCount();
        attendeesDisplay = remoteAttendance > attendeesDisplay ?
                           remoteAttendance : attendeesDisplay;
        eventStatusAttendedNumber.setText(attendeesDisplay.toString());

        eventStatusSavedNumber.setText(app.getData().getSavedCount().toString());
        eventStatusUnsavedNumber.setText(app.getData().getUnsavedCount().toString());
        eventStatusNotAttendedNumber.setText(app.getData().getNotAttendedCount().toString());

        Integer places = app.getData().getCurrentNumberOfPlaces();
        eventStatusPlacesNumber.setText(places < 0 ? "Unlimited" : places.toString());
    }

    private void updateBookingStatus(Booking booking) {
        String stuNumber = booking.getStuNumber();
        String message = "Student " + stuNumber;
        Event event = app.getEvent();
        String slot = "N/A";
        if (!app.isSingleSlot() && null != booking.getSessionId()) {
            for (Session s: event.getSessions()) {
                if (s.getId().equals(booking.getSessionId())) {
                    slot = null != s.getStart() ?
                           Utils.getTimeString(s.getStart()) : s.getId();
                }
            }
        }
        slot = booking.isOnWaitingList() ? "W/L" : slot;
        String bookingStatus = "";
        if (booking.isAlreadyRecorded()) {
            Utils.successNoise();
            message += " has already been recorded";
            bookingStatus = "Already recorded";
        }
        else if (Objects.equals(booking.getStatus(), Booking.EARLY_STATUS)) {
            Utils.failureNoise();
            bookingStatus = "Too early";
            int reply = JOptionPane.showConfirmDialog(app.getMainFrame(),
                                                      "Student has arrived too early to register. "
                                                      + "Allow student to enter?",
                                                      "Student too early",
                                                      JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION) {
                app.addToEarlyList(stuNumber);
                bookingStatus = "Booked";
                message += " has been recorded";
            }
            else {
                message += " has arrived too early";
            }
        }
        else if(booking.isOnWaitingList()) {
            Utils.failureNoise();
            int reply = JOptionPane.showConfirmDialog(app.getMainFrame(),
                                                      "Student is on the waiting list. "
                                                      + "Allow student to enter?",
                                                      "Student on waiting list",
                                                      JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION) {
                try {
                    app.bookStudent(stuNumber, new Booking(stuNumber));
                    bookingStatus = "Recorded";
                    message += " has been recorded";
                } catch (EventFullException ef) {
                    eventFullDisplay(ef.getStuNum());
                    return;
                } catch (IOException ex) {
                    Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ex);
                    app.getLogger().logException(ex);
                    showGenericErrorMessage();
                }
            }
            else {
                bookingStatus = "Waiting list";
                message += " is on the waiting list";
            }
        }
        else if (booking.isBooked()) {
            Utils.successNoise();
            if (app.getBookingFlag()) {
                bookingStatus = "Booked";
                message += " has booked";
                if (app.getSlots() > 1) {
                    message += " for entry slot " + slot;
                }
            }
            else {
                bookingStatus = "Recorded";
                message += " has been recorded";
                
            }
        }
        else { //not booked
            Utils.failureNoise();
            int reply = JOptionPane.showConfirmDialog(app.getMainFrame(),
                                                      "Student is not booked. " +
                                                      "Allow student to enter?",
                                                      "Student not booked",
                                                      JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION) {
                try {
                    app.bookStudent(stuNumber, new Booking(stuNumber));
                    bookingStatus = "Recorded";
                    message += " has been recorded";
                } catch (NoStudentFoundException nsf) {
                    JOptionPane.showMessageDialog(app.getMainFrame(),
                                                  "Student " + nsf.getStuNum() +
                                                  " could not be found in CareerHub.",
                                                  "No student found",
                                                  JOptionPane.ERROR_MESSAGE);
                    message += " could not be found in CareerHub";
                    bookingStatus = "Not booked";
                } catch (EventFullException ef) {
                    eventFullDisplay(ef.getStuNum());
                    return;
                } catch (IOException ex) {
                    Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ex);
                    app.getLogger().logException(ex);
                    showGenericErrorMessage();
                }
            }
            else {
                message += " has NOT booked onto this event";
                bookingStatus = "Not booked";
            }
        }
        displayBookingStatus(bookingStatus, slot);
        displayBookingMessage(message);
        app.log(message);
        updateEventStatus();
    }

    private void displayBookingMessage(String message) {
        message += "\n";
        bookingStatusTextArea1.append(message);
        searchInput.setText("");
        searchInput.requestFocusInWindow();
    }

    private void displayBookingStatus(String statusMessage, String slot) {
        statusDisplayTextField1.setText(statusMessage);
        entrySlotDisplayTextField1.setText(slot);
        Color color;
        boolean enabled;
        switch (statusMessage) {
            case "Booked":
                color = Color.GREEN;
                enabled = true;
                localAttendeeCountTextField.setText(app.incrementLocalAttendeeCount());
                attendeesDisplay++;
                break;
            case "Recorded":
            case "":
                color = Color.WHITE;
                enabled = false;
                localAttendeeCountTextField.setText(app.incrementLocalAttendeeCount());
                attendeesDisplay++;
                break;
            default:
                enabled = false;
                color = statusMessage.equals("Not booked") ? Color.RED : Color.ORANGE;
                break;
        }
        statusDisplayTextField1.setBackground(color);
        entrySlotLabel1.setEnabled(enabled && !app.isSingleSlot());
        entrySlotDisplayTextField1.setEnabled(enabled && !app.isSingleSlot());
        int delay = 500; //milliseconds
        ActionListener taskPerformer = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                statusDisplayTextField1.setBackground(null);
            }
        };
        Timer timer = new Timer(delay, taskPerformer);
        timer.setRepeats(false);
        timer.start();
        if (app.isOnlineMode()){
            try {
                totalAttendeeCountDisplay.setText(app.getAttendeeCount());
            } catch (IOException ex) {
                Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ex);
                app.getLogger().logException(ex);
                totalAttendeeCountDisplay.setText(app.getLocalAttendeeCount());
                //showGenericErrorMessage();
            }
        }
    }

    private void eventFullDisplay(String stuNumber) {
        Utils.failureNoise();
        String message = "Student " + stuNumber + " couldn't be booked because the event is full";
        String bookingStatus = "EVENT FULL";
        String slot = "N/A";
        displayBookingStatus(bookingStatus, slot);
        displayBookingMessage(message);
        app.log(message);
        Object[] options = {"Go to event", "Cancel"};
        int reply = JOptionPane.showOptionDialog(app.getMainFrame(),
                                                 "There are no free spaces to book this student. " +
                                                 "Remove the booking limit on one of the entry slots before continuing.",
                                                 "Event full error",
                                                 JOptionPane.YES_NO_OPTION,
                                                 JOptionPane.QUESTION_MESSAGE,
                                                 null,
                                                 options,
                                                 options[0]);
        if (reply == JOptionPane.YES_OPTION) {
            try {
                browseToUrl(app.getAdminEventURL(app.getEvent().getId()));
            } catch (IOException ex) {
                Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ex);
                app.getLogger().logException(ex);
                showGenericErrorMessage();
            }
        }
    }

    private void earlyRegistrationDisplay(String stuNumber) {
        Utils.failureNoise();
        String message = "Student " + stuNumber + " is trying to register too early";
        String bookingStatus = "TOO EARLY";
        String slot = "N/A";
        displayBookingStatus(bookingStatus, slot);
        displayBookingMessage(message);
        app.log(message);
        JOptionPane.showMessageDialog(app.getMainFrame(),
                                          message,
                                          "Too early to register",
                                          JOptionPane.ERROR_MESSAGE);
    }

    private void switchToPanel(JPanel panel) {
        JFrame mainFrame = app.getMainFrame();
        panelStack.push((JPanel) mainFrame.getContentPane());
        mainFrame.setContentPane(panel);
        if (panel.equals(mainOnlinePanel)) {
            boolean online = app.isOnlineMode();
            boolean booking = app.getBookingFlag();
            searchInput.requestFocusInWindow();
            onlineModeToggle.setSelected(online);
            onlineModeToggle.setToolTipText(online ? onlineModeTooltipText : 
                                                     offlineModeTooltipText);
            checkingModeToggle1.setEnabled(booking);
            checkingModeToggle1.setSelected(booking);
            checkingModeToggle1.setText(booking ? checkingListsText :
                                                  recordingAllText);
            toggleMenuItem.setEnabled(booking);
            updateEventStatus();
        }
        panel.revalidate();
        mainFrame.repaint();
    }

    private void updateBookingPanel(boolean enabled) {
        Integer slots = (Integer)entrySlotsSpinnerOffline.getValue();
        entrySlotsSpinnerOnline.setValue(slots);
        bookingDetailsPanel.setEnabled(enabled);

        for (int i = EventSwipeData.MAX_ENTRY_SLOTS - 1; i >= 0; i--) {
            slotViews[i].enable(enabled && i < slots);
        }

        waitListView.enable(enabled &&
                            yesWaitingListRadioButton.isSelected());

        noWaitingListRadioButton.setEnabled(enabled);
        yesWaitingListRadioButton.setEnabled(enabled);
        waitingListLabel.setEnabled(enabled);

        entrySlotsLabel.setEnabled(enabled);
        entrySlotsSpinnerOffline.setEnabled(enabled);
        
        entrySlotLabel1.setEnabled(enabled);
        entrySlotDisplayTextField1.setText(enabled ? "": "N/A");
        entrySlotDisplayTextField1.setEnabled(enabled);
        checkingModeToggle1.setEnabled(enabled);
        checkingModeToggle1.setText(enabled ? checkingListsText :
                                              recordingAllText);
        toggleMenuItem.setEnabled(enabled);
    }

    private void updateOnlineBookingPanel(boolean enabled) {
        Integer slots = (Integer)entrySlotsSpinnerOnline.getValue();
        entrySlotsSpinnerOffline.setValue(slots);
        bookingDetailsPanel.setEnabled(enabled);

        for (int i = EventSwipeData.MAX_ENTRY_SLOTS - 1; i >= 0; i--) {
            slotViews[i].enable(enabled && i < slots);
        }

        slot1Label.setText(slots > 1 ? "Slot one" : "Event details");
        checkingModeToggle1.setEnabled(enabled);
        checkingModeToggle1.setText(enabled ? checkingListsText :
                                              recordingAllText);
        toggleMenuItem.setEnabled(enabled);
    }


    private void toggleCheckingMode() {
        if (checkingModeToggle1.isSelected()) {
            app.setBookingFlag(true);
            checkingModeToggle1.setText(checkingListsText);
        }
        else {
            app.setBookingFlag(false);
            checkingModeToggle1.setText(recordingAllText);
        }
    }

    private void enableOnlineComponents(Boolean enabled) {
        app.setOnlineModeFlag(enabled);
        totalAttendeeCountDisplay.setEnabled(enabled);
        refreshAttendeesButton.setEnabled(enabled);
        attendeeCountLabel2.setEnabled(enabled);
        totalLabel.setEnabled(enabled);
        onlineModeToggle.setToolTipText(enabled ? onlineModeTooltipText : offlineModeTooltipText);
        onlineModeToggle.setSelected(enabled);
    }

    private boolean logInFromDialog() {
        JPanel logInDialog = new JPanel();
        JLabel uLabel = new JLabel("CareerHub username:");
        JTextField uField = new JTextField(10);
        JLabel pLabel = new JLabel("CareerHub password:");
        JPasswordField pField = new JPasswordField(10);
        logInDialog.add(uLabel);
        logInDialog.add(uField);
        logInDialog.add(pLabel);
        logInDialog.add(pField);
        String[] options = new String[]{"Log in", "Cancel"};
        int option = JOptionPane.showOptionDialog(null, logInDialog,
             "Log in to CareerHub", JOptionPane.NO_OPTION,
             JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        if (option == 0) { // pressing log in button
           return logIn(uField, pField);
        }
        else {
           return false;
        }
    }

    private boolean eventNotNull() {
        try {
            Event e = app.getData().getEvent();
            return !e.getId().isEmpty();
        } catch (NullPointerException ex) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private void toggleOnlineMode() {
        if (onlineModeToggle.isSelected()) {
            boolean success = false;
            if (!app.getBookingFlag() || !eventNotNull()) {
                JOptionPane.showMessageDialog(app.getMainFrame(),
                      "Can't go online. No event IDs have been registered.",
                      "No event IDs",
                      JOptionPane.ERROR_MESSAGE);
            }
            else if(!app.isLoggedIn()) {
                logInFromDialog();
            }
            if (app.isLoggedIn()) {
                try {
                    app.goToOnlineMode();
                    String attendees = app.getAttendeeCount();
                    totalAttendeeCountDisplay.setText(attendees);
                    success = true;
                } catch (EventFullException ef) {
                    eventFullDisplay(ef.getStuNum());
                } catch (IOException ioe) {
                    Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ioe);
                    app.getLogger().logException(ioe);
                    JOptionPane.showMessageDialog(app.getMainFrame(),
                      "Can't connect to the internet.",
                      "Connection error",
                      JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ex);
                    app.getLogger().logException(ex);
                    JOptionPane.showMessageDialog(app.getMainFrame(),
                      "Something went wrong. Please use EventSwipe in offline mode.",
                      "Connection error",
                      JOptionPane.ERROR_MESSAGE);
                }
            }
            enableOnlineComponents(success);
        }
        else {
            enableOnlineComponents(false);
        }
    }

    private void inputFocusChanged(java.awt.event.FocusEvent evt, boolean gained) {
        JFormattedTextField input = (JFormattedTextField) evt.getSource();
        String cleanTitle = gained ? titleInputDefault : "";
        String cleanId = gained ? idInputDefault : "";
        String cleanFile = gained ? fileInputDefault : "";
        boolean isFileInput = (input == waitingListFilePathInput), isIdInput = false;
        if (input == eventTitleInput && input.getText().equals(cleanTitle))
            input.setText(gained ? "" : titleInputDefault);
        else if (!isFileInput) {
            Slot slotView = this.getSlotView(input);
            isFileInput = (input == slotView.allComponents.get(Component.FILE));
            isIdInput = (input == slotView.allComponents.get(Component.ID));
        }
        if (isFileInput && input.getText().equals(cleanFile)) {
            input.setText(gained ? "" : fileInputDefault);
        }
        else if (isIdInput && input.getText().equals(cleanId)) {
            input.setText(gained ? "" : idInputDefault);
        }
    }

    private void browseToUrl(String url) {
        if(!Desktop.isDesktopSupported()) {
            System.err.println("Desktop is not supported");
            showBrowserError();
        }
        else {
            Desktop desktop = Desktop.getDesktop();
            if(!desktop.isSupported(Desktop.Action.BROWSE)) {
                System.err.println("Desktop doesn't support the browse action");
                showBrowserError();
            }
            else {
                URI uri;
                try {
                    uri = new URI(url);
                    desktop.browse(uri);
                } catch (Exception ex) {
                    Logger.getLogger(EventSwipeView.class.getName()).log(Level.SEVERE, null, ex);
                    app.getLogger().logException(ex);
                    showBrowserError();
                }
            }
        }
    }

    private void showGenericErrorMessage() {
        if (!searchInput.isEnabled()) {
            searchInput.setEnabled(true);
        }
        JOptionPane.showMessageDialog(app.getMainFrame(),
                                      "Something has gone wrong! Close EventSwipe and log in again.",
                                      "Unexpected error",
                                      JOptionPane.ERROR_MESSAGE);
    }

    private void showBrowserError() {
        JOptionPane.showMessageDialog(app.getMainFrame(),
                                      "Can't open browser. Edit event manually.",
                                      "Browser error",
                                      JOptionPane.ERROR_MESSAGE);
    }

    private <T extends JComponent> Slot getSlotView(T component, Component type) {
        for (Slot s : slotViews) {
            if (s.allComponents.get(type).equals(component)) {
                return s;
            }
        }
        //TODO error throwing
        return slotViews[0];
    }

    private <T extends JComponent> Slot getSlotView(T component) {
        for (Slot s : slotViews) {
            if (s.allComponents.containsValue(component)) {
                return s;
            }
        }
        //TODO error throwing
        return slotViews[0];
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel aboutEventLabel;
    private javax.swing.JTextField apiIdInput;
    private javax.swing.JLabel apiIdLabel;
    private javax.swing.JTextField apiSecretInput;
    private javax.swing.JLabel apiSecretLabel;
    private javax.swing.JLabel attendeeCountLabel1;
    private javax.swing.JLabel attendeeCountLabel2;
    private javax.swing.JButton backButton1;
    private javax.swing.JPanel bookingDetailsPanel;
    private javax.swing.JPanel bookingDetailsPanel1;
    private javax.swing.JButton bookingListBrowseButton1;
    private javax.swing.JButton bookingListBrowseButton2;
    private javax.swing.JButton bookingListBrowseButton3;
    private javax.swing.JButton bookingListBrowseButton4;
    private javax.swing.JButton bookingListBrowseButton5;
    private javax.swing.JPanel bookingStatusPanel;
    private javax.swing.JScrollPane bookingStatusScrollPane1;
    private javax.swing.JTextArea bookingStatusTextArea1;
    private javax.swing.JToggleButton checkingModeToggle1;
    private javax.swing.JButton configBackButton;
    private javax.swing.JPanel configPanel;
    private javax.swing.JMenuItem connectionMenuItem;
    private javax.swing.JButton countButton;
    private javax.swing.JButton counterBackButton;
    private javax.swing.JPanel counterDisplayPanel;
    private javax.swing.JFormattedTextField counterHundreds;
    private javax.swing.JPanel counterPanel;
    private javax.swing.JLabel counterPanelTitle;
    private javax.swing.JFormattedTextField counterTenThousands;
    private javax.swing.JFormattedTextField counterTens;
    private javax.swing.JFormattedTextField counterThousands;
    private javax.swing.JFormattedTextField counterUnits;
    private javax.swing.JPasswordField defaultPasswordInput;
    private javax.swing.JLabel defaultPasswordLabel;
    private javax.swing.JTextField defaultUsernameInput;
    private javax.swing.JLabel defaultUsernameLabel;
    private javax.swing.JButton doneSettingsButton;
    private javax.swing.JPanel entrySlot1Panel;
    private javax.swing.JPanel entrySlot2Panel;
    private javax.swing.JPanel entrySlot3Panel;
    private javax.swing.JPanel entrySlot4Panel;
    private javax.swing.JPanel entrySlot5Panel;
    private javax.swing.JFormattedTextField entrySlotBookingListFilePathInput1;
    private javax.swing.JFormattedTextField entrySlotBookingListFilePathInput2;
    private javax.swing.JFormattedTextField entrySlotBookingListFilePathInput3;
    private javax.swing.JFormattedTextField entrySlotBookingListFilePathInput4;
    private javax.swing.JFormattedTextField entrySlotBookingListFilePathInput5;
    private javax.swing.JLabel entrySlotBookingListLabel1;
    private javax.swing.JLabel entrySlotBookingListLabel2;
    private javax.swing.JLabel entrySlotBookingListLabel3;
    private javax.swing.JLabel entrySlotBookingListLabel4;
    private javax.swing.JLabel entrySlotBookingListLabel5;
    private javax.swing.JFormattedTextField entrySlotDisplayTextField1;
    private javax.swing.JFormattedTextField entrySlotIdInput1;
    private javax.swing.JFormattedTextField entrySlotIdInput2;
    private javax.swing.JFormattedTextField entrySlotIdInput3;
    private javax.swing.JFormattedTextField entrySlotIdInput4;
    private javax.swing.JFormattedTextField entrySlotIdInput5;
    private javax.swing.JLabel entrySlotIdLabel1;
    private javax.swing.JLabel entrySlotIdLabel2;
    private javax.swing.JLabel entrySlotIdLabel3;
    private javax.swing.JLabel entrySlotIdLabel4;
    private javax.swing.JLabel entrySlotIdLabel5;
    private javax.swing.JLabel entrySlotLabel1;
    private javax.swing.JLabel entrySlotsLabel;
    private javax.swing.JLabel entrySlotsLabel1;
    private javax.swing.JSpinner entrySlotsSpinnerOffline;
    private javax.swing.JSpinner entrySlotsSpinnerOnline;
    private javax.swing.JLabel eventStatusAttendedLabel;
    private javax.swing.JTextField eventStatusAttendedNumber;
    private javax.swing.JLabel eventStatusBookedLabel;
    private javax.swing.JTextField eventStatusBookedNumber;
    private javax.swing.JLabel eventStatusLabel;
    private javax.swing.JLabel eventStatusNotAttendedLabel;
    private javax.swing.JLabel eventStatusNotAttendedLabel1;
    private javax.swing.JTextField eventStatusNotAttendedNumber;
    private javax.swing.JPanel eventStatusPanel;
    private javax.swing.JTextField eventStatusPlacesNumber;
    private javax.swing.JLabel eventStatusSavedLabel;
    private javax.swing.JTextField eventStatusSavedNumber;
    private javax.swing.JLabel eventStatusUnsavedLabel;
    private javax.swing.JTextField eventStatusUnsavedNumber;
    private javax.swing.JFormattedTextField eventTitleInput;
    private javax.swing.JLabel eventTitleInputLabel;
    private javax.swing.JPanel eventTitlePanel;
    private javax.swing.JButton finishBackButton;
    private javax.swing.JButton finishButton;
    private javax.swing.JButton finishCloseButton;
    private javax.swing.JButton finishCountingButton;
    private javax.swing.JPanel finishPanel;
    private javax.swing.JLabel finishPanelTitle;
    private javax.swing.JCheckBox fixedLengthCheckbox;
    private javax.swing.JTextField generatedTitle1;
    private javax.swing.JTextField generatedTitle2;
    private javax.swing.JTextField generatedTitle3;
    private javax.swing.JTextField generatedTitle4;
    private javax.swing.JTextField generatedTitle5;
    private javax.swing.JLabel generatedTitleLabel1;
    private javax.swing.JLabel generatedTitleLabel2;
    private javax.swing.JLabel generatedTitleLabel3;
    private javax.swing.JLabel generatedTitleLabel4;
    private javax.swing.JLabel generatedTitleLabel5;
    private javax.swing.JLabel historyLabel;
    private javax.swing.JTextField hostInput;
    private javax.swing.JLabel hostLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JRadioButton lettersNumbersRadio;
    private javax.swing.JRadioButton lettersRadio;
    private javax.swing.JButton loadEventButton1;
    private javax.swing.JButton loadEventButton2;
    private javax.swing.JButton loadEventButton3;
    private javax.swing.JButton loadEventButton4;
    private javax.swing.JButton loadEventButton5;
    private javax.swing.JLabel loadWaitingListLabel;
    private javax.swing.JFormattedTextField localAttendeeCountTextField;
    private javax.swing.JButton logInButton;
    private javax.swing.JLabel logInLabel;
    private javax.swing.JPanel loginFormPanel;
    private javax.swing.JSeparator loginOfflineSeparator;
    private javax.swing.JPanel mainOnlinePanel;
    private javax.swing.JLabel mainTitle;
    private javax.swing.JCheckBox markAbsentOption;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JRadioButton noBookingRadioButton;
    private javax.swing.JRadioButton noLoadWaitingListRadioButton;
    private javax.swing.JRadioButton noWaitingListRadioButton;
    private javax.swing.JCheckBox notifyAbsentOption;
    private javax.swing.JRadioButton numbersRadio;
    private javax.swing.JSeparator offlineCounterSeparator;
    private javax.swing.JPanel offlinePanel;
    private javax.swing.JButton okConfigButton;
    private javax.swing.JButton okConfigButton1;
    private javax.swing.JButton onlineConfigBackButton;
    private javax.swing.JPanel onlineConfigPanel;
    private javax.swing.JToggleButton onlineModeToggle;
    private javax.swing.ButtonGroup onlineWaitingListButtonGroup;
    private javax.swing.JPasswordField passwordInput;
    private javax.swing.JLabel passwordLabel;
    private javax.swing.JPanel preConfigPanel;
    private javax.swing.JButton refreshAttendeesButton;
    private javax.swing.JTextField regexInput;
    private javax.swing.JLabel regexLabel;
    private javax.swing.ButtonGroup requireBookingButtonGroup;
    private javax.swing.JLabel requireBookingLabel;
    private javax.swing.JPanel requireBookingPanel;
    private javax.swing.JButton resetCounterButton;
    private javax.swing.JButton resetSettingsButton;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JButton searchButton;
    private javax.swing.JButton searchEventsButton1;
    private javax.swing.JButton searchEventsButton2;
    private javax.swing.JButton searchEventsButton3;
    private javax.swing.JButton searchEventsButton4;
    private javax.swing.JButton searchEventsButton5;
    private javax.swing.JFormattedTextField searchInput;
    private javax.swing.JLabel searchInputLabel;
    private javax.swing.JButton settingsBackButton;
    private javax.swing.JMenuItem settingsMenuItem;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JLabel settingsPanelTitle;
    private javax.swing.JPanel slot1DetailsPanel;
    private javax.swing.JLabel slot1Label;
    private javax.swing.JPanel slot2DetailsPanel;
    private javax.swing.JLabel slot2Label;
    private javax.swing.JPanel slot3DtailsPanel;
    private javax.swing.JLabel slot3Label;
    private javax.swing.JPanel slot4DetailsPanel;
    private javax.swing.JLabel slot4Label;
    private javax.swing.JPanel slot5DetailsPanel;
    private javax.swing.JLabel slot5Label;
    private javax.swing.JPanel slotsPanel;
    private javax.swing.JLabel smallLogoLabel;
    private javax.swing.JButton startCounterModeButton;
    private javax.swing.JPanel startCounterModePanel;
    private javax.swing.JButton startOfflineButton;
    private javax.swing.JFormattedTextField statusDisplayTextField1;
    private javax.swing.JLabel statusLabel1;
    private javax.swing.JPanel studentIdFormatPanel;
    private javax.swing.JLabel studentIdFormatTitle;
    private javax.swing.ButtonGroup studentIdFormatTypeButtonGroup;
    private javax.swing.JLabel studentIdLengthLabel;
    private javax.swing.JSpinner studentIdLengthSpinner;
    private javax.swing.JLabel thisMachineLabel;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JPanel titleLoginPanel;
    private javax.swing.JPanel titlePanel;
    private javax.swing.JMenuItem toggleMenuItem;
    private javax.swing.JFormattedTextField totalAttendeeCountDisplay;
    private javax.swing.JLabel totalLabel;
    private javax.swing.JCheckBox useCustomRegexCheckbox;
    private javax.swing.JCheckBox useDefaultLoginCheckbox;
    private javax.swing.JTextArea useOfflineDescription;
    private javax.swing.JTextArea useOfflineDescription1;
    private javax.swing.JLabel useOfflineLabel;
    private javax.swing.JTextField usernameInput;
    private javax.swing.JLabel usernameLabel;
    private javax.swing.JLabel versionLabel;
    private javax.swing.JButton waitingListBrowseButton;
    private javax.swing.ButtonGroup waitingListButtonGroup;
    private javax.swing.JLabel waitingListFileLabel;
    private javax.swing.JPanel waitingListFilePanel;
    private javax.swing.JFormattedTextField waitingListFilePathInput;
    private javax.swing.JLabel waitingListLabel;
    private javax.swing.JPanel waitingListOptionsPanel;
    private javax.swing.JPanel waitingListRadioPanel;
    private javax.swing.JRadioButton yesBookingRadioButton;
    private javax.swing.JRadioButton yesLoadWaitingListRadioButton;
    private javax.swing.JRadioButton yesWaitingListRadioButton;
    // End of variables declaration//GEN-END:variables
    private JDialog aboutBox;
    private String titleInputDefault;
    private String fileInputDefault;
    private String idInputDefault;
    private String usernameInputDefault;
    private String recordingAllText;
    private String checkingListsText;
    private String onlineModeTooltipText;
    private String offlineModeTooltipText;
    private Integer attendeesDisplay = 0;

    private String defaultRegexType = ".";
    private String defaultRegexQuantifier = "+";

    private void setRegexValue(String type, String quantifier) {
        regexInput.setText("^" + type + quantifier + "$");
    }

    private void markErrorField(JComponent field) {
        field.setBorder(BorderFactory.createLineBorder(Color.red));
    }

    private void markDefaultField(JComponent field) {
        field.setBorder(UIManager.getBorder("TextField.border"));
    }

    private boolean markEmptyFields(JTextField[] required) {
        boolean complete = true;
        for (JTextField r : required) {
            if (r.getText().isEmpty() || r.getText().matches("^\\s+$")) {
                markErrorField(r);
                complete = false;
            }
            else {
                markDefaultField(r);
            }
        }
        return complete;
    }

    private void buildCounterMap() {
        countComponents.put(CountComponent.UNITS, counterUnits);
        countComponents.put(CountComponent.TENS, counterTens);
        countComponents.put(CountComponent.HUNDREDS, counterHundreds);
        countComponents.put(CountComponent.THOUSANDS, counterThousands);
        countComponents.put(CountComponent.TEN_THOUSANDS, counterTenThousands);
    }

    private void displayCount(Integer count) {
        String countStr = count.toString();
        int i = 0;
        switch (countStr.length()) {
            case 5:
                counterTenThousands.setEnabled(true);
                counterTenThousands.setText(countStr.substring(i++, i));
            case 4:
                counterThousands.setEnabled(true);
                counterThousands.setText(countStr.substring(i++, i));
            case 3:
                counterHundreds.setEnabled(true);
                counterHundreds.setText(countStr.substring(i++, i));
            case 2:
                counterTens.setEnabled(true);
                counterTens.setText(countStr.substring(i++, i));
            case 1:
                counterUnits.setEnabled(true);
                counterUnits.setText(countStr.substring(i));
                break;
            default:
                this.resetCount();
        }
    }

    private void resetCount() {
        app.resetCounter();
        this.resetCountDisplay();
    }

    private void resetCountDisplay() {
        for (JFormattedTextField c : countComponents.values()) {
            c.setEnabled(false);
            c.setText("0");
        }
    }

    private void initSlotViews() {
        JFormattedTextField[] fileInputs = {
            entrySlotBookingListFilePathInput1,
            entrySlotBookingListFilePathInput2,
            entrySlotBookingListFilePathInput3,
            entrySlotBookingListFilePathInput4,
            entrySlotBookingListFilePathInput5
        };
        JFormattedTextField[] idInputs = {
            entrySlotIdInput1,
            entrySlotIdInput2,
            entrySlotIdInput3,
            entrySlotIdInput4,
            entrySlotIdInput5
        };
        JButton[] browseButtons = {
            bookingListBrowseButton1,
            bookingListBrowseButton2,
            bookingListBrowseButton3,
            bookingListBrowseButton4,
            bookingListBrowseButton5
        };
        JButton[] loadEventButtons = {
            loadEventButton1,
            loadEventButton2,
            loadEventButton3,
            loadEventButton4,
            loadEventButton5
        };
        JButton[] listEventButtons = {
            searchEventsButton1,
            searchEventsButton2,
            searchEventsButton3,
            searchEventsButton4,
            searchEventsButton5
        };
        JTextField[] titleInputs = {
            generatedTitle1,
            generatedTitle2,
            generatedTitle3,
            generatedTitle4,
            generatedTitle5
        };
        JLabel[] slotLabels = {
            slot1Label,
            slot2Label,
            slot3Label,
            slot4Label,
            slot5Label
        };
        JLabel[] bookingListLabels = {
            entrySlotBookingListLabel1,
            entrySlotBookingListLabel2,
            entrySlotBookingListLabel3,
            entrySlotBookingListLabel4,
            entrySlotBookingListLabel5
        };
        for (int i = 0; i < EventSwipeData.MAX_ENTRY_SLOTS; i++) {
            slotViews[i] = new Slot(fileInputs[i], idInputs[i], browseButtons[i],
                                    loadEventButtons[i], listEventButtons[i],
                                    titleInputs[i], slotLabels[i], bookingListLabels[i]);
        }
        waitListView = new WaitingListView(waitingListFilePathInput,
                                           waitingListBrowseButton,
                                           waitingListFileLabel);
    }

    private class Slot {
        Slot() {}
        Slot(JFormattedTextField f,
             JFormattedTextField i,
                         JButton b,
                         JButton l,
                         JButton s,
                      JTextField t,
                        JLabel lab,
                        JLabel bLab) {
            allComponents.put(Component.FILE, f);
            allComponents.put(Component.ID, i);
            allComponents.put(Component.BROWSE, b);
            allComponents.put(Component.LOAD, l);
            allComponents.put(Component.SEARCH, s);
            allComponents.put(Component.TITLE, t);
            allComponents.put(Component.LABEL, lab);
            allComponents.put(Component.BOOKINGLIST, bLab);
        }

        EnumMap<Component,JComponent> allComponents = new EnumMap(Component.class);

        void enable(Boolean e) {
            for (JComponent c : allComponents.values()) {
                if(c != allComponents.get(Component.TITLE))
                    c.setEnabled(e);
            }
        }
        
    }

    private class WaitingListView extends Slot {
        WaitingListView(JFormattedTextField f,
                                    JButton b,
                                   JLabel bLab) {
            allComponents.put(Component.FILE, f);
            allComponents.put(Component.BROWSE, b);
            allComponents.put(Component.BOOKINGLIST, bLab);
        }
    }

    public enum CountComponent {
        UNITS, TENS, HUNDREDS, THOUSANDS, TEN_THOUSANDS
    }

    public enum Component {
        FILE, ID, BROWSE, LOAD, SEARCH,
        TITLE, LABEL, BOOKINGLIST
    }

    private Slot[] slotViews = new Slot[EventSwipeData.MAX_ENTRY_SLOTS];
    private WaitingListView waitListView;

    private EnumMap<CountComponent,JFormattedTextField> countComponents =
            new EnumMap(CountComponent.class);

    private Stack<JPanel> panelStack;

    //End of manually declared variables

    private EventSwipeApp app = EventSwipeApp.getApplication();

}
