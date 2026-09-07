package fullstacks.wd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class SiteImageStorage {

    private static final long MAX_IMAGE_SIZE = 100 * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 1920;
    private static final float JPEG_QUALITY = 0.82f;
    private final Path uploadDirectory;

    public SiteImageStorage(@Value("${site.upload.dir:data/uploads}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            return null;
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Images must be 100 MB or smaller.");
        }

        String extension;
        try (InputStream input = image.getInputStream()) {
            extension = detectExtension(input.readNBytes(12));
        }
        if (extension == null) {
            throw new IllegalArgumentException("Only JPG, PNG, GIF, and WebP images are allowed.");
        }

        Files.createDirectories(uploadDirectory);
        String filename = UUID.randomUUID() + extension;
        Path destination = uploadDirectory.resolve(filename).normalize();
        if (!destination.getParent().equals(uploadDirectory)) {
            throw new IllegalArgumentException("Invalid image filename.");
        }
        if (".jpg".equals(extension) || ".png".equals(extension)) {
            optimizeRasterImage(image, destination, extension);
        } else {
            try (InputStream input = image.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return "/uploads/" + filename;
    }

    public Path getUploadDirectory() {
        return uploadDirectory;
    }

    private void optimizeRasterImage(MultipartFile image, Path destination, String extension) throws IOException {
        BufferedImage source;
        try (InputStream input = image.getInputStream()) {
            source = ImageIO.read(input);
        }
        if (source == null || source.getWidth() < 1 || source.getHeight() < 1) {
            throw new IllegalArgumentException("The selected file is not a readable image.");
        }

        double scale = Math.min(1.0, (double) MAX_IMAGE_DIMENSION / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        boolean jpeg = ".jpg".equals(extension);
        BufferedImage optimized = new BufferedImage(width, height,
                jpeg ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = optimized.createGraphics();
        try {
            if (jpeg) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
            }
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
            source.flush();
        }

        if (jpeg) {
            writeJpeg(optimized, destination);
        } else {
            try (OutputStream output = Files.newOutputStream(destination)) {
                if (!ImageIO.write(optimized, "png", output)) {
                    throw new IOException("PNG encoder is unavailable.");
                }
            }
        }
        optimized.flush();
    }

    private void writeJpeg(BufferedImage image, Path destination) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(Files.newOutputStream(destination))) {
            writer.setOutput(output);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
    }

    private String detectExtension(byte[] header) {
        if (header.length >= 8
                && header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4e && header[3] == 0x47
                && header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a) {
            return ".png";
        }
        if (header.length >= 3 && header[0] == (byte) 0xff && header[1] == (byte) 0xd8 && header[2] == (byte) 0xff) {
            return ".jpg";
        }
        if (header.length >= 6 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F'
                && header[3] == '8' && (header[4] == '7' || header[4] == '9') && header[5] == 'a') {
            return ".gif";
        }
        if (header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return ".webp";
        }
        return null;
    }
}
