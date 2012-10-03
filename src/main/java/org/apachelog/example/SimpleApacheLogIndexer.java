package org.apachelog.example;

import org.apachelog.ApacheLogIndexer;
import org.apachelog.ApacheLogParser;
import org.apachelog.ApacheLogParserException;

public class SimpleApacheLogIndexer {
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage: java -cp:<name>.jar org.apachelog.example.SimpleApacheLogIndexer <filename>.log [<Solr Sever URL]");
            return;
        }

        ApacheLogParser parser = new ApacheLogParser();
        ApacheLogParser invalidLineParser = new ApacheLogParser();

        try {
            parser.parseFormat("%syslog_ti %syslog_ws %syslog_n %{X-Forwarded-For}i %l %u %t %{Host}i \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\" %D");
            invalidLineParser.parseFormat("%syslog_ti %syslog_ws %syslog_n %_");
            ApacheLogIndexer indexer = new ApacheLogIndexer(parser, invalidLineParser);

            if (args.length == 2) {
                indexer.setServerUrl(args[1]);
        }

            try {
                indexer.index(args[0]);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (ApacheLogParserException ex) {
            ex.printStackTrace();
        }
    }
}
