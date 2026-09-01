package io.routex.admin;

import io.routex.model.Route;
import io.routex.camel.DynamicRouteLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/routes")
public class RouteAdminController {

    @Autowired
    private DynamicRouteLoader loader;

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Route cfg) {
        try {
            loader.addRoute(cfg);
            return ResponseEntity.ok().body("added");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            loader.removeRoute(id);
            return ResponseEntity.ok().body("removed");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
