package org.apachelog;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * This class provides functionality to parse apache Log lines
 */
public class ApacheLogParser {
    private String format = "%t0 %ws %{X-Forwarded-For}i %l %u %t %{Host}i \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\" %D";

    private List<String> names;
    private List<String> subPatterns;
    private Pattern pattern;

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public String[] getNames() {
        return names.toArray(new String[names.size()]);
    }

    public void parseFormat() throws ApacheLogParserException {
        parseFormat(format);    
    }

    /**
     * Takes the log format from an Apache configuration file.
     *
     * Best just copy and paste directly from the .conf file
     * and pass using a Python raw string e.g.
     *
     * @param format is the Apache log format
     * @throws ApacheLogParserException if the format can't be compile into a pattern
     */
    public void parseFormat(final String format) throws ApacheLogParserException {
        String f = StringUtils.strip(format);
        f = f.replaceAll("[ \t]+", " ");

        Pattern findQuotes = Pattern.compile("^\"");
        Pattern findReferrerAgent = Pattern.compile("Referer|User-Agent");
        Pattern findXForwardedFor = Pattern.compile("X-Forwarded-For");
        Pattern findPercent = Pattern.compile("^%.*t$");

        String[] elements = StringUtils.split(format, ' ');
        names = new ArrayList<String>(elements.length);
        subPatterns = new ArrayList<String>(elements.length);

        for (String element : elements) {
            boolean hasQuotes = findQuotes.matcher(element).find();

            if (hasQuotes) {
                element = StringUtils.strip(element, "\"");
            }

            names.add(getAlias(element));

            String subPattern = "(\\S*)";
            if (hasQuotes) {
                if (element.equals("%r") || findReferrerAgent.matcher(element).find()) {
                    subPattern = "\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"";
                } else {
                    subPattern = "\"([^\"]*)\"";
                }
            } else if (findPercent.matcher(element).find()) {
                subPattern = "(\\[[^\\]]+\\])";
            } else if (findXForwardedFor.matcher(element).find()) {
                subPattern = "((?:\\S*,\\s)*\\S*)";
            } else if (element.equals("%U")) {
                subPattern = "(.+?)";
            } else if (element.equals("%_")) {
                subPattern = "(.+)?";
            }

            subPatterns.add(subPattern);
        }

        try {
            setPattern(Pattern.compile('^' + StringUtils.join(subPatterns, ' ') + '$'));
        } catch (PatternSyntaxException ex) {
            throw new ApacheLogParserException("Unable to parse format", ex);
        }
    }

    /**
     * Override / replace this method if you want to map format field names to something else. This method is called
     * when the parser is constructed, not when actually parsing a log file
     * @param name is field name in fhe format
     * @return return the alias for the format field name
     */
    public String getAlias(String name) {
        return name;
    }

    /**
     * Parses a single line from the log file and returns a dictionary of it's contents.

     * @param line is the log line to be parsed
     * @return
     */
    public Map<String, String> parse(final String line) throws ApacheLogParserException {
        return parse(line, new HashMap<String, String>());
    }

    public Map<String, String> parse(String line, Map<String, String> fields) throws ApacheLogParserException {
        Matcher m = getPattern().matcher(StringUtils.strip(line));
        if (m.matches()) {
            for (int i = 1; i <= m.groupCount(); ++i) {
                fields.put(names.get(i-1), m.group(i));
            }
        } else {
            throw new ApacheLogParserException(String.format("Unable to parse: %s with the %s regular expression", line, getPattern().pattern()));
        }
        return fields;
    }

    public static void main(String[] args) {
        ApacheLogParser parser = new ApacheLogParser();

        try {
            parser.parseFormat();
        } catch (ApacheLogParserException ex) {
            ex.printStackTrace();
        }
    }

}
