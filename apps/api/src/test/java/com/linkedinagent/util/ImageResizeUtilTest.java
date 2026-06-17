package com.linkedinagent.util;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageResizeUtilTest {

    @Test
    void resizesImageToTargetSquare() throws Exception {
        byte[] original = createPng(800, 600);

        byte[] resized = ImageResizeUtil.resizeToSquarePng(original, 1080);

        BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(resized));
        assertThat(image.getWidth()).isEqualTo(1080);
        assertThat(image.getHeight()).isEqualTo(1080);
    }

    @Test
    void returnsOriginalBytesWhenAlreadyTargetSize() throws Exception {
        byte[] original = createPng(1080, 1080);

        byte[] resized = ImageResizeUtil.resizeToSquarePng(original, 1080);

        assertThat(resized).isEqualTo(original);
    }

    @Test
    void rejectsEmptyBytes() {
        assertThatThrownBy(() -> ImageResizeUtil.resizeToSquarePng(new byte[0], 1080))
                .isInstanceOf(com.linkedinagent.exception.AgentException.class);
    }

    private byte[] createPng(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
