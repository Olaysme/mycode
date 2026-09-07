package fullstacks.wd;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class AdminController {

    private static final String AUTHENTICATED = "adminAuthenticated";
    private final SiteContentService siteContentService;
    private final SiteImageStorage imageStorage;
    private final String adminPassword;

    public AdminController(SiteContentService siteContentService,
                           SiteImageStorage imageStorage,
                           @Value("${ADMIN_PASSWORD:change-me}") String adminPassword) {
        this.siteContentService = siteContentService;
        this.imageStorage = imageStorage;
        this.adminPassword = adminPassword;
    }

    @GetMapping("/admin/login")
    public String loginPage(HttpSession session, Model model) {
        if (isAuthenticated(session)) {
            return "redirect:/admin";
        }
        return "AdminLogin";
    }

    @PostMapping("/admin/login")
    public String login(@RequestParam String password, HttpServletRequest request, Model model) {
        if (secureEquals(password, adminPassword)) {
            HttpSession session = request.getSession();
            request.changeSessionId();
            session.setAttribute(AUTHENTICATED, true);
            return "redirect:/admin";
        }
        model.addAttribute("error", "Incorrect password.");
        return "AdminLogin";
    }

    @GetMapping("/admin")
    public String dashboard(HttpSession session) {
        return isAuthenticated(session) ? "Admin" : "redirect:/admin/login";
    }

    @GetMapping("/admin/content")
    public String contentPage(HttpSession session) {
        return isAuthenticated(session) ? "redirect:/admin" : "redirect:/admin/login";
    }

    @GetMapping("/admin/logout")
    public String logoutPage(HttpSession session) {
        return isAuthenticated(session) ? "redirect:/admin" : "redirect:/admin/login";
    }

    @GetMapping("/admin/image")
    public String imageUploadPage(HttpSession session) {
        return isAuthenticated(session) ? "redirect:/admin" : "redirect:/admin/login";
    }

    @PostMapping("/admin/content")
    public String save(@RequestParam Map<String, String> fields,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }
        try {
            Map<String, String> updatedFields = new LinkedHashMap<>(fields);
            int savedFields = siteContentService.update(updatedFields);
            redirectAttributes.addFlashAttribute("success", savedFields + " fields saved to MySQL and published.");
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("error", "MySQL save failed: " + exception.getMostSpecificCause().getMessage());
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/project/add")
    public String addProject(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }
        try {
            int projectNumber = siteContentService.addProject();
            redirectAttributes.addFlashAttribute("success",
                    "Project " + projectNumber + " added. Add its details below, then save and publish.");
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("error", "MySQL save failed: " + exception.getMostSpecificCause().getMessage());
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/image")
    public String uploadImage(@RequestParam String slot,
                              @RequestParam MultipartFile image,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }
        if (!"home.image".equals(slot) && !siteContentService.isProjectImageKey(slot)) {
            redirectAttributes.addFlashAttribute("error", "Invalid image destination.");
            return "redirect:/admin";
        }
        try {
            String imageUrl = imageStorage.store(image);
            if (imageUrl == null) {
                throw new IllegalArgumentException("Choose an image before clicking Upload image.");
            }
            siteContentService.update(Map.of(slot, imageUrl));
            redirectAttributes.addFlashAttribute("success", "Image optimized, saved to MySQL, and published.");
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("error", "MySQL save failed: " + exception.getMostSpecificCause().getMessage());
        } catch (IOException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }

    private boolean isAuthenticated(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(AUTHENTICATED));
    }

    private boolean secureEquals(String submitted, String expected) {
        return MessageDigest.isEqual(
                submitted.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }

}
