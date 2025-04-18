package com.javarush.kolosov.domain;

import lombok.Getter;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

@Getter
public class CityDAO {
    private final SessionFactory sessionFactory;

    public CityDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<City> getItems(int offset, int limit) {
        Query<City> query = getSessionFactory().getCurrentSession().createQuery("select c from City c", City.class);
        query.setFirstResult(offset);
        query.setMaxResults(limit);

        return query.list();
    }

    public int getTotalCount() {
        Query<Long> query = sessionFactory.getCurrentSession().createQuery("select count(*) from City c", Long.class);
        return Math.toIntExact(query.uniqueResult());
    }

    public City getById(Integer id) {
        Query<City> query = sessionFactory.getCurrentSession()
                .createQuery("select c from City c where c.id = :id", City.class);

        query.setParameter("id", id);

        return query.getSingleResult();
    }
}
