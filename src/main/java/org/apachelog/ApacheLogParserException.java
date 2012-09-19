package org.apachelog;

public class ApacheLogParserException extends Exception {
    public ApacheLogParserException(String msg) {
        super(msg);
    }

    public ApacheLogParserException(Throwable t) {
        super(t);
    }

    public ApacheLogParserException(String msg, Throwable t) {
        super(msg, t);
    }
}
