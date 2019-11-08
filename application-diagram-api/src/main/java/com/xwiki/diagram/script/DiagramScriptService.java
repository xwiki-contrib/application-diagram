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
package com.xwiki.diagram.script;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import com.xwiki.diagram.DiagramConfiguration;
import com.xwiki.diagram.internal.DiagramExportRequest;
import com.xwiki.diagram.internal.DiagramExportRequestFactory;
import com.xwiki.diagram.internal.HTTPDiagramExporter;

/**
 * Script services for the Diagram application.
 * 
 * @version $Id$
 * @since 1.11
 */
@Component
@Named("diagram")
@Singleton
@Unstable
public class DiagramScriptService implements ScriptService
{
    @Inject
    private Logger logger;

    @Inject
    private DiagramConfiguration configuration;

    @Inject
    private HTTPDiagramExporter diagramExporter;

    @Inject
    private DiagramExportRequestFactory diagramExportRequestFactory;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Exports a diagram.
     * 
     * @param request the export request
     * @param response the response to write the output to
     * @throws IOException if it fails to write the output to the response
     */
    public void export(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        DiagramExportRequest diagramExportRequest = validateRequest(request, response);
        if (diagramExportRequest == null) {
            return;
        }

        try {
            long start = System.currentTimeMillis();

            this.diagramExporter.export(diagramExportRequest, response);

            long memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long duration = System.currentTimeMillis() - start;

            this.logger
                .debug("Diagram export: ip=" + request.getRemoteAddr() + " referer=\"" + request.getHeader("Referer")
                    + "\" length=" + request.getContentLength() + " memory=" + memory + " duration=" + duration);
        } catch (OutOfMemoryError e) {
            Runtime runtime = Runtime.getRuntime();
            this.logger.error(
                "Out of memory while exporting diagram. "
                    + "Memory status: free memory [{}], total memory [{}], max memory [{}].",
                runtime.freeMemory(), runtime.totalMemory(), runtime.maxMemory(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            this.logger.error("Failed to export diagram.", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private DiagramExportRequest validateRequest(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        if (request.getContentLength() < this.configuration.getMaxRequestSize()) {
            try {
                DiagramExportRequest diagramExportRequest =
                    this.diagramExportRequestFactory.createDiagramExportRequest(request);
                if (this.authorization.hasAccess(Right.VIEW, diagramExportRequest.diagramReference)) {
                    if (diagramExportRequest.xml == null) {
                        diagramExportRequest.xml = this.documentAccessBridge
                            .getDocumentContentForDefaultLanguage(diagramExportRequest.diagramReference);
                    }
                    if (!StringUtils.isBlank(diagramExportRequest.xml)) {
                        return diagramExportRequest;
                    } else {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Nothing to export.");
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "You are not allowed to view [" + diagramExportRequest.diagramReference + "] diagram.");
                }
            } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                this.logger.warn("Invalid diagram export request. Root cause is [{}].",
                    ExceptionUtils.getRootCauseMessage(e));
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } catch (Exception e) {
                this.logger.error("Failed to process diagram export request.", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ExceptionUtils.getRootCauseMessage(e));
            }
        } else {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                "The request content length exceeds the [" + this.configuration.getMaxRequestSize() + "] limit.");
        }

        return null;
    }
}
