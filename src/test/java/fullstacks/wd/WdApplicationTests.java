package fullstacks.wd;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WdApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SiteContentService siteContentService;

	@Autowired
	private SiteContentRepository siteContentRepository;

	@Autowired
	private SiteImageStorage siteImageStorage;

	@Test
	void contextLoads() {
	}

	@Test
	void adminContentIsPersistedToDatabase() {
		int savedFields = siteContentService.update(Map.of("home.headline", "Saved through MySQL"));

		assertEquals(1, savedFields);
		assertEquals("Saved through MySQL",
				siteContentRepository.findById("home.headline").orElseThrow().getValue());
	}

	@Test
	void saveAndPublishUsesANormalFormWithoutTriggeringImageUpload() throws Exception {
		mockMvc.perform(get("/admin").sessionAttr("adminAuthenticated", true))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"<form method=\"post\" action=\"/admin/content\">")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString(
								"action=\"/admin/content\" enctype=\"multipart/form-data\""))));

		mockMvc.perform(post("/admin/content")
					.sessionAttr("adminAuthenticated", true)
					.param("home.headline", "Saved without an image"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin"));

		assertEquals("Saved without an image",
				siteContentRepository.findById("home.headline").orElseThrow().getValue());
	}

	@Test
	void adminCanAddAProject() throws Exception {
		int previousCount = siteContentService.getProjectCount();

		mockMvc.perform(post("/admin/project/add")
					.sessionAttr("adminAuthenticated", true))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin"));

		int newProject = previousCount + 1;
		assertEquals(newProject, siteContentService.getProjectCount());
		assertEquals("New project", siteContentRepository
				.findById("project." + newProject + ".title").orElseThrow().getValue());

		mockMvc.perform(get("/projects"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("New project")));
	}

	@Test
	void largeUploadedImageIsResizedForTheWeb() throws Exception {
		BufferedImage source = new BufferedImage(2500, 100, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(source, "png", bytes);
		MockMultipartFile upload = new MockMultipartFile(
				"homeImage", "large.png", "image/png", bytes.toByteArray());

		String imageUrl = siteImageStorage.store(upload);
		Path storedImage = siteImageStorage.getUploadDirectory()
				.resolve(imageUrl.substring("/uploads/".length()));
		try {
			BufferedImage optimized = ImageIO.read(storedImage.toFile());
			assertEquals(1920, optimized.getWidth());
			assertTrue(Files.size(storedImage) < bytes.size());
		} finally {
			Files.deleteIfExists(storedImage);
		}
	}

}
