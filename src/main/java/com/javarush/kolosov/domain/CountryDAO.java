package com.javarush.kolosov.domain;

import lombok.Getter;
import org.hibernate.SessionFactory;

import java.util.List;

@Getter
public class CountryDAO {
    private final SessionFactory sessionFactory;
    public CountryDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<Country> getAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("select c " +
                        "from Country c " +
                        "join fetch c.languages", Country.class)
                .list();
    }
}
