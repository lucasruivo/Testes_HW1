package com.example.zeromonos.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class MunicipioService {

    private static final String API_URL = "https://geoapi.pt/municipios";

    public List<String> getMunicipios() {
        RestTemplate restTemplate = new RestTemplate();

        String[] municipios = restTemplate.getForObject(API_URL, String[].class);

        return Arrays.asList(municipios);
    }

    public boolean isValidMunicipality(String municipio) {
        return getMunicipios().contains(municipio);
    }
}