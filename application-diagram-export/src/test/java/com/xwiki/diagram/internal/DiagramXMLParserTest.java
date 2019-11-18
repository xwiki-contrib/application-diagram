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

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;
import com.xwiki.diagram.export.internal.DiagramXMLParser;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link DiagramXMLParser}.
 * 
 * @version $Id$
 * @since 1.11
 */
public class DiagramXMLParserTest
{
    @Rule
    public final MockitoComponentMockingRule<DiagramXMLParser> mocker =
        new MockitoComponentMockingRule<>(DiagramXMLParser.class);

    @Test
    public void parseCompressedDiagram() throws Exception
    {
        String xml = getDiagramXML("/compressedDiagram.xml");
        mxGraph graph = this.mocker.getComponentUnderTest().parse(xml);
        mxRectangle bounds = graph.getGraphBounds();
        assertEquals(80, (int) bounds.getWidth());
        assertEquals(80, (int) bounds.getHeight());
    }

    private String getDiagramXML(String fileName) throws IOException
    {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }
}
