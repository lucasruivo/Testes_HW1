package com.example.zeromonos.boundary;

import com.example.zeromonos.service.MunicipioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/municipios")
public class MunicipioController {

    private final MunicipioService municipioService;

    public MunicipioController(MunicipioService municipioService) {
        this.municipioService = municipioService;
    }

    @GetMapping
    public List<String> getMunicipios() {
        return municipioService.getMunicipios();
    }
}