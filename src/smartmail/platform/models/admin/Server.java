package smartmail.platform.models.admin;

import smartmail.platform.exceptions.DatabaseException;
import smartmail.platform.meta.annotations.Column;
import smartmail.platform.orm.ActiveRecord;

import java.io.Serializable;
import java.sql.Date;

public class Server extends ActiveRecord implements Serializable {
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;

    @Column(name = "status_id", type = "integer", nullable = false)
    public int statusId;

    @Column(name = "provider_id", type = "integer", nullable = false)
    public int providerId;

    @Column(name = "server_type_id", type = "integer", nullable = false)
    public int serverTypeId;

    @Column(name = "name", type = "text", nullable = false, length = 100)
    public String name;

    @Column(name = "host_name", type = "text", nullable = false, length = 100)
    public String hostName;

    @Column(name = "main_ip", type = "text", nullable = false, length = 100)
    public String mainIp;

    @Column(name = "username", type = "text", nullable = false, length = 100)
    public String username;

    @Column(name = "password", type = "text", nullable = false, length = 100)
    public String password;

    @Column(name = "created_by", type = "integer", nullable = false)
    public int createdBy;

    @Column(name = "last_updated_by", type = "integer", nullable = true)
    public int lastUpdatedBy;

    @Column(name = "created_at", type = "date", nullable = false)
    public Date createdAt;

    @Column(name = "last_updated_at", type = "date", nullable = true)
    public Date lastUpdatedAt;

    @Column(name = "ssh_port", type = "integer", nullable = true)
    public int sshPort;

    @Column(name = "authorized_users", type = "text", nullable = true)
    public String authorizedUsers;

    @Column(name = "expiration_date", type = "date", nullable = true)
    public Date expirationDate;

    public Server() throws DatabaseException {
        setDatabase("master");
        setSchema("admin");
        setTable("servers");
    }

    public Server(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        setDatabase("master");
        setSchema("admin");
        setTable("servers");
        load();
    }
}
