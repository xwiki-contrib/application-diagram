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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Hashtable;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.mxgraph.canvas.mxGraphicsCanvas2D;

/**
 * The factory used to create the canvas where the diagrams are rendered before being exported.
 * 
 * @version $Id$
 * @since 1.11
 */
@Component(roles = DiagramCanvasFactory.class)
@Singleton
public class DiagramCanvasFactory
{
    private class DiagramCanvas extends mxGraphicsCanvas2D
    {
        /**
         * Caches custom images for the time of the request.
         */
        private final Hashtable<String, Image> shortCache = new Hashtable<String, Image>();

        /**
         * Creates a new canvas to render diagrams.
         * 
         * @param graphics2D the graphics the created canvas should be based on
         */
        DiagramCanvas(Graphics2D graphics2D)
        {
            super(graphics2D);
        }

        @Override
        public Image loadImage(String src)
        {
            // Uses local image cache by default
            Hashtable<String, Image> cache = shortCache;

            // Uses global image cache for local images (relative URLs)
            if (src.startsWith("/") || !src.contains("://")) {
                cache = imageCache;
            }

            Image image = cache.get(src);

            if (image == null) {
                image = super.loadImage(src);

                if (image != null) {
                    cache.put(src, image);
                } else {
                    cache.put(src, emptyImage);
                }
            } else if (image == emptyImage) {
                image = null;
            }

            return image;
        }
    };

    /**
     * Contains an empty image.
     */
    private static BufferedImage emptyImage;

    /**
     * Initializes the empty image.
     */
    static {
        try {
            emptyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        } catch (Exception e) {
            // Ignore.
        }
    }

    /**
     * Cache for all images.
     */
    private Hashtable<String, Image> imageCache = new Hashtable<String, Image>();

    /**
     * @param graphics2D the graphics the created canvas should be based on
     * @return a new canvas to render diagrams
     */
    public mxGraphicsCanvas2D createCanvas(Graphics2D graphics2D)
    {
        return new DiagramCanvas(graphics2D);
    }
}
