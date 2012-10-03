package org.apachelog;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;

import java.io.*;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides basic functionality to index the logs into a Solr Index (uses SolrCloud)
 *
 * Example schema:
 *
 *    <field name="id"             type="string"  indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="datetime"       type="tdate"   indexed="true"  stored="true"  required="false" multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="webServer"      type="string"  indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="host"           type="string"  indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="method"         type="string"  indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="protocol"       type="string"  indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="resource"       type="string"  indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="resourceParams" type="string"  indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="referer"        type="string"  indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="userAgent"      type="string"  indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="remoteUser"     type="string"  indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="remoteLogName"  type="string"  indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="code"           type="tint"    indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="time"           type="tint"    indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="xForwardFor"    type="string"  indexed="true"  stored="true"  required="true"  multiValued="true"  omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="bytes"          type="string"  indexed="true"  stored="true"  required="true"  multiValued="true"  omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="logText"        type="string"  indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 *    <field name="isValidLine"    type="boolean" indexed="true"  stored="true"  required="true"  multiValued="false" omitNorms="true" termVectors="false" termPositions="false" termOffsets="false" />
 */
public class ApacheLogIndexer {
    private static int DEFAULT_BATCH_SIZE = 2000;

    private String serverUrl = "localhost:8900";
    private ApacheLogParser parser;
    private ApacheLogParser invalidLineParser;
    private Pattern findResponseCode;
    private int batchSize = DEFAULT_BATCH_SIZE;

    public ApacheLogIndexer(ApacheLogParser parser, ApacheLogParser invalidLineParser) {
        this.parser = parser;
        this.invalidLineParser = invalidLineParser;
        this.findResponseCode = Pattern.compile("HTTP/1\\.1\"\\s(\\d+)\\s");
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void index(String filename) throws FileNotFoundException, MalformedURLException, ApacheLogIndexerException {
        index(new File(filename));
    }

    public void index(File file) throws FileNotFoundException, MalformedURLException, ApacheLogIndexerException {
        BufferedReader in = null;

        in = new BufferedReader(new FileReader(file));

        String line = null;
        CloudSolrServer server = new CloudSolrServer(getServerUrl());
        System.out.println("Connecting to '" + getServerUrl() + "'");
        server.setDefaultCollection("apachelog");
        DateFormat dateFormat = new SimpleDateFormat("[dd/MMM/yyyy:HH:mm:ss Z]");
        DateFormat syslogDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
        List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>(getBatchSize());
 
        try {
            Map<String, String> fields = new HashMap<String, String>();
            int processed = 0;
            while ((line = in.readLine()) != null) {
                ++processed;
                fields.clear();
                try {
                    parser.parse(line, fields);
                } catch (ApacheLogParserException ex) {
                    try {
                        invalidLineParser.parse(line + " ", fields);
                        SolrInputDocument doc = createInvalidLineDocument(line, syslogDateFormat, fields);
                        addDocument(server, docs, doc);
                    } catch (ApacheLogParserException ex2) {
                        // log exception from the non invalid line
                        ex2.printStackTrace(System.err);
                    }
                    continue;                    
                }

                SolrInputDocument doc = createDocument(line, dateFormat, fields);
                addDocument(server, docs, doc);
            }
            if (docs.size() > 0) {
                server.add(docs);
                docs.clear();
            }
            System.out.println("Processed " + processed);
        } catch (IOException ex) {
            throw new ApacheLogIndexerException("Exception reading file " + file.getName(), ex);
        } catch (SolrServerException ex) {
            throw new ApacheLogIndexerException("Exception indexing log line " + line, ex);
        } catch (SolrException ex) {
            throw new ApacheLogIndexerException("Exception indexing log line " + line, ex);
        } finally {
            try {
                server.commit(false, true, false);
            } catch (SolrServerException ex) {
                throw new ApacheLogIndexerException("Exception while committing", ex);
            } catch (IOException ex) {
                throw new ApacheLogIndexerException("Exception while committing", ex);
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    throw new ApacheLogIndexerException("Exception closing file " + file.getName(), ex);    
                }
            }
        }
    }

    private void addDocument(CloudSolrServer server, List<SolrInputDocument> docs, SolrInputDocument doc) throws SolrServerException, IOException {
        if (doc != null) {
            docs.add(doc);
            if (docs.size() >= getBatchSize()) {
                server.add(docs);
                docs.clear();
                server.commit(false, true, true);
            }
        }
    }

    private SolrInputDocument createDocument(String line, DateFormat dateFormat, Map<String, String> fields) {
        Date date = null;
        try {
            date = dateFormat.parse(fields.get("%t"));
        } catch (ParseException ex) {
            ex.printStackTrace(System.err);
            return null;
        }
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", fields.get("%t") + "|" + line.hashCode());
        doc.setField("datetime", date);
        doc.setField("webServer", fields.get("%syslog_ws"));
        doc.setField("host", fields.get("%{Host}i"));
        String resource = fields.get("%r");
        String method = "-";
        //boolean print = resource.equals("GET /");

        int firstSpace = resource.indexOf(' ');
        if (firstSpace != -1) {
            doc.setField("method", resource.substring(0, firstSpace));
            resource = resource.substring(firstSpace+1);
        } else {
            doc.setField("method", "-");
        }

        int lastSpace = resource.lastIndexOf(' ');
        if (lastSpace != -1) {
            doc.setField("protocol", resource.substring(lastSpace+1));
            resource = resource.substring(0, lastSpace);
        } else {
            doc.setField("protocol", "-");
        }

        String[] resourceParts = StringUtils.split(resource, '?');
        if (resourceParts.length >= 1) {
            doc.setField("resource", resourceParts[0]);
        } else {
            doc.setField("resource", "-");
        }

        if (resourceParts.length > 0) {
            doc.setField("resourceParams", StringUtils.join(resourceParts, "", 1, resourceParts.length));
        } else {
            doc.setField("resourceParams", "-");
        }


        doc.setField("code", fields.get("%>s"));
        doc.setField("referer", fields.get("%{Referer}i"));
        doc.setField("userAgent", fields.get("%{User-Agent}i"));
        doc.setField("time", fields.get("%D"));
        doc.setField("remoteUser", fields.get("%u"));
        doc.setField("remoteLogName", fields.get("%l"));
        doc.setField("bytes", fields.get("%b"));
        String[] xForwardForServers = StringUtils.split(fields.get("%{X-Forwarded-For}i"), ", ");
        for (String xForwardForServer : xForwardForServers) {
            doc.addField("xForwardFor", xForwardForServer);
        }
        doc.addField("isValidLine", true);
        doc.setField("logText", line);
        return doc;
    }

    private SolrInputDocument createInvalidLineDocument(String line, DateFormat dateFormat, Map<String, String> fields) {
        Date date = null;
        try {
            date = dateFormat.parse(fields.get("%syslog_ti").replaceAll("([\\+\\-]\\d\\d):(\\d\\d)","$1$2"));
        } catch (ParseException ex) {
            ex.printStackTrace(System.err);
            return null;
        }
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", fields.get("%syslog_ti") + "|" + line.hashCode());
        doc.setField("datetime", date);
        doc.setField("webServer", fields.get("%syslog_ws"));
        doc.setField("host", "?");
        doc.setField("method", "?");
        doc.setField("protocol", "?");
        doc.setField("resource", "?");
        doc.setField("resourceParams", "?");
        String responseCode = "-1";

        Matcher m = findResponseCode.matcher(line);
        if (m.find()) {
            responseCode = m.group(1);
        }
        
        doc.setField("code", responseCode);
        doc.setField("referer", "?");
        doc.setField("userAgent", "?");
        doc.setField("time", "-1");
        doc.setField("remoteUser", "?");
        doc.setField("remoteLogName", "?");
        doc.setField("bytes", "-1");
        doc.addField("xForwardFor", "?");
        doc.addField("isValidLine", false);
        doc.setField("logText", line);

        return doc;
    }
}
