
/*
 *
 * <<<<<<< HEAD * Copyright (c) Crio.Do 2019. All rights reserved ======= * Copyright (c) Crio.Do
 * 2019. All rights reserved >>>>>>> dc8b0d3b93ef671e8dec85fa0588a5d535f28259
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

    private final Double peakHoursServingRadiusInKms = 3.0;
    private final Double normalHoursServingRadiusInKms = 5.0;
    @Autowired
    private RestaurantRepositoryService restaurantRepositoryService;


    // TODO: CRIO_TASK_MODULE_RESTAURANTSAPI - Implement findAllRestaurantsCloseby.
    // Check RestaurantService.java file for the interface contract.
    @Override
    public GetRestaurantsResponse findAllRestaurantsCloseBy(
            GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
        List<Restaurant> restaurant;
        if (getRestaurantsRequest.getSearchFor() != null
                && !getRestaurantsRequest.getSearchFor().equals("")) {
            return findRestaurantsBySearchQuery(getRestaurantsRequest, currentTime);
        }
        int h = currentTime.getHour();
        int m = currentTime.getMinute();
        if ((h >= 8 && h <= 9) || (h == 10 && m == 0) || (h == 13) || (h == 14 && m == 0)
                || (h >= 19 && h <= 20) || (h == 21 && m == 0)) {
            restaurant = restaurantRepositoryService.findAllRestaurantsCloseBy(
                    getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(),
                    currentTime, peakHoursServingRadiusInKms);
        } else {
            restaurant = restaurantRepositoryService.findAllRestaurantsCloseBy(
                    getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(),
                    currentTime, normalHoursServingRadiusInKms);
        }
        GetRestaurantsResponse response = new GetRestaurantsResponse(restaurant);
        log.info(response);
        return response;
    }
    // @Override
    // public GetRestaurantsResponse findAllRestaurantsCloseBy(
    // GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {


    // }


    // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
    // Implement findRestaurantsBySearchQuery. The request object has the search string.
    // We have to combine results from multiple sources:
    // 1. Restaurants by name (exact and inexact)
    // 2. Restaurants by cuisines (also called attributes)
    // 3. Restaurants by food items it serves
    // 4. Restaurants by food item attributes (spicy, sweet, etc)
    // Remember, a restaurant must be present only once in the resulting list.
    // Check RestaurantService.java file for the interface contract.
    @Override
    public GetRestaurantsResponse findRestaurantsBySearchQuery(
            GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

        Double latitude = getRestaurantsRequest.getLatitude();
        Double longitude = getRestaurantsRequest.getLongitude();
        String searchFor = getRestaurantsRequest.getSearchFor();
        int h = currentTime.getHour();
        int m = currentTime.getMinute();
        if (searchFor.equals("")) {
            List<Restaurant> restaurantList = new ArrayList<Restaurant>();
            // response.setRestaurants(restaurantList);
            return new GetRestaurantsResponse(restaurantList);
        } else if ((h >= 8 && h <= 9) || (h == 10 && m == 0) || (h == 13) || (h == 14 && m == 0)
                || (h >= 19 && h <= 20) || (h == 21 && m == 0)) {
            List<Restaurant> restaurantList = restaurantRepositoryService.findRestaurantsByName(
                    latitude, longitude, searchFor, currentTime, peakHoursServingRadiusInKms);
            restaurantList.addAll(restaurantRepositoryService.findRestaurantsByAttributes(latitude,
                    longitude, searchFor, currentTime, peakHoursServingRadiusInKms));
            restaurantList.addAll(restaurantRepositoryService.findRestaurantsByItemName(latitude,
                    longitude, searchFor, currentTime, peakHoursServingRadiusInKms));
            restaurantList.addAll(restaurantRepositoryService.findRestaurantsByItemAttributes(
                    latitude, longitude, searchFor, currentTime, peakHoursServingRadiusInKms));

            List<Restaurant> responseList =
                    restaurantList.stream().distinct().collect(Collectors.toList());

            return new GetRestaurantsResponse(responseList);
        } else {
            List<Restaurant> restaurantList = restaurantRepositoryService.findRestaurantsByName(
                    latitude, longitude, searchFor, currentTime, normalHoursServingRadiusInKms);
            restaurantList.addAll(restaurantRepositoryService.findRestaurantsByAttributes(latitude,
                    longitude, searchFor, currentTime, normalHoursServingRadiusInKms));
            restaurantList.addAll(restaurantRepositoryService.findRestaurantsByItemName(latitude,
                    longitude, searchFor, currentTime, normalHoursServingRadiusInKms));
            restaurantList.addAll(restaurantRepositoryService.findRestaurantsByItemAttributes(
                    latitude, longitude, searchFor, currentTime, normalHoursServingRadiusInKms));
            List<Restaurant> responseList =
                    restaurantList.stream().distinct().collect(Collectors.toList());
            return new GetRestaurantsResponse(responseList);
        }
    }

    // @Override
    // public GetRestaurantsResponse findAllRestaurantsCloseBy(
    // GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {


    // }


    // @Override
    // public GetRestaurantsResponse findRestaurantsBySearchQuery(
    // GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

    // }

    // TODO: CRIO_TASK_MODULE_MULTITHREADING
    // Implement multi-threaded version of RestaurantSearch.
    // Implement variant of findRestaurantsBySearchQuery which is at least 1.5x time faster than
    // findRestaurantsBySearchQuery.
    @Override
    public GetRestaurantsResponse findRestaurantsBySearchQueryMt(
            GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime)
            throws InterruptedException, ExecutionException {
        // int timing = currentTime.getHour() * 100 + currentTime.getMinute();

        Double latitude = getRestaurantsRequest.getLatitude();
        Double longitude = getRestaurantsRequest.getLongitude();
        String searchFor = getRestaurantsRequest.getSearchFor();
        int h = currentTime.getHour();
        int m = currentTime.getMinute();
        if (searchFor.equals("")) {
            List<Restaurant> restaurantList = new ArrayList<Restaurant>();
            // response.setRestaurants(restaurantList);
            return new GetRestaurantsResponse(restaurantList);
        } else if ((h >= 8 && h <= 9) || (h == 10 && m == 0) || (h == 13) || (h == 14 && m == 0)
                || (h >= 19 && h <= 20) || (h == 21 && m == 0)) {
            CompletableFuture<List<Restaurant>> restaurantList1 =
                    restaurantRepositoryService.findRestaurantsByNameAsync(latitude, longitude,
                            searchFor, currentTime, peakHoursServingRadiusInKms);
            CompletableFuture<List<Restaurant>> restaurantList2 =
                    restaurantRepositoryService.findRestaurantsByAttributesAsync(latitude,
                            longitude, searchFor, currentTime, peakHoursServingRadiusInKms);
            CompletableFuture<List<Restaurant>> restaurantList3 =
                    restaurantRepositoryService.findRestaurantsByItemNameAsync(latitude, longitude,
                            searchFor, currentTime, peakHoursServingRadiusInKms);
            CompletableFuture<List<Restaurant>> restaurantList4 =
                    restaurantRepositoryService.findRestaurantsByItemAttributesAsync(latitude,
                            longitude, searchFor, currentTime, peakHoursServingRadiusInKms);

            CompletableFuture
                    .allOf(restaurantList1, restaurantList2, restaurantList3, restaurantList4)
                    .join();

            List<Restaurant> restaurantList = restaurantList1.get();
            restaurantList.addAll(restaurantList2.get());
            restaurantList.addAll(restaurantList3.get());
            restaurantList.addAll(restaurantList4.get());
            List<Restaurant> responseList =
                    restaurantList.stream().distinct().collect(Collectors.toList());

            return new GetRestaurantsResponse(responseList);
        } else {
            CompletableFuture<List<Restaurant>> restaurantList1 =
                    restaurantRepositoryService.findRestaurantsByNameAsync(latitude, longitude,
                            searchFor, currentTime, normalHoursServingRadiusInKms);
            CompletableFuture<List<Restaurant>> restaurantList2 =
                    restaurantRepositoryService.findRestaurantsByAttributesAsync(latitude,
                            longitude, searchFor, currentTime, normalHoursServingRadiusInKms);
            CompletableFuture<List<Restaurant>> restaurantList3 =
                    restaurantRepositoryService.findRestaurantsByItemNameAsync(latitude, longitude,
                            searchFor, currentTime, normalHoursServingRadiusInKms);
            CompletableFuture<List<Restaurant>> restaurantList4 =
                    restaurantRepositoryService.findRestaurantsByItemAttributesAsync(latitude,
                            longitude, searchFor, currentTime, normalHoursServingRadiusInKms);

            CompletableFuture
                    .allOf(restaurantList1, restaurantList2, restaurantList3, restaurantList4)
                    .join();

            List<Restaurant> restaurantList = restaurantList1.get();
            restaurantList.addAll(restaurantList2.get());
            restaurantList.addAll(restaurantList3.get());
            restaurantList.addAll(restaurantList4.get());
            List<Restaurant> responseList =
                    restaurantList.stream().distinct().collect(Collectors.toList());
            return new GetRestaurantsResponse(responseList);
        }
    }
}
