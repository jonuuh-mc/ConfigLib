package io.jonuuh.configlib.gui.elements;

import io.jonuuh.configlib.gui.GuiUtils;
import io.jonuuh.configlib.gui.ISettingsGui;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.text.DecimalFormat;

public class GuiSlider extends GuiInteractableElement
{
    protected final int pointerSize;
    protected final float radius;
    protected final double min;
    protected final double max;
    protected double normalizedVal;
    protected boolean mouseDown;

    public GuiSlider(ISettingsGui parent, int xPos, int yPos, int width, int height, float radius, double min, double max, double start)
    {
        super(parent, xPos, yPos, width, height);
        this.pointerSize = height;
//        this.radius = radius;
        this.radius = Math.min(radius, (height / 2F));
        this.min = min;
        this.max = max;

        setValue(start);
    }

    public GuiSlider(ISettingsGui parent, int xPos, int yPos, double min, double max, double start)
    {
        this(parent, xPos, yPos, 100, 10, 2, min, max, start);
    }

    public GuiSlider(ISettingsGui parent, int xPos, int yPos, double min, double max)
    {
        this(parent, xPos, yPos, min, max, 0.0);
    }

    // De-normalize
    public double getValue()
    {
        return normalizedVal * (max - min) + min;
    }

    // Normalize
    public void setValue(double value)
    {
        normalizedVal = (value - min) / (max - min);
        clamp();

        updateTooltip();
    }

    // can't really move by <1 pixel; normalized val is restricted by mouseX which is an int
    // technically all sliders are int sliders bc of this?
    public float getPointerXCenter()
    {
        float x = (float) (xPosition + (normalizedVal * width));
//        GuiUtils.drawRectangle(GL11.GL_LINE_LOOP, x, yPosition, 3, 3, new Color());

        // Clamp value
        x = Math.max(x, xPosition + (pointerSize / 2F));
        x = Math.min(x, xPosition + width - (pointerSize / 2F));

        return x;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY)
    {
        super.drawButton(mc, mouseX, mouseY);

        if (visible)
        {
            if (mouseDown)
            {
                update(mouseX);
            }

//            Color rightTrackColor = new Color();
            float pointerXCenter = getPointerXCenter();
            float trackHeight = (height / 3F);

            if (pointerXCenter != xPosition + (pointerSize / 2F))
            {
                // Slider track left
                GuiUtils.drawRoundedRect(GL11.GL_POLYGON, xPosition, yPosition + trackHeight, pointerXCenter - xPosition, trackHeight, radius, baseColor, true);
            }

            if (pointerXCenter != xPosition + width - (pointerSize / 2F)/*normalizedVal != 1.0*/)
            {
                // Slider track right
                GuiUtils.drawRoundedRect(GL11.GL_POLYGON, pointerXCenter, yPosition + trackHeight, width - (pointerXCenter - xPosition), trackHeight, radius, accentColor, true);
            }

            // Pointer
            GuiUtils.drawRoundedRect(GL11.GL_POLYGON, pointerXCenter - (pointerSize / 2F), yPosition, pointerSize, pointerSize, radius * 3, baseColor, true);
        }
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY)
    {
        if (mouseDown)
        {
            drawTooltip(mc);
        }
        else
        {
            super.mouseDragged(mc, mouseX, mouseY);
        }
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
    {
        boolean pressed = super.mousePressed(mc, mouseX, mouseY);

        if (pressed)
        {
            mouseDown = true;
            update(mouseX);
        }
        return pressed;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY)
    {
        mouseDown = false;
    }

    protected void update(int mouseX)
    {
        normalizedVal = (mouseX - xPosition) / (float) width;
        clamp();

        updateTooltip();

        sendChangeToParent();
    }

    protected void clamp()
    {
        // Clamp value (0,1)
        normalizedVal = Math.max(normalizedVal, 0.0F);
        normalizedVal = Math.min(normalizedVal, 1.0F);
    }

    protected void updateTooltip()
    {
        DecimalFormat df = new DecimalFormat("#.###");
        String str = /*df.format(normalizedVal) + " " + */df.format(getValue());

        int strWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(str) - 1;

        tooltip = new Tooltip(str);
        tooltip.x = getPointerXCenter() - (strWidth / 2F);
    }
}
