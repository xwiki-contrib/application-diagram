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
import java.util.function.BiFunction;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.xml.sax.SAXException;
import org.xwiki.component.annotation.Component;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.canvas.mxICanvas;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxCellRenderer.CanvasFactory;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxGraph;
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
    private Logger logger;

    @Inject
    private DiagramXMLParser xmlParser;

    /**
     * Exports a diagram.
     * 
     * @param request the diagram export request
     * @param outputStream where to write the output to
     * @throws IOException if it fails to write the output to the response
     * @throws ParserConfigurationException if it fails to create an XML reader
     * @throws SAXException if it fails to parse the diagram XML
     * @throws DocumentException if it fails to generate the PDF
     */
    public void export(DiagramExportRequest request, OutputStream outputStream)
        throws IOException, DocumentException, SAXException, ParserConfigurationException
    {
        if (DiagramExportRequest.FORMAT_PDF.equals(request.format)) {
            exportAsPDF(request, outputStream);
        } else {
            exportAsImage(request, outputStream);
        }
    }

    protected void exportAsImage(DiagramExportRequest request, OutputStream outputStream)
        throws IOException, SAXException, ParserConfigurationException
    {
        BufferedImage image = mxUtils.createBufferedImage(request.width, request.height, request.backgroundColor);
        drawToCanvas(request, (width, height) -> {
            Graphics2D graphics2D = image.createGraphics();
            mxUtils.setAntiAlias(graphics2D, true, true);
            return graphics2D;
        });

        ImageIO.write(image, request.format, outputStream);
    }

    protected void exportAsPDF(DiagramExportRequest request, OutputStream outputStream)
        throws DocumentException, IOException, SAXException, ParserConfigurationException
    {
        Rectangle pageSize = PageSize.A4;
        if (request.width != null && request.height != null) {
            pageSize = new Rectangle(request.width, request.height);
        }

        Document document = new Document(pageSize);
        final PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        document.open();

        drawToCanvas(request, writer.getDirectContent()::createGraphics);

        document.close();
    }

    private void drawToCanvas(DiagramExportRequest request, BiFunction<Integer, Integer, Graphics2D> graphicsFactory)
    {
        mxGraph graph = this.xmlParser.parse(request.xml);
        if (graph == null) {
            this.logger.warn("The specified diagram is not valid and thus it can't be drawn.");
            return;
        }
        configureGraph(graph, request);

        mxRectangle clip = null;
        if (request.width != null && request.height != null) {
            clip = new mxRectangle(0, 0, request.width, request.height);
        }
        mxGraphics2DCanvas canvas =
            (mxGraphics2DCanvas) mxCellRenderer.drawCells(graph, null, request.scale, clip, new CanvasFactory()
            {
                public mxICanvas createCanvas(int width, int height)
                {
                    return new mxGraphics2DCanvas(graphicsFactory.apply(width, height));
                }
            });
        if (canvas != null) {
            canvas.getGraphics().dispose();
        }
    }

    private void configureGraph(mxGraph graph, DiagramExportRequest request)
    {
        if (request.borderWidth != null) {
            graph.setBorder(request.borderWidth);
        }
        graph.setEnabled(false);
        graph.setHtmlLabels(true);
    }
}
