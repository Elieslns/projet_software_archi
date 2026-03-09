package com.projet.archi.servicea.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceAController {

    @GetMapping("/hello")
    public String hello() {
        return "hello A";
    }
}
