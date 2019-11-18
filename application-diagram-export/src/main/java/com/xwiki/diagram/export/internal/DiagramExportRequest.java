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

import java.awt.Color;
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;

/**
 * Encapsulates the data needed to perform a diagram export.
 * 
 * @version $Id$
 * @since 1.11
 * @see https://github.com/jgraph/draw-image-export2#common-parameters
 */
public class DiagramExportRequest
{
    /**
     * The PDF export format.
     */
    public static final String FORMAT_PDF = "pdf";

    /**
     * The PNG image export format.
     */
    public static final String FORMAT_PNG = "png";

    /**
     * The reference of the XWiki document holding the diagram to export. This is also used to resolve relative
     * references from the diagram source.
     */
    public DocumentReference diagramReference;

    /**
     * The target export format (e.g. {@link #FORMAT_PDF}, {@link #FORMAT_PNG}).
     */
    public String format;

    /**
     * The name of the output file. Falls back on the {@link #diagramReference} if not specified.
     */
    public String outputFileName;

    /**
     * The width of the canvas used to render the diagram before the export. For an image export this translates to the
     * width of the final image.
     */
    public Integer width;

    /**
     * The height of the canvas used to render the diagram before the export. For an image export this translates to the
     * height of the final image.
     */
    public Integer height;

    /**
     * The background color to use for export.
     */
    public Color backgroundColor;

    /**
     * The printing resolution.
     */
    public Integer dotsPerInch;

    /**
     * The scale to apply to the exported diagram.
     */
    public Double scale = 1D;

    /**
     * The width of the border to draw around the exported diagram.
     */
    public Integer borderWidth;

    /**
     * The XML source of the diagram to export. Falls back on the source of the {@link #diagramReference} loaded from
     * the database, if not specified.
     */
    public String xml;

    /**
     * Whether to embed the XML of the exported diagram in the generated file. Some export formats may not support this.
     */
    public boolean embedXML;

    /**
     * Whether to encode the output of the diagram export as Base64. This makes sense for image formats.
     */
    public boolean base64;

    /**
     * Extra configuration parameters to take into account when rendering the diagram (global variables, grid size, grid
     * steps, grid color, etc.).
     */
    public Map<String, Object> extras;
}
