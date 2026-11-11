package tech.fuzir.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fuzir.client.mixin.PostEffectProcessorAccessor;

import java.io.IOException;

/**
 * Full-screen gaussian background blur for the glass effect, driven by the vanilla two-pass blur post shader.
 * The processor is created lazily and resized together with the main framebuffer.
 */
public final class Blur {
    private static final Logger LOGGER = LoggerFactory.getLogger("Fuzir/Blur");
    private static final Identifier SHADER = new Identifier("minecraft", "shaders/post/blur.json");

    private static PostEffectProcessor processor;
    private static int width = -1;
    private static int height = -1;
    private static boolean failed;

    private Blur() {
    }

    public static void render(float radius, float tickDelta) {
        if (failed || radius < 0.5f) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer main = mc.getFramebuffer();
        try {
            if (processor == null) {
                processor = new PostEffectProcessor(mc.getTextureManager(), mc.getResourceManager(), main, SHADER);
                width = -1;
            }
        } catch (IOException | RuntimeException e) {
            failed = true;
            LOGGER.warn("Background blur unavailable, falling back to a flat overlay", e);
            return;
        }
        if (main.textureWidth != width || main.textureHeight != height) {
            width = main.textureWidth;
            height = main.textureHeight;
            processor.setupDimensions(width, height);
        }
        for (PostEffectPass pass : ((PostEffectProcessorAccessor) processor).fuzir$getPasses()) {
            GlUniform uniform = pass.getProgram().getUniformByName("Radius");
            if (uniform != null) uniform.set(radius);
        }
        processor.render(tickDelta);
        main.beginWrite(false);
    }

    /** Releases GPU resources, e.g. after a resource reload. */
    public static void invalidate() {
        if (processor != null) {
            processor.close();
            processor = null;
        }
        failed = false;
    }
}
