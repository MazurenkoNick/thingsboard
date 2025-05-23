/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.report.util.itext;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.ImageDescriptor;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;
import org.xhtmlrenderer.pdf.PDFAsImage;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.util.ContentTypeDetectingInputStreamWrapper;
import org.xhtmlrenderer.util.ImageUtil;
import org.xhtmlrenderer.util.XRLog;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.thingsboard.server.report.util.ImageUtils.checkAndLoadImageFromSvg;
import static org.thingsboard.server.report.util.ImageUtils.isInternalTbImage;
import static org.thingsboard.server.report.util.ImageUtils.isPublicTbImage;
import static org.thingsboard.server.report.util.ImageUtils.isTbImage;
import static org.xhtmlrenderer.util.IOUtil.readBytes;
import static org.xhtmlrenderer.util.ImageUtil.isEmbeddedBase64Image;

public class PdfReportUserAgent extends ITextUserAgent {

    private final TbReportCtx ctx;
    private final ReportDataService dataService;
    private final ITextOutputDevice _outputDevice;
    private final int dotsPerPixel;
    private final int usablePageWidthPx;

    public PdfReportUserAgent(ReportDataService dataService, TbReportCtx ctx,
                              ITextOutputDevice outputDevice, int dotsPerPixel, int usablePageWidthPx) {
        super(outputDevice, dotsPerPixel);
        this.dataService = dataService;
        this.ctx = ctx;
        this._outputDevice = outputDevice;
        this.dotsPerPixel = dotsPerPixel;
        this.usablePageWidthPx = usablePageWidthPx;
    }

    @Override
    public String resolveURI(String uri) {
        return uri;
    }

    @Override
    public ImageResource getImageResource(String uriStr) {
        String unresolvedUri = uriStr;
        if (!isEmbeddedBase64Image(uriStr) && !isTbImage(uriStr)) {
            uriStr = resolveURI(uriStr);
        }
        ImageResource resource = _imageCache.get(unresolvedUri);

        if (resource == null) {
            resource = loadImageResource(uriStr);
            _imageCache.put(unresolvedUri, resource);
        }
        if (resource != null) {
            FSImage image = resource.getImage();
            if (image instanceof ITextFSImage) {
                image = (FSImage) ((ITextFSImage) resource.getImage()).clone();
            }
            return new ImageResource(resource.getImageUri(), image);
        } else {
            return new ImageResource(uriStr, null);
        }
    }

    @Override
    protected InputStream resolveAndOpenStream(String uri) {
        java.io.InputStream is = null;
        URL url = PdfReportUserAgent.class.getResource(uri);
        if (url == null) {
            if (uri.startsWith("/assets/")) {
                url = PdfReportUserAgent.class.getResource("/public" + uri);
            }
            if (url == null) {
                return super.resolveAndOpenStream(uri);
            }
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

    private ImageResource loadImageResource(String uriStr) {
        if (isEmbeddedBase64Image(uriStr)) {
            return loadEmbeddedBase64ImageResource(uriStr);
        } else if (isTbImage(uriStr)) {
            return loadTbImageResource(uriStr);
        }
        try (InputStream is = resolveAndOpenStream(uriStr)) {
            if (is != null) {
                try (ContentTypeDetectingInputStreamWrapper cis = new ContentTypeDetectingInputStreamWrapper(is)) {
                    if (cis.isPdf()) {
                        URI uri = new URI(uriStr);
                        PdfReader reader = _outputDevice.getReader(uri);
                        Rectangle rect = reader.getPageSizeWithRotation(1);
                        float initialWidth = rect.getWidth() * _outputDevice.getDotsPerPoint();
                        float initialHeight = rect.getHeight() * _outputDevice.getDotsPerPoint();
                        PDFAsImage image = new PDFAsImage(uri, initialWidth, initialHeight);
                        return new ImageResource(uriStr, image);
                    } else {
                        Image image = this.loadImage(readBytes(cis));
                        scaleToOutputResolution(image);
                        return new ImageResource(uriStr, new ITextFSImage(image));
                    }
                }
            }
        } catch (BadElementException | IOException | URISyntaxException e) {
            XRLog.exception("Can't read image file; unexpected problem for URI '" + uriStr + "'", e);
        }
        return null;
    }

    private ImageResource loadEmbeddedBase64ImageResource(final String uri) {
        try {
            byte[] buffer = ImageUtil.getEmbeddedBase64Image(uri);
            Image image = this.loadImage(buffer);
            scaleToOutputResolution(image);
            return new ImageResource(null, new ITextFSImage(image));
        } catch (BadElementException | IOException e) {
            XRLog.exception("Can't read XHTML embedded image.", e);
        }
        return new ImageResource(null, null);
    }

    private ImageResource loadTbImageResource(final String uri) {
        try {
            TbResource resource = null;
            if (isInternalTbImage(uri)) {
                resource = this.loadInternalTbImage(uri);
            } else if (isPublicTbImage(uri)) {
                resource = this.loadPublicTbImage(uri);
            }
            if (resource != null) {
                ImageDescriptor descriptor = resource.getDescriptor(ImageDescriptor.class);
                byte[] imageData = resource.getData();
                boolean skipSvgCheck = false;
                if (descriptor != null) {
                    skipSvgCheck = !descriptor.getMediaType().contains("svg+xml");
                }
                Image image = this.loadImage(imageData, skipSvgCheck);
                scaleToOutputResolution(image);
                return new ImageResource(uri, new ITextFSImage(image));
            }
        } catch (Exception e) {
            XRLog.exception("Can't read TB image.", e);
        }
        return null;
    }

    private TbResource loadInternalTbImage(final String uri) throws Exception {
        String link = StringUtils.removeStart(uri, DataConstants.TB_IMAGE_PREFIX);
        String imageType = null;
        if (link.startsWith("/api/images/tenant/")) {
            imageType = "tenant";
        } else if (link.startsWith("/api/images/system/")) {
            imageType = "system";
        }
        if (imageType != null) {
            var parts = uri.split("/");
            if (parts.length >= 5) {
                String key = parts[4];
                key = URLDecoder.decode(key, StandardCharsets.UTF_8);
                return this.dataService.findImage(imageType, key, this.ctx);
            }
        }
        return null;
    }

    private TbResource loadPublicTbImage(final String uri) throws Exception {
        var parts = uri.split("/");
        if (parts.length >= 5) {
            String publicKey = parts[4];
            return this.dataService.findPublicImage(publicKey, this.ctx);
        }
        return null;
    }

    private void scaleToOutputResolution(Image image) {
        float factor = dotsPerPixel;
        if (factor != 1.0f) {
            image.scaleAbsolute(image.getPlainWidth() * factor, image.getPlainHeight() * factor);
        }
    }

    private Image loadImage(byte[] data) throws IOException {
        return loadImage(data, false);
    }

    private Image loadImage(byte[] data, boolean skipSvgCheck) throws IOException {
        BufferedImage image = skipSvgCheck ? null : checkAndLoadImageFromSvg(data, this.usablePageWidthPx);
        if (image != null) {
            return Image.getInstance(image, null);
        } else {
            return Image.getInstance(data);
        }
    }
}
