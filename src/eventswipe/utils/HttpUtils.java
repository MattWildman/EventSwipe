package eventswipe.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HttpUtils {

    public static CookieManager cm;

    public static void setCookiePolicy() {
        cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cm);
    }

    public static HttpURLConnection connectToURL(String url, 
                                                 String method,
                                                 Map<String, String> headers) throws MalformedURLException, IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setRequestMethod(method);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        if (!headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        connection.connect();
        return connection;
    }

    public static String sendDataToURL(String url,
                                       String method,
                                       String data,
                                       String charset,
                                       Map<String,String> headers) throws MalformedURLException, IOException {
        HttpURLConnection connection = connectToURL(url, method, headers);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(data.getBytes(charset));
        }
        //explicitly request headers to set session cookie
        Map responseHeaders = connection.getHeaderFields();
        InputStream response = connection.getInputStream();
        String responseData;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(response))) {
            String inputLine;
            responseData = "";
            while ((inputLine = in.readLine()) != null) {
                responseData += inputLine;
            }
        }
        return responseData;
    }

    public static String getDataFromURL(String url) throws MalformedURLException, IOException {
        HttpURLConnection connection = connectToURL(url, "GET", new HashMap());
        InputStream response = connection.getInputStream();
        String responseData;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(response))) {
            String inputLine;
            responseData = "";
            while ((inputLine = in.readLine()) != null) {
                responseData += inputLine;
            }
        }
        return responseData;
    }

    public static String getDataFromURL(String url, Map<String,String> headers) throws MalformedURLException, IOException {
        HttpURLConnection connection = connectToURL(url, "GET", headers);
        InputStream response = connection.getInputStream();
        String responseData;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(response))) {
            String inputLine;
            responseData = "";
            while ((inputLine = in.readLine()) != null) {
                responseData += inputLine;
            }
        }
        return responseData;
    }
    
    public static String sendDeleteRequestToUrl(String url, Map<String,String> headers) throws IOException {
        HttpURLConnection connection = connectToURL(url, "DELETE", headers);
        InputStream response = connection.getInputStream();
        String responseData;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(response))) {
            String inputLine;
            responseData = "";
            while ((inputLine = in.readLine()) != null) {
                responseData += inputLine;
            }
        }
        return responseData;
    }

}