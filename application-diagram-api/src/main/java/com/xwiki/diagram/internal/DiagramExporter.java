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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.XMLReaderFactory;

import com.mxgraph.canvas.mxGraphicsCanvas2D;
import com.mxgraph.canvas.mxICanvas2D;
import com.mxgraph.reader.mxSaxOutputHandler;
import com.mxgraph.util.mxUtils;
import com.mxpdf.text.Document;
import com.mxpdf.text.DocumentException;
import com.mxpdf.text.PageSize;
import com.mxpdf.text.Rectangle;
import com.mxpdf.text.pdf.PdfWriter;

/**
 * Exports a diagram to PDF or image format.
 * 
 * @version $Id$
 * @since 1.11
 */
@Component(roles = DiagramExporter.class)
@Singleton
public class DiagramExporter
{
    @Inject
    private DiagramCanvasFactory canvasFactory;

    @Inject
    private XMLReaderFactory xmlReaderFactory;

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
        BufferedImage image = mxUtils.createBufferedImage(request.width, request.height, request.backgroundColor);

        if (image != null) {
            Graphics2D graphics2D = image.createGraphics();
            mxUtils.setAntiAlias(graphics2D, true, true);
            renderXML(request.xml, this.canvasFactory.createCanvas(graphics2D));

            if (request.outputFileName != null) {
                response.setContentType("application/x-unknown");
                setContentDisposition(response, request.outputFileName);
            } else if (request.format != null) {
                response.setContentType("image/" + request.format.toLowerCase());
            }

            ImageIO.write(image, request.format, response.getOutputStream());
        }
    }

    private void exportAsPDF(DiagramExportRequest request, HttpServletResponse response)
        throws DocumentException, IOException, SAXException, ParserConfigurationException
    {
        response.setContentType("application/pdf");

        if (request.outputFileName != null) {
            setContentDisposition(response, request.outputFileName);
        }

        Rectangle pageSize = PageSize.A4;
        if (request.width != null && request.height != null) {
            // The added pixel fixes the PDF offset.
            pageSize = new Rectangle(request.width + 1, request.height + 1);
        }

        Document document = new Document(pageSize);
        PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        mxGraphicsCanvas2D canvas = this.canvasFactory
            .createCanvas(writer.getDirectContent().createGraphics(pageSize.getWidth(), pageSize.getHeight()));

        // Fixes PDF offset.
        canvas.translate(1, 1);

        renderXML(request.xml, canvas);
        canvas.getGraphics().dispose();
        document.close();
        writer.flush();
        writer.close();
    }

    private void setContentDisposition(HttpServletResponse response, String fileName)
    {
        response.setHeader("Content-Disposition",
            "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);
    }

    /**
     * Renders the given XML to the given canvas.
     * 
     * @param xml the XML to render
     * @param canvas the canvas where to render the XML
     */
    private void renderXML(String xml, mxICanvas2D canvas)
        throws SAXException, ParserConfigurationException, IOException
    {
        XMLReader reader = this.xmlReaderFactory.createXMLReader();
        reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
        reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        reader.setContentHandler(new mxSaxOutputHandler(canvas));
        reader.parse(new InputSource(new StringReader(xml)));
    }
}
