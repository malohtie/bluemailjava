package smartmail.platform.models.admin;

import smartmail.platform.exceptions.DatabaseException;
import smartmail.platform.meta.annotations.Column;
import smartmail.platform.orm.ActiveRecord;

import java.io.Serializable;
import java.sql.Date;

public class OfferName extends ActiveRecord implements Serializable {
    @Column(name = "id", primary = true, autoincrement = true, type = "integer", nullable = false)
    public int id;

    @Column(name = "status_id", type = "integer", nullable = false)
    public int statusId;

    @Column(name = "offer_id", type = "integer", nullable = false)
    public int offerId;

    @Column(name = "value", type = "text", nullable = false)
    public String value;

    @Column(name = "created_by", type = "integer", nullable = false)
    public int createdBy;

    @Column(name = "last_updated_by", type = "integer", nullable = true)
    public int lastUpdatedBy;

    @Column(name = "created_at", type = "date", nullable = false)
    public Date createdAt;

    @Column(name = "last_updated_at", type = "date", nullable = true)
    public Date lastUpdatedAt;

    public OfferName() throws DatabaseException {
        setDatabase("master");
        setSchema("admin");
        setTable("offer_names");
    }

    public OfferName(Object primaryValue) throws DatabaseException {
        super(primaryValue);
        setDatabase("master");
        setSchema("admin");
        setTable("offer_names");
        load();
    }
}
