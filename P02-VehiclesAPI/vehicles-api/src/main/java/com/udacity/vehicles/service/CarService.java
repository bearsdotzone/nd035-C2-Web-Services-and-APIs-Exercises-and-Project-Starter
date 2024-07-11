package com.udacity.vehicles.service;

import com.udacity.vehicles.client.maps.Address;
import com.udacity.vehicles.client.prices.Price;
import com.udacity.vehicles.domain.Location;
import com.udacity.vehicles.domain.car.Car;
import com.udacity.vehicles.domain.car.CarRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Implements the car service create, read, update or delete
 * information about vehicles, as well as gather related
 * location and price data when desired.
 */
@Service
public class CarService {

    private final CarRepository repository;
    private final WebClient pricing;
    private final WebClient maps;

    private final DiscoveryClient discoveryClient;

    @Value("${maps.endpoint}")
    private String mapsEndpoint;

    public CarService(CarRepository repository, WebClient pricing, WebClient maps, DiscoveryClient discoveryClient) {
        this.repository = repository;
        this.maps = maps;
        this.pricing = pricing;
        this.discoveryClient = discoveryClient;
    }

    /**
     * Gathers a list of all vehicles
     *
     * @return a list of all vehicles in the CarRepository
     */
    public List<Car> list() {
        return repository.findAll();
    }

    /**
     * Gets car information by ID (or throws exception if non-existent)
     *
     * @param id the ID number of the car to gather information on
     * @return the requested car's information, including location and price
     */
    public Car findById(Long id) {

        Car car = repository.findById(id)
                            .orElseThrow(CarNotFoundException::new);

        assert car.getId() != null;


        ServiceInstance serviceInstance = discoveryClient.getInstances("pricing-service")
                                                         .get(0);

        Price priceServiceResponse = pricing.get()
                                            .uri(serviceInstance.getUri() + "/prices/" + id)
                                            .retrieve()
                                            .bodyToMono(Price.class)
                                            .onErrorReturn(new Price())
                                            .block();
        car.setPrice(priceServiceResponse.getPrice() + " " + priceServiceResponse.getCurrency());

        String mapsUri = mapsEndpoint + String.format("/maps?lat=%s&lon=%s", car.getLocation()
                                                                                .getLat(), car.getLocation()
                                                                                              .getLon());
        Address mapsServiceResponse = maps.get()
                                          .uri(mapsUri)
                                          .retrieve()
                                          .bodyToMono(Address.class)
                                          .onErrorReturn(new Address())
                                          .block();
        Location currentLocation = car.getLocation();
        currentLocation.setAddress(mapsServiceResponse.getAddress());
        currentLocation.setCity(mapsServiceResponse.getCity());
        currentLocation.setState(mapsServiceResponse.getState());
        currentLocation.setZip(mapsServiceResponse.getZip());
        car.setLocation(currentLocation);


        return car;
    }

    /**
     * Either creates or updates a vehicle, based on prior existence of car
     *
     * @param car A car object, which can be either new or existing
     * @return the new/updated car is stored in the repository
     */
    public Car save(Car car) {

        try {
            return repository.findById(car.getId())
                             .map(carToBeUpdated -> {
                                 carToBeUpdated.setDetails(car.getDetails());
                                 carToBeUpdated.setLocation(car.getLocation());
                                 return repository.save(carToBeUpdated);
                             })
                             .orElseThrow(CarNotFoundException::new);
        } catch (CarNotFoundException e) {
            return repository.save(car);
        }
    }

    /**
     * Deletes a given car by ID
     *
     * @param id the ID number of the car to delete
     */
    public void delete(Long id) {
        Car selectedCar = repository.findById(id)
                                    .orElseThrow(CarNotFoundException::new);
        repository.delete(selectedCar);
    }
}
