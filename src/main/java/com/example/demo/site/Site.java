package com.example.demo.site;

import lombok.*;
import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sites", indexes = {
        @Index(name = "idx_site_active", columnList = "active")
})
public class Site {

    @Id
    @SequenceGenerator(
            name = "site_sequence",
            sequenceName = "site_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            generator = "site_sequence",
            strategy = GenerationType.SEQUENCE
    )
    private Long id;

    @Column(nullable = false)
    private String siteName;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Boolean active = true;
}