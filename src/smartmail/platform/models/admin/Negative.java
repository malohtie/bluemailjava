package smartmail.platform.models.admin;

import smartmail.platform.exceptions.DatabaseException;
import smartmail.platform.meta.annotations.Column;
import smartmail.platform.orm.ActiveRecord;

import java.io.Serializable;

public class Negative extends ActiveRecord implements Serializable {
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;

    @Column(name = "user_id", type = "integer", nullable = false)
    public int userId;

    @Column(name = "name", type = "text", nullable = false, length = 100)
    public String name;

    @Column(name = "value", type = "text", nullable = true, length = 200)
    public String value;

    public Negative() throws DatabaseException {
        setDatabase("master");
        setSchema("admin");
        setTable("negative");
    }

    public Negative(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        setDatabase("master");
        setSchema("admin");
        setTable("negative");
        load();
    }
}
