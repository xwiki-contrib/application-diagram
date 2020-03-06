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

import java.util.Collection;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Converts draw.io WebJar paths to draw.io WAR paths. This basically means removing the draw.io WebJar version from the
 * image paths.
 * 
 * @version $Id$
 * @since 1.11
 */
@Component(roles = DrawIOImagePathMigration.class)
@Singleton
public class DrawIOImagePathMigration extends AbstractDiagramMigration
{
    private static final Pattern IMAGE_WEBJAR_PATH = Pattern.compile("image=[\\w/%.]*\\/img/");

    private static final String IMAGE_WAR_PATH = "image=img/";

    @Inject
    @Named("document")
    private QueryFilter documentQueryFilter;

    @Override
    protected Collection<DocumentReference> getDiagramsToMigrate(String wiki) throws QueryException
    {
        // Look for the diagrams that contain draw.io WebJar paths.
        String statement =
            ", BaseObject as obj where obj.name = doc.fullName and obj.className = 'Diagram.DiagramClass'"
                + " and doc.fullName <> 'Diagram.DiagramTemplate' and "
                + "(doc.content like '%/webjars/%/draw.io/%' or doc.content like '%image=/img/%')";
        Query query = this.queryManager.createQuery(statement, Query.HQL);
        return query.setWiki(wiki).addFilter(documentQueryFilter).execute();
    }

    @Override
    protected boolean migrate(DocumentReference diagramReference)
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            XWikiDocument document = xcontext.getWiki().getDocument(diagramReference, xcontext);
            // Convert draw.io WebJar paths to draw.io WAR paths.
            String content = IMAGE_WEBJAR_PATH.matcher(document.getContent()).replaceAll(IMAGE_WAR_PATH);
            // Fix draw.io WAR paths.
            content = content.replace("image=/img/", IMAGE_WAR_PATH);
            document.setContent(content);
            // Preserve the diagram author.
            xcontext.getWiki().saveDocument(document, "Fixed draw.io image paths", xcontext);
            return true;
        } catch (Exception e) {
            this.logger.error("Failed to migrate diagram [{}].", diagramReference, e);
        }
        return false;
    }
}
