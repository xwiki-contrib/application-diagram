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

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Moves the diagram SVG from the diagram object to an attachment.
 * 
 * @version $Id$
 * @since 1.11
 */
@Component(roles = StoreSVGAsAttachmentMigration.class)
@Singleton
public class StoreSVGAsAttachmentMigration extends AbstractDiagramMigration
{
    private static final LocalDocumentReference DIAGRAM_CLASS_REFERENCE =
        new LocalDocumentReference("Diagram", "DiagramClass");

    private static final String DIAGRAM_ATTACHMENT_NAME = "diagram.svg";

    @Override
    protected Collection<DocumentReference> getDiagramsToMigrate(String wiki) throws QueryException
    {
        // Look for the diagram documents that still have the "svg" property.
        String statement = ", BaseObject as obj, LargeStringProperty as prop "
            + "where doc.fullName = obj.name and obj.className = 'Diagram.DiagramClass' and obj.id = prop.id.id"
            + " and prop.id.name = 'svg' and doc.fullName <> 'Diagram.DiagramTemplate'";
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
            // We don't overwrite the existing attachment because it may contain a more recent version of the diagram.
            XWikiAttachment attachment = document.getAttachment(DIAGRAM_ATTACHMENT_NAME);
            if (attachment == null) {
                BaseObject diagramObject = document.getXObject(DIAGRAM_CLASS_REFERENCE);
                if (diagramObject != null) {
                    String svg = diagramObject.getLargeStringValue("svg");
                    document.addAttachment(DIAGRAM_ATTACHMENT_NAME, new ByteArrayInputStream(svg.getBytes("UTF-8")),
                        xcontext);
                    synchronizeObject(diagramObject, xcontext);
                    document.setAuthorReference(xcontext.getUserReference());
                    xcontext.getWiki().saveDocument(document, "Moved diagram SVG to attachments", xcontext);
                    return true;
                }
            }
        } catch (Exception e) {
            this.logger.error("Failed to migrate diagram [{}].", diagramReference, e);
        }
        return false;
    }

    /**
     * Remove deprecated fields (properties deleted from the XClass) from an object.
     *
     * @param object the object to synchronize
     * @param context the current request context
     */
    private void synchronizeObject(BaseObject object, XWikiContext xcontext)
    {
        for (BaseProperty<?> property : object.getXClass(xcontext).getDeprecatedObjectProperties(object)) {
            object.removeField(property.getName());
        }
    }
}
