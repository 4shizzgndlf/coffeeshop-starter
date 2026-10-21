package ek.osnb.demo.coffeeshop.catalog;

import ek.osnb.demo.coffeeshop.common.InvalidInputException;
import jakarta.persistence.*;

@Entity
@Table(name = "categories")
class Category {

    static final int NAME_MAX_LENGTH = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = NAME_MAX_LENGTH)
    private String name;

    protected Category() {
        // Required by JPA
    }

    private Category(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("A category must have a name");
        }

        this.name = name.trim();
    }

    static Category create(String name) {
        return new Category(name);
    }

    Long getId() {
        return id;
    }

    String getName() {
        return name;
    }
}
