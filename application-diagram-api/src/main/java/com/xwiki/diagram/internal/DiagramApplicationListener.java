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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.event.ExtensionEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * Triggers the data migration when the Diagram Application is upgraded.
 * 
 * @version $Id$
 * @since 1.11
 */
@Component
@Named(DiagramApplicationListener.ROLE_HINT)
@Singleton
public class DiagramApplicationListener extends AbstractEventListener
{
    protected static final String ROLE_HINT = "DiagramApplicationListener";

    @Inject
    private Logger logger;

    @Inject
    private StoreSVGAsAttachmentMigrator svgMigrator;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * Default constructor.
     */
    public DiagramApplicationListener()
    {
        // TODO: Filter the extension id from here after we upgrade the parent version to XWiki 9.11.
        super(ROLE_HINT, new ExtensionUpgradedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof ExtensionUpgradedEvent) {
            ExtensionEvent extensionEvent = (ExtensionEvent) event;
            if ("com.xwiki.diagram:application-diagram".equals(extensionEvent.getExtensionId().getId())) {
                getTargetWikis(extensionEvent).forEach(this.svgMigrator::migrate);
            }
        }
    }

    private Collection<String> getTargetWikis(ExtensionEvent event)
    {
        if (event.hasNamespace() && event.getNamespace().startsWith("wiki:")) {
            return Collections.singleton(event.getNamespace().substring(5));
        } else {
            try {
                return this.wikiDescriptorManager.getAllIds();
            } catch (WikiManagerException e) {
                this.logger.error("Failed to get the list of wikis.", e);
                return Collections.emptySet();
            }
        }
    }
}