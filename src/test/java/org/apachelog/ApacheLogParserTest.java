package org.apachelog;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

public class ApacheLogParserTest {
    private ApacheLogParser parser = new ApacheLogParser();
    private ApacheLogParser parserMultipleIps = new ApacheLogParser();
    private ApacheLogParser parserSyslog = new ApacheLogParser();
    private ApacheLogParser parserInvalidLines = new ApacheLogParser();

    @Before
    public void setup() throws Exception {
        parser.parseFormat("%h %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\"");
        parserMultipleIps.parseFormat("%{X-Forwarded-For}i %l %u %t %{Host}i \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\" %D");
        parserSyslog.parseFormat("%syslog_ti %syslog_ws %syslog_n %{X-Forwarded-For}i %l %u %t %{Host}i \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\" %D");
        parserInvalidLines.parseFormat("%syslog_ti %syslog_ws %syslog_n %_");
    }

    @Test
    public void testPattern() {
        assertEquals("^(\\S*) (\\S*) (\\S*) (\\[[^\\]]+\\]) \"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\" (\\S*) (\\S*) \"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\" \"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"$",
                parser.getPattern().pattern());
    }

    @Test
    public void testNames() {
        String[] expectedNames = "%h %l %u %t %r %>s %b %{Referer}i %{User-Agent}i".split(" ");
        assertArrayEquals(expectedNames, parser.getNames());
    }

    @Test
    public void testLine1() throws Exception {
        StringBuffer line = new StringBuffer()
            .append("212.74.15.68 - - [23/Jan/2004:11:36:20 +0000] ")
            .append("\"GET /images/previous.png HTTP/1.1\" 200 2607 ")
            .append("\"http://peterhi.dyndns.org/bandwidth/index.html\" ")
            .append("\"Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.2) Gecko/20021202\"");

        Map<String, String> fields = parser.parse(line.toString());

        assertEquals("212.74.15.68", fields.get("%h"));
        assertEquals("-", fields.get("%l"));
        assertEquals("-", fields.get("%u"));
        assertEquals("[23/Jan/2004:11:36:20 +0000]", fields.get("%t"));
        assertEquals("GET /images/previous.png HTTP/1.1", fields.get("%r"));
        assertEquals("200", fields.get("%>s"));
        assertEquals("2607", fields.get("%b"));
        assertEquals("http://peterhi.dyndns.org/bandwidth/index.html", fields.get("%{Referer}i"));
        assertEquals("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.2) Gecko/20021202", fields.get("%{User-Agent}i"));
    }

    @Test
    public void testLine2() throws Exception {
        StringBuffer line = new StringBuffer()
            .append("212.74.15.68 - - [23/Jan/2004:11:36:20 +0000] ")
            .append("\"GET /images/previous.png=\\\" HTTP/1.1\" 200 2607 ")
            .append("\"http://peterhi.dyndns.org/bandwidth/index.html\" ")
            .append("\"Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.2) Gecko/20021202\"");

        Map<String, String> fields = parser.parse(line.toString());

        assertEquals("212.74.15.68", fields.get("%h"));
        assertEquals("-", fields.get("%l"));
        assertEquals("-", fields.get("%u"));
        assertEquals("[23/Jan/2004:11:36:20 +0000]", fields.get("%t"));
        assertEquals("GET /images/previous.png=\\\" HTTP/1.1", fields.get("%r"));
        assertEquals("200", fields.get("%>s"));
        assertEquals("2607", fields.get("%b"));
        assertEquals("http://peterhi.dyndns.org/bandwidth/index.html", fields.get("%{Referer}i"));
        assertEquals("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.2) Gecko/20021202", fields.get("%{User-Agent}i"));
    }

    @Test
    public void testLine3() throws Exception {
        StringBuffer line = new StringBuffer()
            .append("4.224.234.46 - - [20/Jul/2004:13:18:55 -0700] ")
            .append("\"GET /core/listing/pl_boat_detail.jsp?&units=Feet&checked")
            .append("_boats=1176818&slim=broker&&hosturl=giffordmarine&&ywo=")
            .append("giffordmarine& HTTP/1.1\" 200 2888 \"http://search.yahoo.com/")
            .append("bin/search?p=\\\"grady%20white%20306%20bimini\\\"\" ")
            .append("\"Mozilla/4.0 (compatible; MSIE 6.0; Windows 98; YPC 3.0.3; yplus 4.0.00d)\"");

        Map<String, String> fields = parser.parse(line.toString());

        assertEquals("4.224.234.46", fields.get("%h"));
        assertEquals("-", fields.get("%l"));
        assertEquals("-", fields.get("%u"));
        assertEquals("[20/Jul/2004:13:18:55 -0700]", fields.get("%t"));
        assertEquals("GET /core/listing/pl_boat_detail.jsp?&units=Feet&checked_boats=1176818&slim=broker&&hosturl=giffordmarine&&ywo=giffordmarine& HTTP/1.1", fields.get("%r"));
        assertEquals("200", fields.get("%>s"));
        assertEquals("2888", fields.get("%b"));
        assertEquals("http://search.yahoo.com/bin/search?p=\\\"grady%20white%20306%20bimini\\\"", fields.get("%{Referer}i"));
        assertEquals("Mozilla/4.0 (compatible; MSIE 6.0; Windows 98; YPC 3.0.3; yplus 4.0.00d)", fields.get("%{User-Agent}i"));
    }

    @Test
    public void testLine4() throws Exception {
        StringBuffer line = new StringBuffer()
            .append("23.57.68.53 - - [18/Sep/2012:00:00:06 -0600] ")
            .append("origin-www.backcountry.com ")
            .append("\"GET /images/promos/wm300x250_01.gif HTTP/1.1\" ")
            .append("404 497 \"-\" ")
            .append("\"Mozilla/5.0 (Linux; U; Android 4.0.4; en-us; DROID RAZR Build/6.7.2-180_DHD-16_M4-31) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30\" ")
            .append("576");

        Map<String, String> fields = parserMultipleIps.parse(line.toString());

        assertEquals("23.57.68.53", fields.get("%{X-Forwarded-For}i"));
        System.out.println(fields);
        assertEquals("-", fields.get("%l"));
        assertEquals("-", fields.get("%u"));
        assertEquals("[18/Sep/2012:00:00:06 -0600]", fields.get("%t"));
        assertEquals("GET /images/promos/wm300x250_01.gif HTTP/1.1", fields.get("%r"));
        assertEquals("404", fields.get("%>s"));
        assertEquals("497", fields.get("%b"));
        assertEquals("-", fields.get("%{Referer}i"));
        assertEquals("Mozilla/5.0 (Linux; U; Android 4.0.4; en-us; DROID RAZR Build/6.7.2-180_DHD-16_M4-31) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30", fields.get("%{User-Agent}i"));
    }

    @Test
    public void testLine5() throws Exception {
        StringBuffer line = new StringBuffer()
            .append("70.196.196.64, 23.57.68.53 - - [18/Sep/2012:00:00:06 -0600] ")
            .append("origin-www.backcountry.com ")
            .append("\"GET /images/promos/wm300x250_01.gif HTTP/1.1\" ")
            .append("404 497 \"-\" ")
            .append("\"Mozilla/5.0 (Linux; U; Android 4.0.4; en-us; DROID RAZR Build/6.7.2-180_DHD-16_M4-31) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30\" ")
            .append("576");

        Map<String, String> fields = parserMultipleIps.parse(line.toString());

        assertEquals("70.196.196.64, 23.57.68.53", fields.get("%{X-Forwarded-For}i"));
        System.out.println(fields);
        assertEquals("-", fields.get("%l"));
        assertEquals("-", fields.get("%u"));
        assertEquals("[18/Sep/2012:00:00:06 -0600]", fields.get("%t"));
        assertEquals("origin-www.backcountry.com", fields.get("%{Host}i"));
        assertEquals("GET /images/promos/wm300x250_01.gif HTTP/1.1", fields.get("%r"));
        assertEquals("404", fields.get("%>s"));
        assertEquals("497", fields.get("%b"));
        assertEquals("-", fields.get("%{Referer}i"));
        assertEquals("Mozilla/5.0 (Linux; U; Android 4.0.4; en-us; DROID RAZR Build/6.7.2-180_DHD-16_M4-31) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30", fields.get("%{User-Agent}i"));
    }

    @Test
    public void testLine6() throws Exception {
        StringBuffer line = new StringBuffer()
            .append("70.196.196.64, 23.57.68.53, 127.0.0.1 - - [18/Sep/2012:00:00:06 -0600] ")
            .append("origin-www.backcountry.com ")
            .append("\"GET /images/promos/wm300x250_01.gif HTTP/1.1\" ")
            .append("404 497 \"-\" ")
            .append("\"Mozilla/5.0 (Linux; U; Android 4.0.4; en-us; DROID RAZR Build/6.7.2-180_DHD-16_M4-31) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30\" ")
            .append("576");

        Map<String, String> fields = parserMultipleIps.parse(line.toString());

        assertEquals("70.196.196.64, 23.57.68.53, 127.0.0.1", fields.get("%{X-Forwarded-For}i"));
        assertEquals("-", fields.get("%l"));
        assertEquals("-", fields.get("%u"));
        assertEquals("[18/Sep/2012:00:00:06 -0600]", fields.get("%t"));
        assertEquals("origin-www.backcountry.com", fields.get("%{Host}i"));
        assertEquals("GET /images/promos/wm300x250_01.gif HTTP/1.1", fields.get("%r"));
        assertEquals("404", fields.get("%>s"));
        assertEquals("497", fields.get("%b"));
        assertEquals("-", fields.get("%{Referer}i"));
        assertEquals("Mozilla/5.0 (Linux; U; Android 4.0.4; en-us; DROID RAZR Build/6.7.2-180_DHD-16_M4-31) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30", fields.get("%{User-Agent}i"));
    }

    @Test
    public void testLine7() throws Exception {
        StringBuffer line = new StringBuffer()
            .append("/var/log/mywebserv/09/18/19/apache.log:2012-09-18T19:24:56-06:00 mywebserv apache: 70.196.196.64, 23.57.68.53, 127.0.0.1 - - [18/Sep/2012:00:00:06 -0600] ")
            .append("origin-www.backcountry.com ")
            .append("\"GET /images/promos/wm300x250_01.gif HTTP/1.1\" ")
            .append("404 497 \"-\" ")
            .append("\"Mozilla/5.0 (Linux; U; Android 4.0.4; en-us; DROID RAZR Build/6.7.2-180_DHD-16_M4-31) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30\" ")
            .append("576");

        Map<String, String> fields = parserSyslog.parse(line.toString());

        assertEquals("/var/log/mywebserv/09/18/19/apache.log:2012-09-18T19:24:56-06:00", fields.get("%syslog_ti"));
        assertEquals("mywebserv", fields.get("%syslog_ws"));
        assertEquals("apache:", fields.get("%syslog_n"));
        assertEquals("70.196.196.64, 23.57.68.53, 127.0.0.1", fields.get("%{X-Forwarded-For}i"));
        assertEquals("-", fields.get("%l"));
        assertEquals("-", fields.get("%u"));
        assertEquals("[18/Sep/2012:00:00:06 -0600]", fields.get("%t"));
        assertEquals("GET /images/promos/wm300x250_01.gif HTTP/1.1", fields.get("%r"));
        assertEquals("404", fields.get("%>s"));
        assertEquals("497", fields.get("%b"));
        assertEquals("-", fields.get("%{Referer}i"));
        assertEquals("Mozilla/5.0 (Linux; U; Android 4.0.4; en-us; DROID RAZR Build/6.7.2-180_DHD-16_M4-31) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30", fields.get("%{User-Agent}i"));
    }

    @Test
    public void testLine8() throws Exception {
        StringBuffer line = new StringBuffer()
            .append("/var/log/mywebserv/09/18/19/apache.log:2012-09-18T19:24:56-06:00 mywebserv apache: 70.196.196.64, 23.57.68.53, 127.0.0.1 - - [18/Sep/2012:00:00:06 -0600]");


        Map<String, String> fields = parserInvalidLines.parse(line.toString());

        assertEquals("/var/log/mywebserv/09/18/19/apache.log:2012-09-18T19:24:56-06:00", fields.get("%syslog_ti"));
        assertEquals("mywebserv", fields.get("%syslog_ws"));
        assertEquals("apache:", fields.get("%syslog_n"));
        assertEquals("70.196.196.64, 23.57.68.53, 127.0.0.1 - - [18/Sep/2012:00:00:06 -0600]", fields.get("%_"));
    }



}
