package models;



/**
 * Base entity for all JPA classes
 */
@Entity
public class BaseEntity {

    @Id
    protected Long id;

    public Long getId() {
        return id;
    }

}
