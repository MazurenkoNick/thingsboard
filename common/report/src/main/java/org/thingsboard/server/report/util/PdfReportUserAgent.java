package org.thingsboard.server.report.util;

import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;
import org.xhtmlrenderer.util.XRLog;

import java.io.InputStream;
import java.net.URL;

import static org.xhtmlrenderer.pdf.ITextRenderer.DEFAULT_DOTS_PER_PIXEL;
import static org.xhtmlrenderer.pdf.ITextRenderer.DEFAULT_DOTS_PER_POINT;

public class PdfReportUserAgent extends ITextUserAgent {

    public PdfReportUserAgent() {
        super(new ITextOutputDevice(DEFAULT_DOTS_PER_POINT), DEFAULT_DOTS_PER_PIXEL);
    }

    @Override
    public String resolveURI(String uri) {
        return uri;
    }

    @Override
    protected InputStream resolveAndOpenStream(String uri) {
        java.io.InputStream is = null;
        URL url = PdfReportUserAgent.class.getResource(uri);
        if (url == null) {
            XRLog.load("Didn't find resource [" + uri + "].");
            return null;
        }
        try {
            is = url.openStream();
        }
        catch (java.net.MalformedURLException e) {
            XRLog.exception("bad URL given: " + uri, e);
        }
        catch (java.io.FileNotFoundException e) {
            XRLog.exception("item at URI " + uri + " not found");
        }
        catch (java.io.IOException e) {
            XRLog.exception("IO problem for " + uri, e);
        }
        return is;
    }
}
