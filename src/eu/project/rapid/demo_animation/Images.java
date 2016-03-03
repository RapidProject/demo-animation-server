package eu.project.rapid.demo_animation;

import java.util.HashMap;
import java.util.Map;

import marvin.image.MarvinImage;
import marvin.io.MarvinImageIO;

public class Images {

  // All the components are down.
  public static final MarvinImage im_start_0 =
      MarvinImageIO.loadImage("resources/figs/start-0.png");

  // All components (DS and VMM) are up.
  public static final MarvinImage im_start_1 =
      MarvinImageIO.loadImage("resources/figs/start-1.png");
  // # Just a text "Finished"
  public static final MarvinImage im_finished =
      MarvinImageIO.loadImage("resources/figs/finished.png");

  // The DS is up and running.
  public static final MarvinImage im_ds_up_0 =
      MarvinImageIO.loadImage("resources/figs/ds-up/0.png");

  // The VMM is up and running and registers with the DS
  public static final MarvinImage im_vmm_register_0 =
      MarvinImageIO.loadImage("resources/figs/vmm-up-register-ds/0.png");
  public static final MarvinImage im_vmm_register_1 =
      MarvinImageIO.loadImage("resources/figs/vmm-up-register-ds/1.png");
  public static final MarvinImage im_vmm_register_2 =
      MarvinImageIO.loadImage("resources/figs/vmm-up-register-ds/2.png");
  public static final MarvinImage im_vmm_register_3 =
      MarvinImageIO.loadImage("resources/figs/vmm-up-register-ds/3.png");
  public static final MarvinImage im_vmm_register_4 =
      MarvinImageIO.loadImage("resources/figs/vmm-up-register-ds/4.png");
  public static final MarvinImage im_vmm_register_5 =
      MarvinImageIO.loadImage("resources/figs/vmm-up-register-ds/5.png");

  // Offload Virus scanning
  public static final MarvinImage im_virus_offload_0 =
      MarvinImageIO.loadImage("resources/figs/virus-offload/0.png");
  public static final MarvinImage im_virus_offload_1 =
      MarvinImageIO.loadImage("resources/figs/virus-offload/1.png");
  public static final MarvinImage im_virus_offload_2 =
      MarvinImageIO.loadImage("resources/figs/virus-offload/2.png");
  public static final MarvinImage im_virus_offload_3 =
      MarvinImageIO.loadImage("resources/figs/virus-offload/3.png");
  public static final MarvinImage im_virus_offload_4 =
      MarvinImageIO.loadImage("resources/figs/virus-offload/4.png");

  // Local Virus scanning
  public static final MarvinImage im_virus_local_0 =
      MarvinImageIO.loadImage("resources/figs/virus-local/0.png");
  public static final MarvinImage im_virus_local_1 =
      MarvinImageIO.loadImage("resources/figs/virus-local/1.png");
  public static final MarvinImage im_virus_local_2 =
      MarvinImageIO.loadImage("resources/figs/virus-local/2.png");

  // The UD registers NEW or PREV
  public static final Map<Integer, MarvinImage> prevImages = new HashMap<>();
  public static final Map<Integer, MarvinImage> newImages = new HashMap<>();

  static {
    for (int i = 1; i <= 13; i++) {
      prevImages.put(i, MarvinImageIO.loadImage("resources/figs/prev-" + i + ".png"));
      newImages.put(i, MarvinImageIO.loadImage("resources/figs/new-" + i + ".png"));
    }
  }

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
