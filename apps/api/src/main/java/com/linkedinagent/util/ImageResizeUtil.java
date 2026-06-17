package com.linkedinagent.util;

import com.linkedinagent.exception.AgentException;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public final class ImageResizeUtil {

    private ImageResizeUtil() {
    }

    public static byte[] resizeToSquarePng(byte[] imageBytes, int targetSize) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new AgentException("Image bytes are empty");
        }

        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (source == null) {
                throw new AgentException("Unable to decode image bytes");
            }

            if (source.getWidth() == targetSize && source.getHeight() == targetSize) {
                return imageBytes;
            }

            BufferedImage resized = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = resized.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source.getScaledInstance(targetSize, targetSize, Image.SCALE_SMOOTH), 0, 0, null);
            graphics.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(resized, "png", output);
            return output.toByteArray();
        } catch (IOException e) {
            log.error("Failed to resize image", e);
            throw new AgentException("Failed to resize image to " + targetSize + "x" + targetSize, e);
        }
    }
}
