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
package com.xwiki.diagram;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.stability.Unstable;

/**
 * Configuration properties for the Diagram application.
 * 
 * @version $Id$
 * @since 1.11
 */
@Component(roles = DiagramConfiguration.class)
@Singleton
@Unstable
public class DiagramConfiguration
{
    /**
     * @return the maximum size (in bytes) for export request payloads, 10485760 (10MB) by default
     */
    public int getMaxRequestSize()
    {
        return 10485760;
    }

    /**
     * @return the maximum area for exports, 10000x10000px by default
     */
    public int getMaxArea()
    {
        return 10000 * 10000;
    }
}
