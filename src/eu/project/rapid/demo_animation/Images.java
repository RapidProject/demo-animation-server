/*******************************************************************************
 * Copyright (C) 2015, 2016 RAPID EU Project
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *******************************************************************************/
package eu.project.rapid.demo_animation;

import java.util.HashMap;
import java.util.Map;

import marvin.image.MarvinImage;
import marvin.io.MarvinImageIO;

public class Images {

  // images list
  public static final Map<Integer, MarvinImage> images = new HashMap<>();

  static {
    for (int i = 1; i <= 25; i++) {
      images.put(i, MarvinImageIO.loadImage("resources/figs/100-dip/f-" + i + ".png"));
    }
  }

  public static final MarvinImage getImage(int i) {
    return images.get(i);
  }


}
