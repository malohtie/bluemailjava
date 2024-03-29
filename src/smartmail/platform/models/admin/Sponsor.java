package smartmail.platform.models.admin;

import smartmail.platform.exceptions.DatabaseException;
import smartmail.platform.meta.annotations.Column;
import smartmail.platform.orm.ActiveRecord;

import java.io.Serializable;
import java.sql.Date;

public class Sponsor extends ActiveRecord implements Serializable {
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;

    @Column(name = "status_id", type = "integer", nullable = false)
    public int statusId;

    @Column(name = "affiliate_id", type = "integer", nullable = false)
    public int affiliateId;

    @Column(name = "name", type = "text", nullable = false, length = 20)
    public String name;

    @Column(name = "website", type = "text", nullable = false)
    public String website;

    @Column(name = "username", type = "text", nullable = false, length = 100)
    public String username;

    @Column(name = "password", type = "text", nullable = false, length = 100)
    public String password;

    @Column(name = "api_key", type = "text", nullable = true)
    public String apiKey;

    @Column(name = "api_url", type = "text", nullable = true)
    public String apiUrl;

    @Column(name = "api_type", type = "text", nullable = true)
    public String apiType;

    @Column(name = "created_by", type = "integer", nullable = false)
    public int createdBy;

    @Column(name = "last_updated_by", type = "integer", nullable = true)
    public int lastUpdatedBy;

    @Column(name = "created_at", type = "date", nullable = false)
    public Date createdAt;

    @Column(name = "last_updated_at", type = "date", nullable = true)
    public Date lastUpdatedAt;

    public Sponsor() throws DatabaseException {
        setDatabase("master");
        setSchema("admin");
        setTable("sponsors");
    }

    public Sponsor(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        setDatabase("master");
        setSchema("admin");
        setTable("sponsors");
        load();
    }
}
