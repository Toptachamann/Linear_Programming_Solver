/**
 * Created by Timofey on 9/9/2017.
 */

package graphics.utils;

import java.awt.*;

public class GBC extends GridBagConstraints {
    public static final int NORTH = GridBagConstraints.NORTH, SOUTH = GridBagConstraints.SOUTH,
            WEST = GridBagConstraints.WEST, EAST = GridBagConstraints.EAST, CENTER = GridBagConstraints.CENTER;

    public static final int NONE = GridBagConstraints.NONE, VERTICAL = GridBagConstraints.VERTICAL,
            HORIZONTAL = GridBagConstraints.HORIZONTAL, BOTH = GridBagConstraints.BOTH;

    public static final int REMAINDER = GridBagConstraints.REMAINDER, RELATIVE = GridBagConstraints.RELATIVE;

    public GBC(int gridx, int gridy) {
        this.gridx = gridx;
        this.gridy = gridy;
    }

    public GBC(int gridx, int gridy, int gridwidth, int gridheight) {
        this.gridx = gridx;
        this.gridy = gridy;
        this.gridwidth = gridwidth;
        this.gridheight = gridheight;
    }

    public GBC(int gridx, int gridy, int gridwidth, int gridheight, int weightx, int weighty) {
        this.gridx = gridx;
        this.gridy = gridy;
        this.gridwidth = gridwidth;
        this.gridheight = gridheight;
        this.weightx = weightx;
        this.weighty = weighty;
    }

    public GBC setAnchor(int anchor) {
        this.anchor = anchor;
        return this;
    }

    public GBC setFill(int fill) {
        this.fill = fill;
        return this;
    }

    public GBC setWeight(int weightx, int weighty) {
        this.weightx = weightx;
        this.weighty = weighty;
        return this;
    }

    public GBC setInsets(int top, int left, int bottom, int right) {
        this.insets = new Insets(top, left, bottom, right);
        return this;
    }

    public GBC setIpad(int ipadx, int ipady) {
        this.ipadx = ipadx;
        this.ipady = ipady;
        return this;
    }
}







