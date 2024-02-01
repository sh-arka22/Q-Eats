package com.crio.qeats.controller;

import lombok.extern.log4j.Log4j2;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.validation.Valid;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.services.RestaurantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: CRIO_TASK_MODULE_RESTAURANTSAPI
// Implement Controller using Spring annotations.
// Remember, annotations have various "targets". They can be class level, method level or others.
@RestController // I added
@RequestMapping(RestaurantController.RESTAURANT_API_ENDPOINT) // i added
@Log4j2
public class RestaurantController {

  public static final String RESTAURANT_API_ENDPOINT = "/qeats/v1";
  public static final String RESTAURANTS_API = "/restaurants";
  public static final String MENU_API = "/menu";
  public static final String CART_API = "/cart";
  public static final String CART_ITEM_API = "/cart/item";
  public static final String CART_CLEAR_API = "/cart/clear";
  public static final String POST_ORDER_API = "/order";
  public static final String GET_ORDERS_API = "/orders";

  @Autowired
  private RestaurantService restaurantService;

  // @GetMapping(RESTAURANTS_API)
  // public ResponseEntity<GetRestaurantsResponse> getRestaurants(
  //     GetRestaurantsRequest getRestaurantsRequest) {

  //   // log.info("getRestaurants called with {}", getRestaurantsRequest);
  //   GetRestaurantsResponse getRestaurantsResponse = null;

  //   // CHECKSTYLE:OFF
  //   // (getRestaurantsRequest.getLatitude() != null && getRestaurantsRequest.getLongitude() != null
  //   if(getRestaurantsRequest.getLatitude() == null || getRestaurantsRequest.getLongitude() == null)
  //       return ResponseEntity.ok().body(getRestaurantsResponse);
  //   if (getRestaurantsRequest.getLatitude() >= -90 && getRestaurantsRequest.getLongitude() >= -180
  //       && getRestaurantsRequest.getLatitude() <= 90
  //       && getRestaurantsRequest.getLongitude() <= 180) {
  //         String searchFor = getRestaurantsRequest.getSearchFor();
  //         if(searchFor == null || searchFor.equals(""))
  //             getRestaurantsResponse = restaurantService.findAllRestaurantsCloseBy(getRestaurantsRequest, LocalTime.now());
  //         else getRestaurantsResponse = restaurantService.findRestaurantsBySearchQuery(getRestaurantsRequest, LocalTime.now());
  //     // checking for special character
  //     if (getRestaurantsResponse != null && !getRestaurantsResponse.getRestaurants().isEmpty()) {
  //       List<Restaurant> restaurants = getRestaurantsResponse.getRestaurants();
  //       for (int i = 0; i < restaurants.size(); i++) {
  //         restaurants.get(i).setName(restaurants.get(i).getName().replace("é", "?"));
  //       }
  //       getRestaurantsResponse.setRestaurants(restaurants);
  //     }
  //     // log.info("getRestaurants returned {}", getRestaurantsResponse);
  //     return ResponseEntity.ok().body(getRestaurantsResponse);
  //   } 
  //   else {
  //     return ResponseEntity.badRequest().body(null);
  //   }
  // }
  // @GetMapping(RESTAURANTS_API)
  // public ResponseEntity<GetRestaurantsResponse> getRestaurants(@Valid GetRestaurantsRequest getRestaurantsRequest) {
  //   log.info("getRestaurants called with {}", getRestaurantsRequest);

  //   GetRestaurantsResponse getRestaurantsResponse = null;

  //   Double latitude = getRestaurantsRequest.getLatitude();
  //   Double longitude = getRestaurantsRequest.getLongitude();
  //   String searchFor = getRestaurantsRequest.getSearchFor();
  //   // if(latitude == null || longitude == null)
  //   //   return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(getRestaurantsResponse);
  //   if (latitude == null || longitude == null || latitude < 0 || latitude > 90 || longitude < 0 || longitude > 180) {
  //     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(getRestaurantsResponse);
  //   }
  //   if (searchFor != null && !searchFor.equals("")) {
  //     getRestaurantsResponse = restaurantService.
  //     findRestaurantsBySearchQuery(getRestaurantsRequest, LocalTime.now());
  //       for (int i = 0; i < getRestaurantsResponse.getRestaurants().size(); i++) {
  //         getRestaurantsResponse.getRestaurants().get(i).setName(getRestaurantsResponse.getRestaurants().get(i).getName().replace("é", "?"));
  //       }
  //     return ResponseEntity.ok().body(getRestaurantsResponse);
  //   }
  //   getRestaurantsResponse = restaurantService.findAllRestaurantsCloseBy(getRestaurantsRequest, LocalTime.now());
  //   for (int i = 0; i < getRestaurantsResponse.getRestaurants().size(); i++) {
  //     getRestaurantsResponse.getRestaurants().get(i).setName(getRestaurantsResponse.getRestaurants().get(i).getName().replace("é", "?"));
  //   }
  //   return ResponseEntity.ok().body(getRestaurantsResponse);
  // }
  @GetMapping(RESTAURANTS_API)
  public ResponseEntity<GetRestaurantsResponse> getRestaurants(@Valid GetRestaurantsRequest getRestaurantsRequest) throws InterruptedException, ExecutionException {
    log.info("getRestaurants called with {}", getRestaurantsRequest);
    if (!getRestaurantsRequest.isValid()) {
      return ResponseEntity.badRequest().build();
    }
    

    GetRestaurantsResponse getRestaurantsResponse = null;
    final LocalTime now = LocalTime.now();
    if (getRestaurantsRequest.getSearchFor() != null && !getRestaurantsRequest.equals("")) {
      getRestaurantsResponse =
          restaurantService.findRestaurantsBySearchQuery(getRestaurantsRequest, now);
          // restaurantService.findRestaurantsBySearchQueryMt(getRestaurantsRequest, now);
    } 
    else {
      getRestaurantsResponse = restaurantService
          .findAllRestaurantsCloseBy(getRestaurantsRequest, now);
    }
    log.info("getRestaurants returned {}", getRestaurantsResponse);
    
    if(getRestaurantsResponse == null || getRestaurantsResponse.getRestaurants().size() == 0)
      return ResponseEntity.ok().body(getRestaurantsResponse);

    if(getRestaurantsResponse != null){
      for (int i = 0; i < getRestaurantsResponse.getRestaurants().size(); i++) {
        getRestaurantsResponse.getRestaurants().get(i).setName(getRestaurantsResponse.getRestaurants().get(i).getName().replace("é", "?"));
      }
    }
    //  ResponseEntity.ok().body(getRestaurantsResponse);
    
    return ResponseEntity.ok().body(getRestaurantsResponse);
  }
  // @GetMapping(RESTAURANTS_API)
  // public ResponseEntity<GetRestaurantsResponse> getRestaurants(
  // GetRestaurantsRequest getRestaurantsRequest) {

  // // log.info("getRestaurants called with {}", getRestaurantsRequest);
  // GetRestaurantsResponse getRestaurantsResponse;

  // // CHECKSTYLE:OFF
  // // (getRestaurantsRequest.getLatitude() != null && getRestaurantsRequest.getLongitude() != null
  // Double latitude = getRestaurantsRequest.getLatitude();
  // Double longitude = getRestaurantsRequest.getLongitude();
  // String searchFor = getRestaurantsRequest.getSearchFor();
  // List<Restaurant> restaurantList = new ArrayList<>();
  // getRestaurantsResponse =
  // restaurantService.findAllRestaurantsCloseBy(getRestaurantsRequest, LocalTime.now());
  // log.info("getRestaurants returned {}", getRestaurantsResponse);
  // if (latitude == null || longitude == null || latitude < 0 || latitude > 90 || longitude < 0
  // || longitude > 180) {
  // return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(getRestaurantsResponse);
  // }
  // else if (searchFor != null && !searchFor.equals("")) {
  // getRestaurantsResponse =
  // restaurantService.findRestaurantsBySearchQuery(getRestaurantsRequest, LocalTime.now());
  // }
  // else {
  // getRestaurantsResponse =
  // restaurantService.findAllRestaurantsCloseBy(getRestaurantsRequest, LocalTime.now());
  // }
  // List<Restaurant> restaurants = getRestaurantsResponse.getRestaurants();

  // for(Restaurant restaurant: restaurants){
  // restaurant.setName(restaurant.getName().replace("é","?"));
  // }

  // getRestaurantsResponse.setRestaurants(restaurants);
  // return ResponseEntity.ok().body(getRestaurantsResponse);
  // }

  // TIP(MODULE_MENUAPI): Model Implementation for getting menu given a restaurantId.
  // Get the Menu for the given restaurantId
  // API URI: /qeats/v1/menu?restaurantId=11
  // Method: GET
  // Query Params: restaurantId
  // Success Output:
  // 1). If restaurantId is present return Menu
  // 2). Otherwise respond with BadHttpRequest.
  //
  // HTTP Code: 200
  // {
  //  "menu": {
  //    "items": [
  //      {
  //        "attributes": [
  //          "South Indian"
  //        ],
  //        "id": "1",
  //        "imageUrl": "www.google.com",
  //        "itemId": "10",
  //        "name": "Idly",
  //        "price": 45
  //      }
  //    ],
  //    "restaurantId": "11"
  //  }
  // }
  // Error Response:
  // HTTP Code: 4xx, if client side error.
  //          : 5xx, if server side error.
  // Eg:
  // curl -X GET "http://localhost:8081/qeats/v1/menu?restaurantId=11"












}

