package fullstacks.wd;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteContentRepository extends JpaRepository<SiteContentEntry, String> {
}
