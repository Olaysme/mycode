package fullstacks.wd;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "site_content")
public class SiteContentEntry {

    @Id
    @Column(name = "content_key", nullable = false, length = 120)
    private String key;

    @Column(name = "content_value", nullable = false, length = 10000)
    private String value;

    protected SiteContentEntry() {
    }

    public SiteContentEntry(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
