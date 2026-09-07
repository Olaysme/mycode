package fullstacks.wd;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PortfolioController {

    @GetMapping({"/", "/home", "/Home.html"})
    public String home() {
        return "Home";
    }

    @GetMapping({"/services", "/Services.html", "/services.html"})
    public String services() {
        return "Services";
    }

    @GetMapping({"/about", "/About.html", "/about.html"})
    public String about() {
        return "About";
    }

    @GetMapping({"/projects", "/Projects.html", "/projects.html"})
    public String projects() {
        return "Projects";
    }

    @GetMapping({"/contact", "/get-in-touch", "/Getintouch.html"})
    public String contact() {
        return "Getintouch";
    }

    // @GetMapping({"/stack", "/Stack.html", "/stack.html"})
    // public String stack() {
    //     return "Stack";
    // }
}
