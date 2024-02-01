/*
 *
 * * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */
package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import redis.clients.jedis.Jedis;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Provider;
import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Primary
@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {



  // @Autowired
  // private MongoTemplate mongoTemplate;

  // @Autowired
  // private Provider<ModelMapper> modelMapperProvider;

  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private RedisConfiguration redisConfiguration;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }


  // Without Caching
  // public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude,
  // LocalTime currentTime, Double servingRadiusInKms) {
  // // ModelMapper modelMapper = modelMapperProvider.get();
  // List<RestaurantEntity> restaurantEntityList = restaurantRepository.findAll();
  // List<Restaurant> restaurantList = new ArrayList<>();

  // for (RestaurantEntity restaurantEntity : restaurantEntityList) {
  // if (isOpenNow(currentTime, restaurantEntity)) {
  // if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude,
  // servingRadiusInKms)) {
  // restaurantList.add(modelMapperProvider.get().map(restaurantEntity, Restaurant.class));
  // }
  // }
  // }
  // return restaurantList;
  // }

  // with caching
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurants = null;
    if (redisConfiguration.isCacheAvailable()) {
      restaurants =
          findAllRestaurantsCloseByFromCache(latitude, longitude, currentTime, servingRadiusInKms);
    } else {
      restaurants =
          findAllRestaurantsCloseFromDb(latitude, longitude, currentTime, servingRadiusInKms);
    }
    return restaurants;
  }

  private List<Restaurant> findAllRestaurantsCloseFromDb(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {
    ModelMapper modelMapper = modelMapperProvider.get();
    List<RestaurantEntity> restaurantEntities = restaurantRepository.findAll();
    List<Restaurant> restaurants = new ArrayList<Restaurant>();
    for (RestaurantEntity restaurantEntity : restaurantEntities) {
      if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude,
          servingRadiusInKms)) {
        restaurants.add(modelMapper.map(restaurantEntity, Restaurant.class));
      }
    }
    return restaurants;
  }

  private List<Restaurant> findAllRestaurantsCloseByFromCache(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {
    GeoHash geoHash = GeoHash.withCharacterPrecision(latitude, longitude, 7);
    String key = geoHash.toBase32();
    Jedis jedis = redisConfiguration.getJedisPool().getResource();

    List<Restaurant> restaurants = new ArrayList<>();
    if (jedis.get(key) == null) {
      restaurants =
          findAllRestaurantsCloseFromDb(latitude, longitude, currentTime, servingRadiusInKms);
      try {
        jedis.set(key, new ObjectMapper().writeValueAsString(restaurants));
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }

    } else {
      try {
        restaurants =
            new ObjectMapper().readValue(jedis.get(key), new TypeReference<List<Restaurant>>() {});
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return restaurants;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose names have an exact or partial match with the search query.
  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    BasicQuery query = new BasicQuery("{name: {$regex: /" + searchString + "/i}}");
    List<RestaurantEntity> restaurants =
        mongoTemplate.find(query, RestaurantEntity.class, "restaurants");

    List<Restaurant> restaurantList = new ArrayList<Restaurant>();
    ModelMapper modelMapper = modelMapperProvider.get();
    for (RestaurantEntity restaurant : restaurants) {
      if (isRestaurantCloseByAndOpen(restaurant, currentTime, latitude, longitude,
          servingRadiusInKms)) {
        restaurantList.add(modelMapper.map(restaurant, Restaurant.class));
      }
    }
    return restaurantList;
  }

  @Override
  @Async
  public CompletableFuture<List<Restaurant>> findRestaurantsByNameAsync(Double latitude,
      Double longitude, String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    BasicQuery query = new BasicQuery("{name: {$regex: /" + searchString + "/i}}");
    List<RestaurantEntity> restaurants =
        mongoTemplate.find(query, RestaurantEntity.class, "restaurants");
    List<Restaurant> restaurantList = new ArrayList<Restaurant>();

    // List<Restaurant> restaurantList = new ArrayList<Restaurant>();
    ModelMapper modelMapper = modelMapperProvider.get();
    for (RestaurantEntity restaurant : restaurants) {
      if (isRestaurantCloseByAndOpen(restaurant, currentTime, latitude, longitude,
          servingRadiusInKms)) {
        restaurantList.add(modelMapper.map(restaurant, Restaurant.class));
      }
    }
    return CompletableFuture.completedFuture(restaurantList);
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    BasicQuery query = new BasicQuery("{attributes: {$regex: /" + searchString + "/i}}");
    List<RestaurantEntity> restaurants =
        mongoTemplate.find(query, RestaurantEntity.class, "restaurants");
    List<Restaurant> restaurantList = new ArrayList<Restaurant>();
    ModelMapper modelMapper = modelMapperProvider.get();
    for (RestaurantEntity restaurant : restaurants) {
      if (isRestaurantCloseByAndOpen(restaurant, currentTime, latitude, longitude,
          servingRadiusInKms)) {
        restaurantList.add(modelMapper.map(restaurant, Restaurant.class));
      }
    }
    return restaurantList;
  }



  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose names form a complete or partial match
  // with the search query.

  @Override
  public List<Restaurant> findRestaurantsByItemName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    BasicQuery query = new BasicQuery("{'items.name': {$regex: /" + searchString + "/i}}");
    List<MenuEntity> menus = mongoTemplate.find(query, MenuEntity.class, "menus");
    List<RestaurantEntity> restaurants = new ArrayList<>();
    for (MenuEntity menu : menus) {
      String restaurantId = menu.getRestaurantId();
      BasicQuery restaurantQuery = new BasicQuery("{restaurantId:" + restaurantId + "}");
      restaurants
          .add(mongoTemplate.findOne(restaurantQuery, RestaurantEntity.class, "restaurants"));
    }
    List<Restaurant> restaurantList = new ArrayList<Restaurant>();
    ModelMapper modelMapper = modelMapperProvider.get();
    for (RestaurantEntity restaurant : restaurants) {
      if (isRestaurantCloseByAndOpen(restaurant, currentTime, latitude, longitude,
          servingRadiusInKms)) {
        restaurantList.add(modelMapper.map(restaurant, Restaurant.class));
      }
    }
    return restaurantList;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    BasicQuery query = new BasicQuery("{'items.attributes': {$regex: /" + searchString + "/i}}");
    List<MenuEntity> menus = mongoTemplate.find(query, MenuEntity.class, "menus");
    List<RestaurantEntity> restaurants = new ArrayList<>();
    for (MenuEntity menu : menus) {
      String restaurantId = menu.getRestaurantId();
      BasicQuery restaurantQuery = new BasicQuery("{restaurantId:" + restaurantId + "}");
      restaurants
          .add(mongoTemplate.findOne(restaurantQuery, RestaurantEntity.class, "restaurants"));
    }
    List<Restaurant> restaurantList = new ArrayList<Restaurant>();
    ModelMapper modelMapper = modelMapperProvider.get();
    for (RestaurantEntity restaurant : restaurants) {
      if (isRestaurantCloseByAndOpen(restaurant, currentTime, latitude, longitude,
          servingRadiusInKms)) {
        restaurantList.add(modelMapper.map(restaurant, Restaurant.class));
      }
    }
    return restaurantList;
  }

  // @Override
  // public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
  // String searchString, LocalTime currentTime, Double servingRadiusInKms) {

  // }

  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return GeoUtils.findDistanceInKm(latitude, longitude, restaurantEntity.getLatitude(),
          restaurantEntity.getLongitude()) < servingRadiusInKms;
    }

    return false;
  }


  @Override
  @Async
  public CompletableFuture<List<Restaurant>> findRestaurantsByAttributesAsync(Double latitude,
      Double longitude, String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    BasicQuery query = new BasicQuery("{attributes: {$regex: /" + searchString + "/i}}");
    List<RestaurantEntity> restaurants =
        mongoTemplate.find(query, RestaurantEntity.class, "restaurants");
    List<Restaurant> restaurantList = new ArrayList<Restaurant>();

    // List<Restaurant> restaurantList = new ArrayList<Restaurant>();
    ModelMapper modelMapper = modelMapperProvider.get();
    for (RestaurantEntity restaurant : restaurants) {
      if (isRestaurantCloseByAndOpen(restaurant, currentTime, latitude, longitude,
          servingRadiusInKms)) {
        restaurantList.add(modelMapper.map(restaurant, Restaurant.class));
      }
    }
    return CompletableFuture.completedFuture(restaurantList);
  }


  @Override
  @Async
  public CompletableFuture<List<Restaurant>> findRestaurantsByItemNameAsync(Double latitude,
      Double longitude, String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    BasicQuery query = new BasicQuery("{'items.name': {$regex: /" + searchString + "/i}}");
    List<MenuEntity> menus = mongoTemplate.find(query, MenuEntity.class, "menus");
    List<RestaurantEntity> restaurants = new ArrayList<>();
    for (MenuEntity menu : menus) {
      String restaurantId = menu.getRestaurantId();
      BasicQuery restaurantQuery = new BasicQuery("{restaurantId:" + restaurantId + "}");
      restaurants
          .add(mongoTemplate.findOne(restaurantQuery, RestaurantEntity.class, "restaurants"));
    }
    ModelMapper modelMapper = modelMapperProvider.get();
    List<Restaurant> restaurantList = new ArrayList<Restaurant>();
    for (RestaurantEntity restaurant : restaurants) {
      if (isRestaurantCloseByAndOpen(restaurant, currentTime, latitude, longitude,
          servingRadiusInKms)) {
        restaurantList.add(modelMapper.map(restaurant, Restaurant.class));
      }
    }
    return CompletableFuture.completedFuture(restaurantList);
  }


  @Override
  @Async
  public CompletableFuture<List<Restaurant>> findRestaurantsByItemAttributesAsync(Double latitude,
      Double longitude, String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    BasicQuery query = new BasicQuery("{'items.attributes': {$regex: /" + searchString + "/i}}");
    List<MenuEntity> menus = mongoTemplate.find(query, MenuEntity.class, "menus");
    List<RestaurantEntity> restaurants = new ArrayList<>();
    for (MenuEntity menu : menus) {
      String restaurantId = menu.getRestaurantId();
      BasicQuery restaurantQuery = new BasicQuery("{restaurantId:" + restaurantId + "}");
      restaurants
          .add(mongoTemplate.findOne(restaurantQuery, RestaurantEntity.class, "restaurants"));
    }
    ModelMapper modelMapper = modelMapperProvider.get();
    List<Restaurant> restaurantList = new ArrayList<Restaurant>();
    for (RestaurantEntity restaurant : restaurants) {
      if (isRestaurantCloseByAndOpen(restaurant, currentTime, latitude, longitude,
          servingRadiusInKms)) {
        restaurantList.add(modelMapper.map(restaurant, Restaurant.class));
      }
    }
    return CompletableFuture.completedFuture(restaurantList);
  }
}
