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
import java.util.Collections;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Base class for implementing Diagram Application migrations.
 * 
 * @version $Id$
 * @since 1.11
 */
public abstract class AbstractDiagramMigration
{
    @Inject
    protected Logger logger;

    @Inject
    protected QueryManager queryManager;

    @Inject
    protected DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    /**
     * Performs the migration on the diagrams from the specified wiki.
     * 
     * @param wiki the wiki where to run the migration
     * @return the collection of diagrams that have been migrated
     */
    public Collection<DocumentReference> migrate(String wiki)
    {
        try {
            return getDiagramsToMigrate(wiki).stream().filter(this::migrate).collect(Collectors.toList());
        } catch (QueryException e) {
            this.logger.error("Failed to get the list of diagrams to migrate.", e);
            return Collections.emptyList();
        }
    }

    /**
     * @param wiki the wiki where to look for diagrams to migrate
     * @return the list of diagrams from the specified wiki that should be migrated
     * @throws QueryException if executing the query fails
     */
    protected abstract Collection<DocumentReference> getDiagramsToMigrate(String wiki) throws QueryException;

    /**
     * Migrates the specified diagram.
     * 
     * @param diagramReference the diagram to migrate
     * @return {@code true} if the specified diagram was migrated, {@code false} otherwise
     */
    protected abstract boolean migrate(DocumentReference diagramReference);
}
