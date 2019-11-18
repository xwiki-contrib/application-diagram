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
package com.xwiki.diagram.export.internal;

import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xwiki.component.annotation.Component;

import com.mxpdf.text.DocumentException;

/**
 * Exports a diagram to PDF or image format.
 * 
 * @version $Id$
 * @since 1.11
 */
@Component(roles = HTTPDiagramExporter.class)
@Singleton
public class HTTPDiagramExporter extends DiagramExporter
{
    /**
     * Exports a diagram.
     * 
     * @param request the diagram export request
     * @param response the response to write the output to
     * @throws IOException if it fails to write the output to the response
     * @throws ParserConfigurationException if it fails to create an XML reader
     * @throws SAXException if it fails to parse the diagram XML
     * @throws DocumentException if it fails to generate the PDF
     */
    public void export(DiagramExportRequest request, HttpServletResponse response)
        throws IOException, DocumentException, SAXException, ParserConfigurationException
    {
        try (OutputStream out = response.getOutputStream()) {
            if (DiagramExportRequest.FORMAT_PDF.equals(request.format)) {
                exportAsPDF(request, response);
            } else {
                exportAsImage(request, response);
            }
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    private void exportAsImage(DiagramExportRequest request, HttpServletResponse response)
        throws IOException, SAXException, ParserConfigurationException
    {
        if (request.outputFileName != null) {
            response.setContentType("application/x-unknown");
            setContentDisposition(response, request.outputFileName);
        } else if (request.format != null) {
            response.setContentType("image/" + request.format.toLowerCase());
        }

        exportAsImage(request, response.getOutputStream());
    }

    private void exportAsPDF(DiagramExportRequest request, HttpServletResponse response)
        throws DocumentException, IOException, SAXException, ParserConfigurationException
    {
        response.setContentType("application/pdf");

        if (request.outputFileName != null) {
            setContentDisposition(response, request.outputFileName);
        }

        exportAsPDF(request, response.getOutputStream());
    }

    private void setContentDisposition(HttpServletResponse response, String fileName)
    {
        response.setHeader("Content-Disposition",
            "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);
    }
}
