package com.example.car.services;

import com.example.car.entities.Car;
import com.example.car.models.CarResponse;
import com.example.car.models.Client;
import com.example.car.repositories.CarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class CarService {
    private static final Logger log = LoggerFactory.getLogger(CarService.class);

    private final CarRepository carRepository;
    private final RestTemplate restTemplate;
    private final String clientServiceUrl;

    public CarService(
            CarRepository carRepository,
            RestTemplate restTemplate,
            @Value("${client.service.url}") String clientServiceUrl) {
        this.carRepository = carRepository;
        this.restTemplate = restTemplate;
        this.clientServiceUrl = clientServiceUrl;
    }

    public List<CarResponse> findAll() {
        return carRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CarResponse findById(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new CarNotFoundException(id));
        return toResponse(car);
    }

    private CarResponse toResponse(Car car) {
        Client client = null;
        if (car.getClientId() != null) {
            try {
                client = restTemplate.getForObject(
                        clientServiceUrl + "/{id}", Client.class, car.getClientId());
            } catch (RestClientException ex) {
                log.warn("Could not retrieve client {} through the gateway: {}",
                        car.getClientId(), ex.getMessage());
            }
        }
        return CarResponse.builder()
                .id(car.getId())
                .brand(car.getBrand())
                .model(car.getModel())
                .matricule(car.getMatricule())
                .client(client)
                .build();
    }

    public static class CarNotFoundException extends RuntimeException {
        public CarNotFoundException(Long id) {
            super("Voiture non trouvée avec l'ID: " + id);
        }
    }
}
