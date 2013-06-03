package com.littleinc.MessageMe.widget;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.ui.DoodleComposerActivity.OnPaintUpdateListener;

public class ColorPicker extends HorizontalScrollView {

    private LinearLayout container;

    private List<ColorIcon> colorIcons;

    private OnPaintUpdateListener listener;

    private DoodleBrushColor selectedColor = DoodleBrushColor.COLOR_000;

    public ColorPicker(Context context) {
        super(context);
        init();
    }

    public ColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        colorIcons = new LinkedList<ColorIcon>();
        container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.HORIZONTAL);

        for (DoodleBrushColor doodleColor : DoodleBrushColor
                .getDoodleBrushColors()) {
            ColorIcon colorIcon = (ColorIcon) LayoutInflater.from(getContext())
                    .inflate(R.layout.color_icon, container, false);
            colorIcon.setDoodleColor(doodleColor);

            if (DoodleBrushColor.COLOR_000 == doodleColor) {
                colorIcon.setSelected(true);
            }

            container.addView(colorIcon);
            colorIcons.add(colorIcon);
        }

        addView(container);
    }

    public void setListener(OnPaintUpdateListener listener) {
        this.listener = listener;

        for (ColorIcon colorIcon : colorIcons) {
            colorIcon.setListener(this.listener);
        }
    }

    public DoodleBrushColor getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(DoodleBrushColor selectedColor) {
        this.selectedColor = selectedColor;

        for (ColorIcon colorIcon : colorIcons) {
            colorIcon.setSelected(false);
            if (colorIcon.getDoodleColor() == this.selectedColor) {
                colorIcon.setSelected(true);
            }
        }
    }

    public enum DoodleBrushSize {
        SIZE_00(R.dimen.size_00), SIZE_01(R.dimen.size_01), SIZE_02(
                R.dimen.size_02), SIZE_03(R.dimen.size_03);

        private int dimenResId;

        private DoodleBrushSize(int dimenResId) {
            this.dimenResId = dimenResId;
        }

        public int getDimenResId() {
            return dimenResId;
        }

        public static List<DoodleBrushSize> getDoodleBrushSizes() {
            List<DoodleBrushSize> doodleBrushSizes = new LinkedList<DoodleBrushSize>();

            doodleBrushSizes.add(DoodleBrushSize.SIZE_00);
            doodleBrushSizes.add(DoodleBrushSize.SIZE_01);
            doodleBrushSizes.add(DoodleBrushSize.SIZE_02);
            doodleBrushSizes.add(DoodleBrushSize.SIZE_03);

            return doodleBrushSizes;
        }
    }

    public enum DoodleBrushColor {
        COLOR_000(R.color.color_000), COLOR_001(R.color.color_001), COLOR_002(
                R.color.color_002), COLOR_003(R.color.color_003), COLOR_004(
                R.color.color_004), COLOR_005(R.color.color_005), COLOR_006(
                R.color.color_006), COLOR_007(R.color.color_007), COLOR_008(
                R.color.color_008), COLOR_009(R.color.color_009), COLOR_010(
                R.color.color_010), COLOR_011(R.color.color_011), COLOR_012(
                R.color.color_012), COLOR_013(R.color.color_013), COLOR_014(
                R.color.color_014), COLOR_015(R.color.color_015), COLOR_016(
                R.color.color_016), COLOR_017(R.color.color_017), COLOR_018(
                R.color.color_018), COLOR_019(R.color.color_019), COLOR_020(
                R.color.color_020), COLOR_021(R.color.color_021), COLOR_022(
                R.color.color_022), COLOR_023(R.color.color_023), TRANSPARENT(
                android.R.color.transparent);

        private int colorResId;

        private DoodleBrushColor(int colorResId) {
            this.colorResId = colorResId;
        }

        public int getColorResId() {
            return colorResId;
        }

        public static List<DoodleBrushColor> getDoodleBrushColors() {
            List<DoodleBrushColor> doodleBrushColors = new LinkedList<ColorPicker.DoodleBrushColor>();

            doodleBrushColors.add(DoodleBrushColor.COLOR_000);
            doodleBrushColors.add(DoodleBrushColor.COLOR_001);
            doodleBrushColors.add(DoodleBrushColor.COLOR_002);
            doodleBrushColors.add(DoodleBrushColor.COLOR_003);
            doodleBrushColors.add(DoodleBrushColor.COLOR_004);
            doodleBrushColors.add(DoodleBrushColor.COLOR_005);
            doodleBrushColors.add(DoodleBrushColor.COLOR_006);
            doodleBrushColors.add(DoodleBrushColor.COLOR_007);
            doodleBrushColors.add(DoodleBrushColor.COLOR_008);
            doodleBrushColors.add(DoodleBrushColor.COLOR_009);
            doodleBrushColors.add(DoodleBrushColor.COLOR_010);
            doodleBrushColors.add(DoodleBrushColor.COLOR_011);
            doodleBrushColors.add(DoodleBrushColor.COLOR_012);
            doodleBrushColors.add(DoodleBrushColor.COLOR_013);
            doodleBrushColors.add(DoodleBrushColor.COLOR_014);
            doodleBrushColors.add(DoodleBrushColor.COLOR_015);
            doodleBrushColors.add(DoodleBrushColor.COLOR_016);
            doodleBrushColors.add(DoodleBrushColor.COLOR_017);
            doodleBrushColors.add(DoodleBrushColor.COLOR_018);
            doodleBrushColors.add(DoodleBrushColor.COLOR_019);
            doodleBrushColors.add(DoodleBrushColor.COLOR_020);
            doodleBrushColors.add(DoodleBrushColor.COLOR_021);
            doodleBrushColors.add(DoodleBrushColor.COLOR_022);
            doodleBrushColors.add(DoodleBrushColor.COLOR_023);

            return doodleBrushColors;
        }
    }
}