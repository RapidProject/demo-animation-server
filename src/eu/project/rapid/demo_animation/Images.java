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

import eu.project.rapid.common.RapidMessages;
import marvin.image.MarvinImage;
import marvin.io.MarvinImageIO;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

class Images {

    // images list
    private static final Map<String, MarvinImage> images = new HashMap<>();

    static {
        readImages(Paths.get("resources/figs/"));
    }

    static void readImages(Path path) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
//                System.out.println(entry);
                if (Files.isDirectory(entry)) {
                    readImages(entry);
                }
                else {
                    if (entry.toString().endsWith(".png")) {
                        String imgName = entry.getFileName().toString();
                        imgName = imgName.substring(0, imgName.lastIndexOf(".png"));
                        String imgPath = entry.toString();
//                        System.out.println(imgName + " - " + imgPath);
                        images.put(imgName, MarvinImageIO.loadImage(imgPath));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static MarvinImage getImage(RapidMessages.AnimationMsg msg) {
        return getImage(msg.toString());
    }

    static MarvinImage getImage(String imgName) {
        return images.get(imgName);
    }
}
