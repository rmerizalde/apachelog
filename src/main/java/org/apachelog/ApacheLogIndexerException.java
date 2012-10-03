package org.apachelog;

public class ApacheLogIndexerException extends Exception {
    public ApacheLogIndexerException(String msg) {
        super(msg);
    }

    public ApacheLogIndexerException(Throwable t) {
        super(t);
    }

    public ApacheLogIndexerException(String msg, Throwable t) {
        super(msg, t);
    }
}
