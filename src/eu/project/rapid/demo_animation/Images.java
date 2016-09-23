/*******************************************************************************
 * Copyright (C) 2015, 2016 RAPID EU Project
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *******************************************************************************/
package eu.project.rapid.demo_animation;

import java.util.HashMap;
import java.util.Map;

import marvin.image.MarvinImage;
import marvin.io.MarvinImageIO;

public class Images {

  // All the components are down.
  //public static final MarvinImage im_start_0 =
      //MarvinImageIO.loadImage("resources/figs/start-0.png");

  // All components (DS and VMM) are up.
  //public static final MarvinImage im_start_1 =
    //  MarvinImageIO.loadImage("resources/figs/start-1.png");
  // # Just a text "Finished"
  //public static final MarvinImage im_finished =
      //MarvinImageIO.loadImage("resources/figs/finished.png");


  // The UD registers NEW or PREV
  public static final Map<Integer, MarvinImage> prevImages = new HashMap<>();
  public static final Map<Integer, MarvinImage> newImages = new HashMap<>();

  /*static {
    for (int i = 1; i <= 13; i++) {
      prevImages.put(i, MarvinImageIO.loadImage("resources/figs/prev-" + i + ".png"));
      newImages.put(i, MarvinImageIO.loadImage("resources/figs/new-" + i + ".png"));
    }
  }*/

  public static final MarvinImage getUdRegisterImage(int i, boolean prev) {
    if (prev) {
      return prevImages.get(i);
    }
    return newImages.get(i);
  }

  /**
   * Get the image from the NEW array
   * 
   * @param i
   * @return
   */
  public static final MarvinImage getUdRegisterImage(int i) {
    return getUdRegisterImage(i, false);
  }
}
