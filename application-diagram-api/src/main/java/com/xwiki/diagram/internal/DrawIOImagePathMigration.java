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
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

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

    @Override
    protected Collection<DocumentReference> getDiagramsToMigrate(String wiki) throws QueryException
    {
        // Look for the diagrams that contain draw.io WebJar paths.
        String statement =
            ", BaseObject as obj where obj.name = doc.fullName and obj.className = 'Diagram.DiagramClass'"
                + " and doc.fullName <> 'Diagram.DiagramTemplate' and doc.content like '%/webjars/%/draw.io/%'";
        Query query = this.queryManager.createQuery(statement, Query.HQL);
        // TODO: Use the "document" query filter instead when upgrading the parent version to XWiki 9.11.
        WikiReference wikiReference = new WikiReference(wiki);
        return query.setWiki(wiki).execute().stream()
            .map(result -> this.documentReferenceResolver.resolve(result.toString(), wikiReference))
            .collect(Collectors.toList());
    }

    @Override
    protected boolean migrate(DocumentReference diagramReference)
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            XWikiDocument document = xcontext.getWiki().getDocument(diagramReference, xcontext);
            document.setContent(IMAGE_WEBJAR_PATH.matcher(document.getContent()).replaceAll("image=img/"));
            document.setAuthorReference(xcontext.getUserReference());
            xcontext.getWiki().saveDocument(document, "Fixed draw.io image paths", xcontext);
            return true;
        } catch (Exception e) {
            this.logger.error("Failed to migrate diagram [{}].", diagramReference, e);
        }
        return false;
    }
}
