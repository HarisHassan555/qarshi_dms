package com.bezkoder.spring.login.sa.bll.dto;

import java.io.Serializable;
import java.util.List;

public class CountryDTO implements Serializable {
    private Long id;
    private String name;
    private List<CityDTO> cities;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CityDTO> getCities() {
        return cities;
    }

    public void setCities(List<CityDTO> cities) {
        this.cities = cities;
    }

    // Getters and Setters
}