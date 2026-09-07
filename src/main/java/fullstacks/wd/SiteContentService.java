package fullstacks.wd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SiteContentService {

    private static final String PROJECT_COUNT_KEY = "projects.count";
    private static final Pattern PROJECT_KEY = Pattern.compile(
            "^project\\.(\\d{1,3})\\.(title|description|tags|image)$");

    private final SiteContentRepository repository;
    private final Path legacyContentFile;
    private final LinkedHashMap<String, String> defaults = new LinkedHashMap<>();

    public SiteContentService(SiteContentRepository repository,
                              @Value("${site.legacy-content-file:data/site-content.properties}") String legacyContentFile) {
        this.repository = repository;
        this.legacyContentFile = Path.of(legacyContentFile).toAbsolutePath().normalize();
        addDefaults();
    }

    @PostConstruct
    public void initializeContent() {
        Map<String, String> initialValues = new LinkedHashMap<>(defaults);
        if (repository.count() == 0) {
            initialValues.putAll(loadLegacyContent());
        }

        Map<String, SiteContentEntry> existing = new LinkedHashMap<>();
        repository.findAll().forEach(entry -> existing.put(entry.getKey(), entry));
        List<SiteContentEntry> missing = new ArrayList<>();
        initialValues.forEach((key, value) -> {
            if (!existing.containsKey(key)) {
                missing.add(new SiteContentEntry(key, value));
            }
        });
        if (!missing.isEmpty()) {
            repository.saveAll(missing);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, String> getAll() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        defaults.forEach(result::put);
        repository.findAll().forEach(entry -> {
            if (isEditableKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        });
        int projectCount = parseProjectCount(result.get(PROJECT_COUNT_KEY));
        for (int number = 1; number <= projectCount; number++) {
            addMissingProjectFields(result, number);
        }
        return result;
    }

    @Transactional
    public int update(Map<String, String> submitted) {
        List<SiteContentEntry> changes = new ArrayList<>();
        submitted.forEach((key, value) -> {
            if (!PROJECT_COUNT_KEY.equals(key) && isEditableKey(key)) {
                SiteContentEntry entry = repository.findById(key)
                        .orElseGet(() -> new SiteContentEntry(key, defaults.getOrDefault(key, "")));
                entry.setValue(value.strip());
                changes.add(entry);
            }
        });
        if (!changes.isEmpty()) {
            repository.saveAll(changes);
            repository.flush();
        }
        return changes.size();
    }

    @Transactional
    public int addProject() {
        int projectNumber = getProjectCount() + 1;
        if (projectNumber > 100) {
            throw new IllegalArgumentException("A maximum of 100 projects is supported.");
        }
        List<SiteContentEntry> entries = new ArrayList<>();
        SiteContentEntry countEntry = repository.findById(PROJECT_COUNT_KEY)
                .orElseGet(() -> new SiteContentEntry(PROJECT_COUNT_KEY, "3"));
        countEntry.setValue(Integer.toString(projectNumber));
        entries.add(countEntry);
        entries.add(new SiteContentEntry(projectKey(projectNumber, "title"), "New project"));
        entries.add(new SiteContentEntry(projectKey(projectNumber, "description"), ""));
        entries.add(new SiteContentEntry(projectKey(projectNumber, "tags"), ""));
        entries.add(new SiteContentEntry(projectKey(projectNumber, "image"), ""));
        repository.saveAll(entries);
        repository.flush();
        return projectNumber;
    }

    @Transactional(readOnly = true)
    public int getProjectCount() {
        return repository.findById(PROJECT_COUNT_KEY)
                .map(SiteContentEntry::getValue)
                .map(this::parseProjectCount)
                .orElse(3);
    }

    public List<Integer> getProjectNumbers() {
        int count = getProjectCount();
        List<Integer> numbers = new ArrayList<>(count);
        for (int number = 1; number <= count; number++) {
            numbers.add(number);
        }
        return numbers;
    }

    public boolean isProjectImageKey(String key) {
        Matcher matcher = PROJECT_KEY.matcher(key);
        return matcher.matches()
                && "image".equals(matcher.group(2))
                && Integer.parseInt(matcher.group(1)) <= getProjectCount();
    }

    private boolean isEditableKey(String key) {
        return defaults.containsKey(key) || PROJECT_KEY.matcher(key).matches();
    }

    private int parseProjectCount(String value) {
        try {
            return Math.max(3, Math.min(100, Integer.parseInt(value)));
        } catch (NumberFormatException exception) {
            return 3;
        }
    }

    private void addMissingProjectFields(Map<String, String> content, int number) {
        content.putIfAbsent(projectKey(number, "title"), "New project");
        content.putIfAbsent(projectKey(number, "description"), "");
        content.putIfAbsent(projectKey(number, "tags"), "");
        content.putIfAbsent(projectKey(number, "image"), "");
    }

    private String projectKey(int number, String field) {
        return "project." + number + "." + field;
    }

    private Map<String, String> loadLegacyContent() {
        LinkedHashMap<String, String> legacy = new LinkedHashMap<>();
        if (!Files.exists(legacyContentFile)) {
            return legacy;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(legacyContentFile)) {
            properties.load(input);
            defaults.keySet().forEach(key -> {
                if (properties.containsKey(key)) {
                    legacy.put(key, properties.getProperty(key));
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Could not migrate existing site content from " + legacyContentFile, exception);
        }
        return legacy;
    }

    private void put(String key, String value) {
        defaults.put(key, value);
    }

    private void addDefaults() {
        put("site.name", "Olayinka Solaja");
        put("home.status", "Available for freelance work");
        put("home.headline", "Websites and apps that pay for themselves.");
        put("home.intro", "Full-stack development for startups and small businesses — from a simple website to a full SaaS platform. Clear packages, fixed scope, delivered on time.");
        put("home.image", "/images/olayinka-hero.png");

        put("about.bio1", "I'm a full-stack developer and designer with over 3 years of experience building digital products that are fast, user-friendly, and scalable. I specialize in mobile apps, web development, SaaS platforms, and database architecture.");
        put("about.bio2", "From idea to launch, I help startups and businesses turn their vision into powerful, results-driven solutions — focused on clean code, modern design, and delivering on time, every time.");
        put("about.location", "Berlin, Germany");
        put("about.availability", "Remote or in-person");
        put("about.experience", "3+ years");

        String[][] services = {
                {"Web Development", "Responsive, high-performance websites and web apps."},
                {"Mobile Development", "Cross-platform apps with smooth performance and clean UI."},
                {"SaaS Development", "End-to-end platforms — architecture, dashboards, billing."},
                {"Application Development", "Custom software that turns requirements into working tools."},
                {"Database Development", "Scalable architecture and integrations that stay fast under load."},
                {"Web Design", "Modern interface design that supports the product, not decoration."}
        };
        for (int i = 0; i < services.length; i++) {
            put("service." + (i + 1) + ".title", services[i][0]);
            put("service." + (i + 1) + ".description", services[i][1]);
        }
        put("services.stack", "HTML, CSS, JavaScript, Vue.js, APIs & Back-End Logic, Authentication, Database Architecture, Third-Party Integrations");

        String[][] projects = {
                {"SaaS Dashboard Platform", "Multi-tenant admin dashboard with role-based access and real-time data views.", "Vue.js, Node.js, PostgreSQL"},
                {"Mobile Booking App", "Cross-platform app with live availability, push notifications, and in-app payments.", "React Native, REST API, Stripe"},
                {"Business Reporting Tool", "Internal web app turning operational data into clear, exportable reports.", "JavaScript, Chart.js, SQL"}
        };
        put(PROJECT_COUNT_KEY, Integer.toString(projects.length));
        for (int i = 0; i < projects.length; i++) {
            put("project." + (i + 1) + ".title", projects[i][0]);
            put("project." + (i + 1) + ".description", projects[i][1]);
            put("project." + (i + 1) + ".tags", projects[i][2]);
            put("project." + (i + 1) + ".image", "");
        }
        put("projects.note", "These three projects are placeholders — swap in real case studies and links whenever you're ready.");

        put("contact.heading", "Let's build something together");
        put("contact.intro", "Based in Berlin, available remote or in person. Reach out to talk through your project.");
        put("contact.email", "hello@olayinkasolaja.com");
        put("contact.linkedin", "https://www.linkedin.com");
    }
}
