package com.javarush.kolosov.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Entity
@Table(name = "country")
@Getter
@Setter
@NoArgsConstructor
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 3)
    private String code;

    @Column(name = "code_2", nullable = false, length = 2)
    private String alternativeCode;

    @Column(nullable = false, length = 52)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private Continent continent;

    @Column(nullable = false, length = 26)
    private String region;

    @Column(name = "surface_area", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private BigDecimal surfaceArea;

    @Column(name = "indep_year")
    private Short independentYear;

    @Column(nullable = false)
    private Integer population;

    @Column(name = "life_expectancy", columnDefinition = "DECIMAL(3,1)")
    private BigDecimal lifeExpectancy;

    @Column(columnDefinition = "DECIMAL(10,2)")
    private BigDecimal gnp;

    @Column(name = "gnpo_id", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal gnpoId;

    @Column(name = "local_name", nullable = false, length = 45)
    private String localName;

    @Column(name = "government_form", nullable = false, length = 45)
    private String governmentForm;

    @Column(name = "head_of_state", length = 60)
    private String headOfState;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capital")
    private City city;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "country")
    private Set<CountryLanguage> languages;
}
