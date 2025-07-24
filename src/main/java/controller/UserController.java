package controller;

import Messages.ErrorResponse;
import Messages.SuccessResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.*;
import services.UserService;
import utils.SimpleJwtUtil;

import static spark.Spark.*;

public class UserController {

    private static final UserService userService = new UserService();
    public static final Gson gson = new Gson();

    public static void initRoutes() {

        before((req, res) -> {
            String method = req.requestMethod();
            String path = req.pathInfo();

            // Allow public access for register and login POST routes without token
            if (method.equalsIgnoreCase("POST") && (path.equals("/users/register") || path.equals("/users/login"))) {
                return; // Skip token check here
            }

            // Allow OPTIONS (for CORS preflight)
            if (method.equalsIgnoreCase("OPTIONS")) {
                return;
            }

            // For all other requests, check Authorization header
            String authHeader = req.headers("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                res.status(401);
                halt(401, gson.toJson(new ErrorResponse("Missing or invalid token")));
            }

            String token = authHeader.substring(7);
            if (!SimpleJwtUtil.validateToken(token)) {
                res.status(401);
                halt(401, gson.toJson(new ErrorResponse("Invalid or expired token")));
            }
        });


        path("/users", () -> {

            // Get all users (requires token)
            get("", (req, res) -> gson.toJson(userService.getAllUsers()));

            // Get user by ID
            get("/:id", (req, res) -> {
                int id = Integer.parseInt(req.params(":id"));
                User user = userService.getUser(id);
                if (user == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("User not found"));
                }
                return gson.toJson(user);
            });

            // Register: expects JSON with fullName, phone, password, address, role, photo optional
            post("/register", (req, res) -> {
                try {
                    // Parse JSON manually to allow missing optional fields easily
                    JsonObject jsonObj = JsonParser.parseString(req.body()).getAsJsonObject();

                    String fullName = jsonObj.has("fullName") ? jsonObj.get("fullName").getAsString() : null;
                    String phone = jsonObj.has("phone") ? jsonObj.get("phone").getAsString() : null;
                    String password = jsonObj.has("password") ? jsonObj.get("password").getAsString() : null;
                    String address = jsonObj.has("address") ? jsonObj.get("address").getAsString() : null;
                    String role = jsonObj.has("role") ? jsonObj.get("role").getAsString().toLowerCase() : null;
                    String photo = jsonObj.has("profileImageBase64") ? jsonObj.get("profileImageBase64").getAsString() : null;

                    // Validate required fields
                    if (fullName == null || phone == null || password == null || address == null || role == null) {
                        res.status(400);
                        return gson.toJson(new ErrorResponse("Missing required fields"));
                    }

                    // Create user based on role
                    User user;
                    switch (role) {
                        case "buyer":
                            user = new Buyer(0, fullName, phone, null, password, address, photo, null, "active", null);
                            break;
                        case "vendor":
                        case "seller": // if you want to support 'seller' as alias for vendor
                            user = new Vendor(0, fullName, phone, null, password, address, photo, null, "active", null);
                            break;
                        case "courier":
                            user = new Courier(0, fullName, phone, null, password, address, photo, null, "active", null);
                            break;
                        default:
                            res.status(400);
                            return gson.toJson(new ErrorResponse("Invalid role"));
                    }

                    user.validateRequiredFields();

                    User registered = userService.register(user);
                    if (registered == null) {
                        res.status(400);
                        return gson.toJson(new ErrorResponse("Phone already registered"));
                    }

                    res.status(201);
                    return gson.toJson(registered);

                } catch (Exception e) {
                    res.status(400);
                    return gson.toJson(new ErrorResponse("Invalid input: " + e.getMessage()));
                }
            });

            // Login route
            post("/login", (req, res) -> {
                LoginRequest loginReq = gson.fromJson(req.body(), LoginRequest.class);

                User loggedIn = userService.login(loginReq.getPhone(), loginReq.getPassword());
                if (loggedIn == null) {
                    res.status(401);
                    return gson.toJson(new ErrorResponse("Invalid phone or password"));
                }

                String token = SimpleJwtUtil.generateToken(loggedIn.getId(), loggedIn.getRole());

                LoginResponse response = new LoginResponse(loggedIn);
                response.token = token;
                return gson.toJson(response);
            });

            // Get profile by id
            get("/:id/profile", (req, res) -> {
                int id = Integer.parseInt(req.params(":id"));
                User user = userService.getUser(id);
                if (user == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("User not found"));
                }
                return gson.toJson(user);
            });

            // Update profile
            put("/:id/profile", (req, res) -> {
                int id = Integer.parseInt(req.params(":id"));
                User existing = userService.getUser(id);
                if (existing == null) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("User not found"));
                }

                // You can parse the update JSON similarly here or reuse createUserFromRole() if needed

                // For simplicity, just return 501 for now
                res.status(501);
                return gson.toJson(new ErrorResponse("Profile update not implemented yet"));
            });

            // Delete user
            delete("/:id", (req, res) -> {
                int id = Integer.parseInt(req.params(":id"));
                boolean deleted = userService.deleteUser(id);
                if (!deleted) {
                    res.status(404);
                    return gson.toJson(new ErrorResponse("User not found"));
                }
                return gson.toJson(new SuccessResponse("User deleted successfully"));
            });
        });
    }

    private static class LoginResponse {
        int id;
        String name;
        String role;
        String message;
        String token;

        public LoginResponse(User user) {
            this.id = user.getId();
            this.name = user.getFullName();
            this.role = user.getRole();
            this.message = "Login successful!";
        }
    }

    private static class LoginRequest {
        private String phone;
        private String password;

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
