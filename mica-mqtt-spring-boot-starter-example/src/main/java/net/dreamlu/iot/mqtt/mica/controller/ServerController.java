package net.dreamlu.iot.mqtt.mica.controller;

import net.dreamlu.iot.mqtt.mica.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/micamqtt")
@RestController
public class ServerController {
    @Autowired
    private ServerService service;

    @PostMapping("/server/publish")
    public boolean publish(@RequestBody String body) {
        return service.publish(body);
    }
}
