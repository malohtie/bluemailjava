package smartmail.platform.models.lists;

import java.io.Serializable;
import smartmail.platform.exceptions.DatabaseException;
import smartmail.platform.meta.annotations.Column;
import smartmail.platform.orm.ActiveRecord;

public class HardBounce extends ActiveRecord implements Serializable {
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;

    @Column(name = "email", type = "text", nullable = false, length = 100)
    public String email;

    public HardBounce() throws DatabaseException {
        setDatabase("lists");
        setSchema("");
        setTable("hard_bounce");
    }

    public HardBounce(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        setDatabase("lists");
        setSchema("");
        setTable("hard_bounce");
        load();
    }
}
