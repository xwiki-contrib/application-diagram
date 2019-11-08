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
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;

import com.mxgraph.io.mxCodec;
import com.mxgraph.online.Utils;
import com.mxgraph.util.mxBase64;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;

/**
 * Parses an XML export of a diagram.
 * 
 * @version $Id$
 * @since 1.11
 */
@Component(roles = DiagramXMLParser.class)
@Singleton
public class DiagramXMLParser
{
    private static final String TAG_DIAGRAM = "diagram";

    private static final String TAG_MX_GRAPH_MODEL = "mxGraphModel";

    /**
     * Allow TAB, LF and CR.
     */
    private static final Set<Integer> ALLOWED_CONTROL_CHARS = new HashSet<>(Arrays.asList(9, 10, 13));

    @Inject
    private Logger logger;

    /**
     * @param xml the diagram XML
     * @return the corresponding diagram object
     */
    public mxGraph parse(String xml)
    {
        Element graphModelElement = extractGraphModel(mxXmlUtils.parseXml(xml));
        if (graphModelElement == null) {
            return null;
        }

        mxCodec decoder = new mxCodec(graphModelElement.getOwnerDocument());
        mxGraph graph = new mxGraph();
        decoder.decode(graphModelElement, graph.getModel());
        return graph;
    }

    // See Editor.extractGraphModel
    // https://github.com/jgraph/drawio/blob/master/src/main/webapp/js/diagramly/Editor.js
    private Element extractGraphModel(Document document)
    {
        Element graphModel = document.getDocumentElement();
        Element diagramElement = getDiagramElement(document);
        if (diagramElement != null) {
            graphModel = parseDiagram(diagramElement);
        }
        if (graphModel != null && !TAG_MX_GRAPH_MODEL.equalsIgnoreCase(graphModel.getTagName())) {
            graphModel = null;
        }
        return graphModel;
    }

    private Element getDiagramElement(Document document)
    {
        Element root = document.getDocumentElement();
        if (TAG_DIAGRAM.equalsIgnoreCase(root.getTagName())) {
            return root;
        } else if ("mxfile".equalsIgnoreCase(root.getTagName())) {
            NodeList diagrams = root.getElementsByTagName(TAG_DIAGRAM);
            if (diagrams.getLength() > 0) {
                return (Element) diagrams.item(0);
            }
        }
        return null;
    }

    // See Editor.parseDiagramNode
    // https://github.com/jgraph/drawio/blob/master/src/main/webapp/js/diagramly/Editor.js
    private Element parseDiagram(Element diagramElement)
    {
        Element firstChildElement = getFirstChildElement(diagramElement);
        String text = diagramElement.getTextContent().trim();
        if (firstChildElement != null && TAG_MX_GRAPH_MODEL.equalsIgnoreCase(firstChildElement.getTagName())) {
            return firstChildElement;
        } else if (!text.isEmpty()) {
            try {
                String xml = decompress(text);
                if (!StringUtils.isBlank(xml)) {
                    return mxXmlUtils.parseXml(xml).getDocumentElement();
                }
            } catch (IOException e) {
                this.logger.error("Failed to decompress diagram XML.", e);
            }
        }
        return null;
    }

    private Element getFirstChildElement(Element parent)
    {
        Node child = parent.getFirstChild();
        while (child != null && child.getNodeType() != Node.ELEMENT_NODE) {
            child = child.getNextSibling();
        }
        return (Element) child;
    }

    // See Graph.decompress
    // https://github.com/jgraph/mxgraph2/blob/master/javascript/examples/grapheditor/www/js/Graph.js
    private String decompress(String text) throws IOException
    {
        byte[] base64Ddecoded = mxBase64.decodeFast(text);
        String inflated = Utils.inflate(base64Ddecoded);
        String urlDecoded = URLDecoder.decode(inflated, "UTF-8");
        return zapGremlins(urlDecoded);
    }

    // See Graph.zapGremlins
    // https://github.com/jgraph/mxgraph2/blob/master/javascript/examples/grapheditor/www/js/Graph.js
    private String zapGremlins(String text)
    {
        StringBuilder result = new StringBuilder();
        text.chars().filter(this::isNotGremlin).forEach(result::appendCodePoint);
        return result.toString();
    }

    private boolean isNotGremlin(int code)
    {
        // Removes all control chars except TAB, LF and CR.
        return (code >= 32 || ALLOWED_CONTROL_CHARS.contains(code)) && code != 0xFFFF && code != 0xFFFE;
    }
}
