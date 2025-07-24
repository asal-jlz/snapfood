package main;

import static controller.UserController.gson;
import com.google.gson.Gson;
import static spark.Spark.*;

import Messages.ErrorResponse;
import controller.AdminController;
import controller.UserController;
import controller.RestaurantController;
import controller.FoodController;

public class simpleSparkServer {
    private static final Gson gson = new Gson();
    public static void main(String[] args) {
        exception(Exception.class, (e, req, res) -> {
            e.printStackTrace(); // Full stack trace
            res.type("application/json");
            res.status(500);
            res.body(gson.toJson(new ErrorResponse("Server error: " + e.getMessage())));
        });

        port(8080);
        UserController.initRoutes();
        RestaurantController.initRoutes();
        FoodController.initRoutes();
        AdminController.initRoutes();
        get("/", (req, res) -> "Food Service API is running!");
        System.out.println("==> Spark has started on http://localhost:8080");
    }
}