package smartmail.platform.models.admin;

import smartmail.platform.exceptions.DatabaseException;
import smartmail.platform.meta.annotations.Column;
import smartmail.platform.orm.ActiveRecord;

import java.io.Serializable;
import java.sql.Date;

public class DataList extends ActiveRecord implements Serializable {
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;

    @Column(name = "name", type = "text", nullable = false, length = 100)
    public String name;

    @Column(name = "isp_id", type = "integer", nullable = false)
    public int ispId;

    @Column(name = "flag", type = "text", nullable = false, length = 50)
    public String flag;

    @Column(name = "created_by", type = "integer", nullable = false)
    public int createdBy;

    @Column(name = "last_updated_by", type = "integer", nullable = true)
    public int lastUpdatedBy;

    @Column(name = "created_at", type = "date", nullable = false)
    public Date createdAt;

    @Column(name = "last_updated_at", type = "date", nullable = true)
    public Date lastUpdatedAt;

    @Column(name = "authorized_users", type = "text", nullable = true)
    public String authorizedUsers;

    @Column(name = "status_id", type = "integer", nullable = false)
    public int statusId;

    public DataList() throws DatabaseException {
        setDatabase("master");
        setSchema("admin");
        setTable("data_lists");
    }

    public DataList(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        setDatabase("master");
        setSchema("admin");
        setTable("data_lists");
        load();
    }
}
