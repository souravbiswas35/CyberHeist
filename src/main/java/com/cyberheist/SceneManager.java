package com.cyberheist;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class SceneManager {
    private static SceneManager instance;
    private Stage primaryStage;
    private String currentUsername;
    private Map<String, Scene> sceneCache = new HashMap<>();
    private Map<String, Object> controllerCache = new HashMap<>();

    private SceneManager() {}

    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void switchToScene(String fxmlFile) {
        try {
            Scene scene = sceneCache.get(fxmlFile);
            Object controller = controllerCache.get(fxmlFile);

            if (scene == null) {
                // 1. Load the FXML and create a new Scene
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
                Parent root = loader.load();
                scene = new Scene(root, 1024, 800);

                // 2. **Add your CSS here** so every new Scene gets styled[1]
                String css = getClass().getResource("styles.css").toExternalForm();
                scene.getStylesheets().add(css);

                // 3. Cache the scene and controller for reuse
                controller = loader.getController();
                sceneCache.put(fxmlFile, scene);
                controllerCache.put(fxmlFile, controller);
            }

            // 4. Pass username to controller if applicable
            if (currentUsername != null && controller != null) {
                try {
                    Method setUsernameMethod =
                            controller.getClass().getMethod("setUsername", String.class);
                    setUsernameMethod.invoke(controller, currentUsername);

                    if (controller instanceof GameMenuController) {
                        ((GameMenuController) controller).loadUserData();
                    }
                } catch (Exception e) {
                    // No setUsername method—ignore
                }
            }

            // 5. Finally, show the (already-styled) Scene
            primaryStage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
