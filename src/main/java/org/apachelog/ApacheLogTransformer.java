package org.apachelog;

import java.io.*;
import java.util.*;

/**
 * Simple class to extract fields from Apache Log entries.
 */
public class ApacheLogTransformer {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java -jar *jar \"<apache log format\" \"<field names>\" file ");
            System.out.println("  java -jar *.jar '%h %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\"' '%r %>b' /var/log/apache.log");
        }
        ApacheLogParser parser = new ApacheLogParser();
        try {
            parser.parseFormat(args[0]);
            try {
                BufferedReader in = null;

                try {
                    in = new BufferedReader(new FileReader(new File(args[2])));
                } catch (FileNotFoundException ex) {
                    throw new ApacheLogIndexerException(ex);
                }

                try {
                    String line = null;
                    List<String> fieldNames = Arrays.asList(args[1].split(" "));
                    Map<String, String> fields = new HashMap<String, String>();
                    StringBuffer buffer = new StringBuffer();
                    while ((line = in.readLine()) != null) {
                        fields.clear();
                        try {
                            parser.parse(line, fields);
                            buffer.setLength(0);
                            for (String fieldName : fieldNames) {
                                buffer.append("\"").append(fields.get(fieldName)).append("\"").append(",");
                            }
                            buffer.setLength(buffer.length() - 1);
                            System.out.println(buffer.toString());
                        } catch (ApacheLogParserException ex) {
                            ex.printStackTrace(System.err);
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();    
                }

            } catch (ApacheLogIndexerException ex) {
                ex.printStackTrace();
            }
        } catch (ApacheLogParserException ex) {
            ex.printStackTrace();
        }
    }
}
