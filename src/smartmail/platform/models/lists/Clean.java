package smartmail.platform.models.lists;

import java.io.Serializable;
import smartmail.platform.exceptions.DatabaseException;
import smartmail.platform.meta.annotations.Column;
import smartmail.platform.orm.ActiveRecord;

public class Clean extends ActiveRecord implements Serializable {
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;

    @Column(name = "email", type = "text", nullable = false, length = 100)
    public String email;

    @Column(name = "fname", type = "text", nullable = true, length = 100)
    public String fname;

    @Column(name = "lname", type = "text", nullable = true, length = 100)
    public String lname;

    @Column(name = "offers_excluded", type = "text", nullable = true)
    public String offersExcluded;

    public Clean() throws DatabaseException {
        setDatabase("lists");
        setSchema("");
        setTable("");
    }

    public Clean(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        setDatabase("lists");
        setSchema("");
        setTable("");
        load();
    }
}
