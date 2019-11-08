/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.diagram.internal;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mxgraph.util.mxUtils;
import com.xwiki.diagram.DiagramConfiguration;

/**
 * Collects the data needed to perform a diagram export.
 * 
 * @version $Id$
 * @since 1.11
 */
@Component(roles = DiagramExportRequestFactory.class)
@Singleton
public class DiagramExportRequestFactory
{
    private static final String UTF8 = "UTF-8";

    @Inject
    private DiagramConfiguration configuration;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentStringDocumentReferenceResolver;

    /**
     * Collects the data needed to perform a diagram export.
     * 
     * @param request the actual request to collect the data from
     * @return the data needed to perform the diagram export
     * @throws UnsupportedEncodingException if the request is not valid
     */
    public DiagramExportRequest createDiagramExportRequest(ServletRequest request) throws UnsupportedEncodingException
    {
        DiagramExportRequest diagramExportRequest = new DiagramExportRequest();

        diagramExportRequest.diagramReference = this.currentStringDocumentReferenceResolver
            .resolve(StringUtils.defaultString(request.getParameter("diagramReference")));

        diagramExportRequest.format = request.getParameter("format");
        if (StringUtils.isEmpty(diagramExportRequest.format)) {
            throw new IllegalArgumentException("Export format must be specified.");
        }

        setWidthAndHeight(diagramExportRequest, request);
        setBackgroundColor(diagramExportRequest, request);
        setScale(diagramExportRequest, request);
        setExtras(diagramExportRequest, request);

        diagramExportRequest.dotsPerInch = getInteger(request, "dpi", 1, "Invalid export DPI.");
        diagramExportRequest.borderWidth = getInteger(request, "border", 0, "Invalid export border width.");

        diagramExportRequest.base64 = getBoolean(request, "base64");
        diagramExportRequest.embedXML = getBoolean(request, "embedXml");

        diagramExportRequest.outputFileName = request.getParameter("filename");
        if (StringUtils.endsWithIgnoreCase(diagramExportRequest.outputFileName, ".xml")) {
            diagramExportRequest.outputFileName =
                diagramExportRequest.outputFileName.substring(0, diagramExportRequest.outputFileName.length() - 3)
                    + diagramExportRequest.format;
        }

        diagramExportRequest.xml = request.getParameter("xml");
        // Decoding is optional (no plain text values allowed).
        if (StringUtils.startsWith(diagramExportRequest.xml, "%3C")) {
            diagramExportRequest.xml = URLDecoder.decode(diagramExportRequest.xml, UTF8);
        }

        return diagramExportRequest;
    }

    private void setWidthAndHeight(DiagramExportRequest diagramExportRequest, ServletRequest request)
    {
        diagramExportRequest.width = getInteger(request, "w", 1, "Invalid export width.");
        diagramExportRequest.height = getInteger(request, "h", 1, "Invalid export height.");

        if (diagramExportRequest.width != null && diagramExportRequest.height != null
            && diagramExportRequest.width * diagramExportRequest.height >= this.configuration.getMaxArea()) {
            throw new IllegalArgumentException("Export area (width * height) too large.");
        }
    }

    private Integer getInteger(ServletRequest request, String key, int minValue, String message)
    {
        Integer integerValue = null;
        String value = request.getParameter(key);
        if (value != null) {
            integerValue = Integer.parseInt(value);
            if (integerValue < minValue) {
                throw new IllegalArgumentException(message);
            }
        }
        return integerValue;
    }

    private boolean getBoolean(ServletRequest request, String key)
    {
        String value = request.getParameter(key);
        return "1".equals(value) || Boolean.valueOf(value);
    }

    private void setBackgroundColor(DiagramExportRequest diagramExportRequest, ServletRequest request)
    {
        String backgroundColor = request.getParameter("bg");
        diagramExportRequest.backgroundColor = (backgroundColor != null) ? mxUtils.parseColor(backgroundColor) : null;

        // Allow transparent backgrounds only for PNG export format.
        if (diagramExportRequest.backgroundColor == null
            && !DiagramExportRequest.FORMAT_PNG.equals(diagramExportRequest.format)) {
            diagramExportRequest.backgroundColor = Color.WHITE;
        }
    }

    private void setScale(DiagramExportRequest diagramExportRequest, ServletRequest request)
    {
        String value = request.getParameter("scale");
        if (value != null) {
            diagramExportRequest.scale = Double.parseDouble(value);
            if (diagramExportRequest.scale <= 0) {
                throw new IllegalArgumentException("Invalid export scale.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setExtras(DiagramExportRequest diagramExportRequest, ServletRequest request)
        throws UnsupportedEncodingException
    {
        String json = request.getParameter("extras");
        // draw.io double-encodes the parameter values sometimes.
        if (StringUtils.startsWith(json, "%7B")) {
            json = URLDecoder.decode(json, UTF8);
        }

        if (StringUtils.isNotBlank(json)) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                diagramExportRequest.extras = objectMapper.readValue(json, Map.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid JSON for the extras export parameter.", e);
            }
        }
    }
}
