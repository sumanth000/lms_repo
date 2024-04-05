package com.libraryzone.resource;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class apiFile {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/check")
    public String getString() {
        String responseString = "running properly";
        return responseString;
    }

}
