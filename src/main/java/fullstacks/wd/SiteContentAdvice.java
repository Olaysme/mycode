package fullstacks.wd;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.List;

@ControllerAdvice
public class SiteContentAdvice {

    private final SiteContentService siteContentService;

    public SiteContentAdvice(SiteContentService siteContentService) {
        this.siteContentService = siteContentService;
    }

    @ModelAttribute("site")
    public Map<String, String> siteContent() {
        return siteContentService.getAll();
    }

    @ModelAttribute("projectNumbers")
    public List<Integer> projectNumbers() {
        return siteContentService.getProjectNumbers();
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleOversizedUpload(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Upload too large. Each image must be 100 MB or smaller.");
        return "redirect:/admin";
    }
}
