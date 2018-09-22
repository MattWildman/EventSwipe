import eventswipe.exceptions.NoPropertiesException;
import java.util.HashMap;
import java.util.Map;
import eventswipe.EventSwipeApp;
import eventswipe.EventSwipeData;
import java.util.Properties;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author wildmanm
 */
public class PropertiesTest {

    public EventSwipeApp app;
    public Map<String,String> pMap;

    public PropertiesTest() {
        app = new EventSwipeApp();
        pMap = new HashMap<String,String>();
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
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
    public void createPropertiesTest() {
        pMap.put("studentIdPattern", "^\\d+$");
        pMap.put("secret", "+W1qOnJB/WZo4mWoGhQcb6Dwpaao3Xom2QVOPaSB0n0=");
        pMap.put("password", "miketiley");
        pMap.put("host", "https://careers.lse.ac.uk/");
        pMap.put("username", "eventswipe");
        pMap.put("id", "lse.careers");
        try {
            app.saveProperties(pMap);
        } catch (NoPropertiesException npe) {
            fail();
        }
    }

    @Test
    public void getPropertiesTest() {
        try {
            Properties p = app.getProperties(EventSwipeData.API_PROPERITES_PATH);
            p.list(System.out);
            assert(p.size() > 0);
        } catch (IOException ex) {
            Logger.getLogger(PropertiesTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
    }

    @Test
    public void getValueTest() {
        try {
            Properties p = app.getProperties(EventSwipeData.API_PROPERITES_PATH);
            assertEquals("Incorrect app id", "lse.careers", p.getProperty(EventSwipeData.API_ID_KEY));
        } catch (IOException ex) {
            Logger.getLogger(PropertiesTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
    }

    @Test
    public void deletePropertiesTest() throws Exception {
        app.clearProperties();
        Properties p = app.getProperties(EventSwipeData.API_PROPERITES_PATH);
        p.list(System.out);
        assertEquals("Properties not cleared", "default", p.getProperty(EventSwipeData.STATUS_KEY));
        assert(!app.getData().isPropertiesFlag());
    }

}