package com.example.CycleSharingSystemBackend.service.impl;

import com.example.CycleSharingSystemBackend.dto.*;
import com.example.CycleSharingSystemBackend.dto.StationRequestDto;
import com.example.CycleSharingSystemBackend.model.Bikes;
import com.example.CycleSharingSystemBackend.model.Location;
import com.example.CycleSharingSystemBackend.model.Station;
import com.example.CycleSharingSystemBackend.repository.Stationrepository;
import com.example.CycleSharingSystemBackend.service.LocationService;
import com.example.CycleSharingSystemBackend.service.StationService;
import com.google.maps.errors.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class StationServicelmpl implements StationService {
    @Value("${google.maps.api.key}")
    private String apiKey;
    @Autowired
    private Stationrepository stationRepository;
    @Autowired
    private LocationService locationService;
    private final AtomicLong currentId = new AtomicLong(0);
    private String generateStationId() {
        return "ST" + currentId.incrementAndGet();
    }

    @Override
    public StationDto addStation(StationRequestDto stationRequestDto) {
        // Create a new Location object based on the location info from Dto
        LocationRequestDto locationRequestDto= stationRequestDto.getLocation();
        LocationDto savedLocationDto = locationService.addLocation(locationRequestDto);

        // Create a new Station object and set its properties from the Dto
        Station station = new Station();
        station.setName(stationRequestDto.getName());
        station.setLocation(convertToEntity(savedLocationDto));
        station.setCapacity(stationRequestDto.getCapacity());
        station.setStationId(generateStationId());

        // Save the station to the repository
        Station savedStation = stationRepository.save(station);

        // Convert the saved Station object to StationDto
        return convertToDto(savedStation);
    }
    private Location convertToEntity(LocationDto locationDto) {
        Location location = new Location();
        location.setLocationId(locationDto.getLocationId());
        location.setLatitude(locationDto.getLatitude());
        location.setLongitude(locationDto.getLongitude());
        return location;
    }

    private StationDto convertToDto(Station station) {
        Location location = station.getLocation();
        LocationDto locationDto = new LocationDto(location.getLocationId(), location.getLatitude(), location.getLongitude());

        return StationDto.builder()
                .stationId(station.getStationId())
                .name(station.getName())
                .location(locationDto)
                .capacity(station.getCapacity())
                .availableBike(station.getAvailableBike())
                .availableParkingSlots(station.getAvailableParkingSlots())
                .build();
    }

    @Override
    public List<StationDto> getAllStations() {
        List<Station> stations = stationRepository.findAll();
        List<StationDto> stationDtos = new ArrayList<>();

        for (Station station : stations) {
            stationDtos.add(convertToDto(station));
        }

        return stationDtos;
    }


    @Override
    public List<StationDistanceDTO> findNearestStations(double latitude, double longitude, int radius) throws IOException, InterruptedException, ApiException {

        // Radius of the Earth in kilometers
        final double earthRadius = 6371;

        // Convert latitude and longitude from degrees to radians
        double lat1 = Math.toRadians(latitude);
        double lon1 = Math.toRadians(longitude);

        List<StationDto> allStations = getAllStations();
        Map<StationDto, Double> stationDistances = new HashMap<>();

        for (StationDto station : allStations) {
            double lat2 = Math.toRadians(station.getLocation().getLatitude());
            double lon2 = Math.toRadians(station.getLocation().getLongitude());

            // Haversine formula
            double dlon = lon2 - lon1;
            double dlat = lat2 - lat1;
            double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = earthRadius * c;

            // Check if the station is within the given radius
            if (distance <= radius) {
                stationDistances.put(station, distance);
            }
        }

        // Sort stations by distance
        List<Map.Entry<StationDto, Double>> sortedStations = new ArrayList<>(stationDistances.entrySet());
        sortedStations.sort(Map.Entry.comparingByValue());

        List<StationDistanceDTO> nearestStations = new ArrayList<>();
        for (Map.Entry<StationDto, Double> entry : sortedStations) {
            StationDto station = entry.getKey();
            Double distanceKilometers = entry.getValue();
            double distanceMeters = Math.round(distanceKilometers * 1000 * 100.0) / 100.0;
            nearestStations.add(new StationDistanceDTO(station, distanceMeters));
        }

        return nearestStations;
    }

    @Override
    public Optional<StationDto> getStationById(String stationId) {
        Optional<Station> stationOptional = stationRepository.findByStationId(stationId);
        return stationOptional.map(this::convertToDto);
    }

    @Override
    public Station saveStation(Station station) {
        return stationRepository.save(station);
    }

    @Override
    public List<Bikes> getStationBikes(String stationId) {
        Optional<Station> station = stationRepository.findByStationId(stationId);
        return station.map(Station::getBikes).orElse(Collections.emptyList());
    }

    @Override
    public int getAvailableBikeCount(String stationId) {
        Optional<Station> stationOptional = stationRepository.findByStationId(stationId);
        return stationOptional.map(Station::getAvailableBike).orElse(0);
    }

    @Override
    public int getAvailableParkingSlots(String stationId) {
        Optional<Station> stationOptional = stationRepository.findByStationId(stationId);
        return stationOptional.map(station -> station.getCapacity() - station.getAvailableBike()).orElse(0);
    }

    @Override
    public void addBikeToStation(Bikes bike, Station station) {
        if (!station.getBikes().contains(bike)) { // Check if the bike is already in the station's list of bikes
            station.getBikes().add(bike);
            bike.setStation(station);
            saveStation(station);
        }
    }
    @Override
    public void removeBikeFromStation(Bikes bike, Station station) {
        if (station.getBikes().contains(bike)) { // Check if the bike is in the station's list of bikes
            station.getBikes().remove(bike);
            bike.setStation(null);
            saveStation(station);
        }
    }
    @Override
    public int getTotalAvailableBikes() {
        List<Station> allStations = stationRepository.findAll();
        return allStations.stream()
                .mapToInt(Station::getAvailableBike)
                .sum();
    }
}