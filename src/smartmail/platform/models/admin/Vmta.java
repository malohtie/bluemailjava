package smartmail.platform.models.admin;

import smartmail.platform.exceptions.DatabaseException;
import smartmail.platform.meta.annotations.Column;
import smartmail.platform.orm.ActiveRecord;

import java.io.Serializable;
import java.sql.Date;

public class Vmta extends ActiveRecord implements Serializable {
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;

    @Column(name = "status_id", type = "integer", nullable = false)
    public int statusId;

    @Column(name = "server_id", type = "integer", nullable = false)
    public int serverId;

    @Column(name = "ip_id", type = "integer", nullable = false)
    public int ipId;

    @Column(name = "name", type = "text", nullable = false, length = 50)
    public String name;

    @Column(name = "type", type = "text", nullable = false, length = 50)
    public String type;

    @Column(name = "ip_value", type = "text", nullable = true, length = 100)
    public String ipValue;

    @Column(name = "domain", type = "text", nullable = true, length = 100)
    public String domain;

    @Column(name = "username", type = "text", nullable = true, length = 100)
    public String username;

    @Column(name = "password", type = "text", nullable = true, length = 100)
    public String password;

    @Column(name = "smtphost", type = "text", nullable = true, length = 100)
    public String smtphost;

    @Column(name = "created_by", type = "integer", nullable = false)
    public int createdBy;

    @Column(name = "last_updated_by", type = "integer", nullable = true)
    public int lastUpdatedBy;

    @Column(name = "created_at", type = "date", nullable = false)
    public Date createdAt;

    @Column(name = "last_updated_at", type = "date", nullable = true)
    public Date lastUpdatedAt;

    public Vmta() throws DatabaseException {
        setDatabase("master");
        setSchema("admin");
        setTable("vmtas");
    }

    public Vmta(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        setDatabase("master");
        setSchema("admin");
        setTable("vmtas");
        load();
    }
}
