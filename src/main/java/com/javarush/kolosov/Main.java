package com.javarush.kolosov;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.kolosov.domain.*;
import com.javarush.kolosov.redis.CityCountry;
import com.javarush.kolosov.redis.Language;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStringCommands;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {
    private final SessionFactory sessionFactory;
    private final RedisClient redisClient;

    private final ObjectMapper mapper;

    private final CityDAO cityDAO;
    private final CountryDAO countryDAO;

    public Main() {
        sessionFactory = prepareRelationalDb();
        cityDAO = new CityDAO(sessionFactory);
        countryDAO = new CountryDAO(sessionFactory);

        redisClient = prepareRedisClient();
        mapper = new ObjectMapper();
    }

    public static void main(String[] args) {
        Main main = new Main();
        List<City> cities = main.fetchData(main);
        List<CityCountry> preparedData = main.transformData(cities);
        main.pushToRedis(preparedData);

        main.sessionFactory.getCurrentSession().close();

        List<Integer> ids = List.of(3, 2545, 123, 4, 189, 89, 3458, 1189, 10, 102);

        long startRedis = System.currentTimeMillis();
        main.testRedisData(ids);
        long stopRedis = System.currentTimeMillis();

        long startMySql = System.currentTimeMillis();
        main.testMySqlData(ids);
        long stopMySql = System.currentTimeMillis();

        System.out.printf("%s:\t%d ms\n", "Redis", (stopRedis - startRedis));
        System.out.printf("%s:\t%d ms\n", "MySql", (stopMySql - startMySql));

        main.shutdown();
    }

    private SessionFactory prepareRelationalDb() {
        Properties properties = new Properties();
        properties.put(Environment.DIALECT, "org.hibernate.dialect.MySQL8Dialect");
        properties.put(Environment.DRIVER, "com.p6spy.engine.spy.P6SpyDriver");
        properties.put(Environment.URL, "jdbc:p6spy:mysql://localhost:3306/world");
        properties.put(Environment.USER, "root");
        properties.put(Environment.PASS, "root");
        properties.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        properties.put(Environment.HBM2DDL_AUTO, "validate");
        properties.put(Environment.STATEMENT_BATCH_SIZE, "100");

        return new Configuration()
                .addProperties(properties)
                .addAnnotatedClass(Country.class)
                .addAnnotatedClass(City.class)
                .addAnnotatedClass(CountryLanguage.class)
                .buildSessionFactory();
    }

    private RedisClient prepareRedisClient() {
        RedisClient redisClient = RedisClient.create(RedisURI.create("localhost", 6379));
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            System.out.println("\nConnected to Redis\n");
        }

        return redisClient;
    }

    private void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }

        if (redisClient != null) {
            redisClient.shutdown();
        }
    }

    private List<City> fetchData(Main main) {
        try (Session session = main.sessionFactory.getCurrentSession()) {
            List<City> allCities = new ArrayList<>();
            session.beginTransaction();
            List<Country> countries = main.countryDAO.getAll();

            int totalCount = main.cityDAO.getTotalCount();
            int step = 500;
            for (int i = 0; i < totalCount; i += step) {
                allCities.addAll(main.cityDAO.getItems(i, step));
            }

            session.getTransaction().commit();
            return allCities;
        }
    }

    private List<CityCountry> transformData(List<City> cities) {
        List<CityCountry> cityCountries = new ArrayList<>();

        for (City city : cities) {
            CityCountry cityCountry = new CityCountry();
            cityCountry.setId(city.getId());
            cityCountry.setName(city.getName());
            cityCountry.setDistrict(city.getDistrict());
            cityCountry.setPopulation(city.getPopulation());

            Country country = city.getCountry();
            cityCountry.setCountryCode(country.getCode());
            cityCountry.setAlternativeCountryCode(country.getAlternativeCode());
            cityCountry.setCountryName(country.getName());
            cityCountry.setContinent(country.getContinent());
            cityCountry.setContinent(country.getContinent());
            cityCountry.setCountrySurfaceArea(country.getSurfaceArea());
            cityCountry.setCountryPopulation(country.getPopulation());

            Set<CountryLanguage> countryLanguages = country.getLanguages();
            Set<Language> languages = countryLanguages.stream().map(item -> {
                Language language = new Language();
                language.setLanguage(item.getLanguage());
                language.setIsOfficial(item.getIsOfficial());
                language.setPercentage(item.getPercentage());
                return language;
            }).collect(Collectors.toSet());

            cityCountry.setLanguages(languages);

            cityCountries.add(cityCountry);
        }

        return cityCountries;
    }

    private void pushToRedis(List<CityCountry> data) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (CityCountry cityCountry : data) {
                try {
                    String value = mapper.writeValueAsString(cityCountry);
                    sync.set(String.valueOf(cityCountry.getId()), value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void testRedisData(List<Integer> ids) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (Integer id : ids) {
                try {
                    mapper.readValue(sync.get(String.valueOf(id)), CityCountry.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void testMySqlData(List<Integer> ids) {
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();

            for(Integer id : ids) {
                City city = cityDAO.getById(id);
                Set<CountryLanguage> languages = city.getCountry().getLanguages();
            }

            session.getTransaction().commit();
        }
    }
}